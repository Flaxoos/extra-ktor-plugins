package io.github.flaxoos.ktor.server.plugins.ratelimiter

import io.github.flaxoos.ktor.server.plugins.ratelimiter.CallVolumeUnit.Bytes
import io.github.flaxoos.ktor.server.plugins.ratelimiter.implementations.LeakyBucket
import io.github.flaxoos.ktor.server.plugins.ratelimiter.implementations.SlidingWindow
import io.github.flaxoos.ktor.server.plugins.ratelimiter.implementations.TokenBucket
import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.spec.style.scopes.FunSpecContainerScope
import io.kotest.datatest.withData
import io.kotest.inspectors.forAll
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainOnly
import io.kotest.matchers.comparables.shouldBeLessThan
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode.Companion.Forbidden
import io.ktor.http.HttpStatusCode.Companion.InternalServerError
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.http.HttpStatusCode.Companion.TooManyRequests
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.AuthenticationStrategy
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.basic
import io.ktor.server.auth.principal
import io.ktor.server.plugins.callid.CallId
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.ktor.utils.io.core.toByteArray
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.reflect.KClass
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private const val LIMIT = 5
private const val EXCEED = 2
private const val REQUEST_DELAY_MS = 10
private const val RATE_LIMITER_RATE_MS = 100
private const val CALLER1 = "testCaller1"
private const val CALLER2 = "testCaller2"
private const val BASIC_AUTH_PASSWORD = "password"
private const val LIMITED_PATH = "limited"
private const val UNLIMITED_PATH = "unlimited"
private const val USER_AGENT = "some.agent"
private const val LOCALHOST = "localhost"

@OptIn(ExperimentalEncodingApi::class)
private fun encodeBasicAuth(name: String) = Base64.encode("$name:$BASIC_AUTH_PASSWORD".toByteArray())

private val logger = KotlinLogging.logger { }

class RateLimitingPluginTest : FunSpec() {
    init {
        context("Rate Limiting Plugin Tests") {
            context("Installation") {
                testRateLimiting(
                    "If call volume unit is bytes, should install double receive plugin",
                    modifyConfiguration = {
                        rateLimiter { callVolumeUnit = Bytes(1) }
                    },
                ) {
                    it.testCalls(
                        times = 2,
                    ) {
                        shouldBeOk()
                    }
                }
            }
            context("Basic Functionality") {
                testRateLimiting("Exceeding rate limit on applied route should return Too Many Requests status") {
                    it.testCalls(
                        times = LIMIT + EXCEED,
                    ) { shouldBeLimited() }
                }

                testRateLimiting(
                    "Other routes should not be affected",
                ) {
                    it.testCalls(
                        times = LIMIT + EXCEED,
                        path = UNLIMITED_PATH,
                    ) { shouldBeOk() }
                }

                testRateLimiting(
                    "Excluded routes should not be affected",
                    modifyConfiguration = {
                        excludePaths = setOf(Regex("$LIMITED_PATH/$UNLIMITED_PATH/.*"))
                    },
                ) {
                    it.testCalls(
                        times = LIMIT + EXCEED,
                        path = "$LIMITED_PATH/$UNLIMITED_PATH/$UNLIMITED_PATH",
                    ) { shouldBeOk() }
                }

                testRateLimiting("Should distinguish between callers") {
                    it.testCalls(
                        times = LIMIT,
                        callers = listOf(CALLER1, CALLER2),
                    ) { shouldBeOk() }
                }

                testRateLimiting("Following requests should pass") {
                    it.testCalls(
                        times = LIMIT + EXCEED,
                    ) { shouldBeLimited() }

                    logger.info { "waiting..." }
                    delay(RATE_LIMITER_RATE_MS.milliseconds * 1.1)

                    it.testCalls(
                        times = 1,
                    ) { shouldBeOk() }
                }
            }

            context("Bursts") {
                testRateLimiting(
                    "Should handle bursts",
                ) {
                    it.testCalls(
                        times = LIMIT,
                    ) {
                        shouldBeOk()
                        val earliestRequestTime =
                            minOf { response ->
                                response.requestTime.timestamp
                            }
                        when (it.implementation) {
                            TokenBucket::class, SlidingWindow::class -> {
                                forAll { response ->
                                    (response.responseTime.timestamp - earliestRequestTime).milliseconds shouldBeLessThan
                                        RATE_LIMITER_RATE_MS.milliseconds
                                }
                            }

                            LeakyBucket::class -> {
                                // Check they are spaced by the rate
                                asSequence()
                                    .sortedBy { response -> response.responseTime.timestamp }
                                    .windowed(2)
                                    .map { responses ->
                                        (responses[1].responseTime.timestamp - responses[0].responseTime.timestamp).milliseconds
                                    }.onEach { duration ->
                                        // Grace period of 20ms for inaccuracies
                                        duration.shouldBeLessThan((RATE_LIMITER_RATE_MS + 20L).milliseconds)
                                    }.sumOf { duration ->
                                        duration.inWholeMilliseconds
                                    }.shouldBeLessThan(RATE_LIMITER_RATE_MS * LIMIT.toLong())
                            }

                            else -> {
                                error("Unknown implementation")
                            }
                        }
                    }
                }
            }

            context("Whitelisting") {
                testRateLimiting(
                    "Should let whitelisted users pass",
                    { whiteListedPrincipals = setOf(UserIdPrincipal(CALLER1)) },
                ) {
                    it.testCalls(
                        times = LIMIT + EXCEED,
                    ) { shouldBeOk() }
                }

                testRateLimiting("Should let whitelisted hosts pass", {
                    whiteListedHosts = setOf(LOCALHOST)
                }) {
                    it.testCalls(
                        times = LIMIT + EXCEED,
                    ) { shouldBeOk() }
                }

                testRateLimiting("Should let whitelisted user agents pass", {
                    whiteListedAgents = setOf(USER_AGENT)
                }) {
                    it.testCalls(
                        times = LIMIT + EXCEED,
                        userAgent = USER_AGENT,
                    ) { shouldBeOk() }
                }
            }

            context("Blacklisting") {
                testRateLimiting("Should not let blacklisted users pass", {
                    blackListedPrincipals = setOf(UserIdPrincipal(CALLER1))
                }) {
                    it.testCalls(
                        times = 1,
                    ) { shouldBeForbidden() }
                }

                testRateLimiting("Should not let blacklisted hosts pass", {
                    blackListedHosts = setOf(LOCALHOST)
                }) {
                    it.testCalls(
                        times = 1,
                    ) { shouldBeForbidden() }
                }

                testRateLimiting("Should not let blacklisted user agents pass", {
                    blackListedAgents = setOf(USER_AGENT)
                }) {
                    it.testCalls(
                        times = 1,
                        userAgent = USER_AGENT,
                    ) { shouldBeForbidden() }
                }
            }
        }
    }

    private suspend fun FunSpecContainerScope.testRateLimiting(
        testName: String,
        modifyConfiguration: RateLimitingConfiguration.() -> Unit = {},
        block: suspend (RateLimiterTestScope) -> Unit,
    ) {
        withData(
            nameFn = { "${it.simpleName}: $testName" },
            listOf(TokenBucket::class, LeakyBucket::class, SlidingWindow::class),
        ) { implementation ->
            logger.info { "--------------------------" }
            logger.info { "Starting test: $testName" }

            testApplication {
                application {
                    configure(implementation, modifyConfiguration)
                }
                block(RateLimiterTestScope(client, implementation))
            }
        }
    }

    private fun Application.configure(
        implementation: KClass<out RateLimiter>,
        modifyConfiguration: RateLimitingConfiguration.() -> Unit,
    ) {
        install(Authentication) {
            basic("auth-basic") {
                validate { credentials ->
                    UserIdPrincipal(credentials.name)
                }
            }
        }
        install(StatusPages) {
            exception<Throwable> { call, cause ->
                call.respondText(text = "500: ${cause.stackTraceToString()}", status = InternalServerError)
            }
        }
        install(CallId) {
            retrieveFromHeader(HttpHeaders.XRequestId)
        }
        routing {
            authenticate("auth-basic", strategy = AuthenticationStrategy.Required) {
                route(LIMITED_PATH) {
                    install(RateLimiting) {
                        config(implementation, modifyConfiguration)
                    }
                    get {
                        // Invoke double receive
                        call.receive(ByteArray::class)

                        call.principal<UserIdPrincipal>()?.name ?: error("no principal")
                        call.respond(OK)
                    }
                    route(UNLIMITED_PATH) {
                        route(UNLIMITED_PATH) {
                            get {
                                call.respond(OK)
                            }
                        }
                    }
                }
            }
            route(UNLIMITED_PATH) {
                get {
                    call.respond(OK)
                }
            }
        }
    }

    private fun RateLimitingConfiguration.config(
        implementation: KClass<out RateLimiter>,
        configuration: RateLimitingConfiguration.() -> Unit,
    ) {
        rateLimiter {
            this.type = implementation
            this.capacity = LIMIT
            this.rate = RATE_LIMITER_RATE_MS.milliseconds
        }
        configuration()
    }

    private suspend fun RateLimiterTestScope.testCalls(
        path: String = LIMITED_PATH,
        times: Int = LIMIT,
        callDelay: Duration = REQUEST_DELAY_MS.milliseconds,
        callers: List<String> = listOf(CALLER1),
        userAgent: String? = null,
        checkResponses: suspend List<HttpResponse>.() -> Unit,
    ) {
        coroutineScope {
            (1..times)
                .flatMap { index ->
                    callers.map { caller ->
                        async {
                            delay(callDelay)
                            client.get(path) {
                                val auth = encodeBasicAuth(caller)
                                headers.append("Authorization", "Basic $auth")
                                headers.append(HttpHeaders.XRequestId, "${this.url}, caller: $caller index: $index")
                                userAgent?.let { headers.append(HttpHeaders.UserAgent, it) }
                            }
                        }
                    }
                }.let {
                    checkResponses(it.awaitAll())
                }
        }
    }

    private suspend fun Iterable<HttpResponse>.shouldBeLimited() {
        withClue("Should be limited") {
            logErrors()
            map { it.status } shouldContain TooManyRequests
        }
    }

    private suspend fun Iterable<HttpResponse>.shouldBeOk() {
        withClue("Should not be limited") {
            logErrors()
            map { it.status }.shouldContainOnly(OK)
        }
    }

    private suspend fun Iterable<HttpResponse>.shouldBeForbidden() {
        withClue("Should be forbidden") {
            logErrors()
            map { it.status }.shouldContainOnly(Forbidden)
        }
    }

    private suspend fun Iterable<HttpResponse>.logErrors() {
        filter { it.status.value == InternalServerError.value }.forEach {
            it.bodyAsText().let { logger.error { it } }
        }
    }
}

data class RateLimiterTestScope(
    val client: HttpClient,
    val implementation: KClass<out RateLimiter>,
)
