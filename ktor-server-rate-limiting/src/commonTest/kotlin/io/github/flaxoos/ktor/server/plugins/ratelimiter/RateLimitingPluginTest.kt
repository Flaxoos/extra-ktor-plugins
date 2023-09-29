@file:OptIn(
    ExperimentalStdlibApi::class,
    KtorServerPluginUnstableAPI::class,
    ExperimentalEncodingApi::class,
    ExperimentalCoroutinesApi::class,
    ExperimentalEncodingApi::class
)
@file:Suppress("SuspendFunctionOnCoroutineScope")

package io.github.flaxoos.ktor.server.plugins.ratelimiter

import io.github.flaxoos.ktor.server.plugins.ratelimiter.providers.RateLimitProvider
import io.github.flaxoos.ktor.server.plugins.ratelimiter.providers.TokenBucket
import io.kotest.assertions.withClue
import io.kotest.core.config.configuration
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.spec.style.scopes.ContainerScope
import io.kotest.core.test.TestScope
import io.kotest.core.test.testCoroutineScheduler
import io.kotest.datatest.withData
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainOnly
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.mpp.log
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.HttpStatusCode.Companion.Forbidden
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.http.HttpStatusCode.Companion.TooManyRequests
import io.ktor.server.application.Application
import io.ktor.server.application.Plugin
import io.ktor.server.application.PluginInstance
import io.ktor.server.application.RouteScopedPlugin
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.AuthenticationStrategy
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.basic
import io.ktor.server.auth.principal
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.createTestEnvironment
import io.ktor.util.logging.KtorSimpleLogger
import io.ktor.utils.io.core.toByteArray
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.setMain
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.reflect.KClass
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private const val LIMIT = 5
private const val EXCEED = 1
private const val REQUEST_DELAY_MS = 10
private const val RATE_LIMITER_RATE_MS = 100
private const val BASIC_AUTH_NAME = "test"
private const val BASIC_AUTH_PASSWORD = "password"
private const val LIMITED_PATH = "limited"
private const val UNLIMITED_PATH = "unlimited"

private val encodedBasicAuth = Base64.encode("$BASIC_AUTH_NAME:$BASIC_AUTH_PASSWORD".toByteArray())

private fun encodeBasicAuth(name: String) = Base64.encode("$name:$BASIC_AUTH_PASSWORD".toByteArray())

private const val USER_AGENT = "some.agent"

private const val LOCALHOST = "localhost"

class RateLimitingPluginTest : FunSpec() {

    init {

        coroutineTestScope = true
        invocationTimeout = 30.seconds.inWholeMilliseconds

        context("Rate Limiting Tests") {
            context("Basic Functionality") {
//                testRateLimiting("Exceeding rate limit on applied route should return Too Many Requests status") {
//                    it.call(
//                        times = LIMIT + EXCEED
//                    ) { shouldBeLimited() }
//                }
//
//                testRateLimiting(
//                    "Failed calls should release call count",
//                    callShouldFail = true
//                ) {
//                    it.call(
//                        times = LIMIT
//                    ) { shouldNotBeLimited() }
//                }
//
//                testRateLimiting(
//                    "Other routes should not be affected",
//                    exclude = ApplicationRateLimiting
//                ) {
//                    it.call(
//                        times = LIMIT + EXCEED,
//                        path = UNLIMITED_PATH
//                    ) { shouldNotBeLimited() }
//                }
//
//                testRateLimiting("Should distinguish between callers") {
//                    val callers = 2
//                    it.call(
//                        times = LIMIT,
//                        basicAuthFn = { callIndex ->
//                            encodeBasicAuth(
//                                (callIndex % callers).toString()
//                            )
//                        }
//                    ) { shouldNotBeLimited() }
//                }

                testRateLimiting("Following requests should pass") {
                    it.call(
                        times = LIMIT + EXCEED
                    ) { shouldBeLimited() }

                    testCoroutineScheduler.advanceTimeBy(RATE_LIMITER_RATE_MS.milliseconds)

                    it.call(
                        times = 1
                    ) { shouldNotBeLimited() }
                }
            }

            xcontext("Bursts") {
                testRateLimiting(
                    "Bursts should pass if within time window",
                    modifyConfiguration = { providerConfiguration.capacity = LIMIT }
                ) {
                    it.call(
                        times = LIMIT + EXCEED
                    ) { shouldNotBeLimited() }

                }

                testRateLimiting(
                    "Bursts should not pass if not within time window",
                    modifyConfiguration = { providerConfiguration.capacity = LIMIT }
                ) {
                    it.call(
                        times = LIMIT + 10
                    ) { shouldBeLimited() }

                }
            }

            xcontext("Whitelisting") {
                // TODO: solve problem with no authentication available in [TestApplicationCall] and remove exclusion
                // more info: https://medium.com/@emirhanemmez/write-test-for-authenticated-requests-in-ktor-630f2fd0ca25
                testRateLimiting(
                    "Should let whitelisted users pass",
                    { whiteListedPrincipals = setOf(UserIdPrincipal(BASIC_AUTH_NAME)) },
                    exclude = ApplicationRateLimiting
                ) {
                    it.call(
                        times = LIMIT + EXCEED
                    ) { shouldNotBeLimited() }
                }


                testRateLimiting("Should let whitelisted hosts pass", {
                    whiteListedHosts = setOf(LOCALHOST)
                }) {
                    it.call(
                        times = LIMIT + EXCEED
                    ) { shouldNotBeLimited() }
                }

                testRateLimiting("Should let whitelisted user agents pass", {
                    whiteListedAgents = setOf(USER_AGENT)
                }) {
                    it.call(
                        times = LIMIT + EXCEED,
                        userAgent = USER_AGENT
                    ) { shouldNotBeLimited() }
                }
            }
        }

        xcontext("Blacklisting") {
            // TODO: solve problem with no authentication available in [TestApplicationCall] and remove exclusion
            // more info: https://medium.com/@emirhanemmez/write-test-for-authenticated-requests-in-ktor-630f2fd0ca25
            testRateLimiting("Should not let blacklisted users pass", {
                blackListedPrincipals = setOf(UserIdPrincipal(BASIC_AUTH_NAME))
            }, exclude = ApplicationRateLimiting) {
                it.call(
                    times = 1
                ) { shouldBeForbidden() }

            }

            testRateLimiting("Should not let blacklisted hosts pass", {
                blackListedHosts = setOf(LOCALHOST)
            }) {
                it.call(
                    times = 1
                ) { shouldBeForbidden() }
            }

            testRateLimiting("Should not let blacklisted user agents pass", {
                blackListedAgents = setOf(USER_AGENT)
            }) {
                it.call(
                    times = 1,
                    userAgent = USER_AGENT
                ) { shouldBeForbidden() }
            }
        }
    }

    @OptIn(KtorServerPluginUnstableAPI::class)
    private suspend fun ContainerScope.testRateLimiting(
        testName: String,
        modifyConfiguration: RateLimitingConfiguration.() -> Unit = {},
        callShouldFail: Boolean = false,
        exclude: Plugin<Application, RateLimitingConfiguration, PluginInstance>? = null,
        bucketTypes: List<KClass<out RateLimitProvider>> = listOf(
            TokenBucket::class,
//            LeakyBucket::class,
//            SlidingWindow::class
        ),
        test: suspend (TestClientScope) -> Unit
    ) {
        withData(
            nameFn = { "${it.first.key.name} - ${it.second.simpleName}: $testName" },
            ts = listOf(RouteRateLimiting, ApplicationRateLimiting).let { plugins ->
                exclude?.let { plugins.minus(it) } ?: plugins
            }.flatMap { plugin ->
                bucketTypes.map { plugin to it }
            }
        ) { (rateLimiter, bucketType) ->
            println("\n\n$testName")
            val engine = createAppEngine(rateLimiter, bucketType, testName, modifyConfiguration, callShouldFail)
            try {
                engine.start()
                test(TestClientScope(this@testRateLimiting, engine.client))
            } finally {
                engine.stop()
            }
        }
    }

    @OptIn(KtorServerPluginUnstableAPI::class)
    private fun ContainerScope.createAppEngine(
        rateLimiter: Plugin<Application, RateLimitingConfiguration, PluginInstance>,
        bucketType: KClass<out RateLimitProvider>,
        testName: String,
        modifyConfiguration: RateLimitingConfiguration.() -> Unit,
        callShouldFail: Boolean
    ) = TestApplicationEngine(createTestEnvironment {
        module {
            val applicationScope = rateLimiter !is RouteScopedPlugin
            install(Authentication) {
                basic("auth-basic") {
                    validate { credentials ->
                        UserIdPrincipal(credentials.name)
                    }
                }
            }
            install(StatusPages) {
                exception<Throwable> { call, cause ->
                    call.respondText(text = "500: $cause", status = HttpStatusCode.InternalServerError)
                }
            }
            if (applicationScope) {
                install(ApplicationRateLimiting) {
                    configureForTest(rateLimiter, bucketType, testName, modifyConfiguration)
                }
            }
            routing {
                authenticate("auth-basic", strategy = AuthenticationStrategy.Required) {
                    route(LIMITED_PATH) {
                        if (!applicationScope) {
                            install(RouteRateLimiting) {
                                configureForTest(rateLimiter, bucketType, testName, modifyConfiguration)
                            }
                        }
                        get {
                            if (callShouldFail) {
                                error("Call failed")
                            } else {
                                call.respondText(
                                    call.principal<UserIdPrincipal>()?.name ?: error("no principal")
                                )
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
    })

    private fun RateLimitingConfiguration.configureForTest(
        rateLimiter: Plugin<Application, RateLimitingConfiguration, PluginInstance>,
        bucketType: KClass<out RateLimitProvider>,
        testName: String,
        modifyConfiguration: RateLimitingConfiguration.() -> Unit
    ) {
        this.providerConfiguration.type = bucketType
        this.providerConfiguration.capacity = LIMIT
        this.providerConfiguration.rate = RATE_LIMITER_RATE_MS.milliseconds
        this.loggerProvider = { KtorSimpleLogger("io.github.flaxoos.${rateLimiter.key.name}: $testName") }
        modifyConfiguration()
    }

    private suspend fun TestClientScope.call(
        path: String = LIMITED_PATH,
        times: Int = LIMIT,
        delay: Duration = REQUEST_DELAY_MS.milliseconds,
        basicAuthFn: (Int) -> String = { _ -> encodedBasicAuth },
        userAgent: String? = null,
        check: List<HttpResponse>.() -> Unit
    ) {
        (1..times).map { index ->
            testScope.testCoroutineScheduler.advanceTimeBy(delay)
            client.get(path) {
                headers.append("Authorization", "Basic ${basicAuthFn(index)}")
                userAgent?.let { headers.append(HttpHeaders.UserAgent, it) }
            }
        }.let {
            check(it)
        }
    }

    private fun Iterable<HttpResponse>.shouldBeLimited() {
        withClue("Should be limited") {
            map { it.status } shouldContain TooManyRequests
        }
    }

    private fun Iterable<HttpResponse>.shouldNotBeLimited() {
        withClue("Should not be limited") {
            map { it.status } shouldNotContain TooManyRequests
        }
    }

    private fun Iterable<HttpResponse>.shouldBeForbidden() {
        withClue("Should be forbidden") {
            map { it.status }.shouldContainOnly(Forbidden)
        }
    }

    private fun HttpResponse.shouldBeLimited() {
        withClue("Should be limited") {
            status.shouldBe(TooManyRequests)
        }
    }

    private fun HttpResponse.shouldNotBeLimited() {
        withClue("Should not be limited") {
            status.shouldNotBe(TooManyRequests)
        }
    }

    private fun HttpResponse.shouldBeForbidden() {
        withClue("Should be forbidden") {
            status.shouldBe(Forbidden)
        }
    }
}

data class TestClientScope(
    val testScope: TestScope,
    val client: HttpClient
) : CoroutineScope by testScope