package io.flax.ktor.server.plugins

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.spec.style.scopes.ContainerScope
import io.kotest.datatest.withData
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainOnly
import io.kotest.matchers.collections.shouldNotContain
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
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.ktor.util.logging.KtorSimpleLogger
import io.ktor.utils.io.core.toByteArray
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private const val LIMIT = 5
private const val EXCEED = 1
private const val DELAY_MS = 1
private const val BASIC_AUTH_NAME = "test"
private const val BASIC_AUTH_PASSWORD = "password"
private const val LIMITED_PATH = "limited"
private const val UNLIMITED_PATH = "unlimited"
private val window = 500.milliseconds

@OptIn(ExperimentalEncodingApi::class)
private val encodedBasicAuth = Base64.encode("$BASIC_AUTH_NAME:$BASIC_AUTH_PASSWORD".toByteArray())

@OptIn(ExperimentalEncodingApi::class)
private fun encodeBasicAuth(name: String) = Base64.encode("$name:$BASIC_AUTH_PASSWORD".toByteArray())

private const val USER_AGENT = "some.agent"

private const val LOCALHOST = "localhost"

@OptIn(KtorServerPluginUnstableAPI::class)
class RateLimitingTest : FunSpec() {

    init {
        context("Rate Limiting Tests") {
            context("Basic Functionality") {
                testRateLimiting("Exceeding rate limit on applied route should return Too Many Requests status") { testClient ->
                    testClient.call(times = LIMIT + EXCEED)
                        .shouldBeLimited()
                }

                testRateLimiting("Failed calls should release call count", callShouldFail = true) { testClient ->
                    testClient.call(times = LIMIT)
                        .shouldNotBeLimited()
                }

                testRateLimiting("Other routes should not be affected", exclude = ApplicationRateLimiting) {
                    client.call(
                        times = LIMIT + EXCEED,
                        path = UNLIMITED_PATH
                    ).shouldNotBeLimited()
                }

                testRateLimiting("Should distinguish between callers") { testClient ->
                    val callers = 2
                    testClient.call(
                        times = LIMIT,
                        basicAuthFn = { callIndex ->
                            encodeBasicAuth(
                                (callIndex % callers).toString()
                            )
                        }
                    ).shouldNotBeLimited()
                }

                testRateLimiting("Following requests should pass") { testClient ->
                    testClient.call(
                        times = LIMIT + EXCEED
                    ).shouldBeLimited()

                    delay(window)

                    testClient.call(times = 1).shouldNotBeLimited()
                }
            }

            context("Bursts") {
                testRateLimiting(
                    "Bursts should pass if within time window",
                    modifyConfiguration = {
                        limit = LIMIT
                        timeWindow = window
                        burstLimit = LIMIT * 2
                    }
                ) {
                    client.call(times = LIMIT + EXCEED).shouldNotBeLimited()
                }

                testRateLimiting(
                    "Bursts should not pass if not within time window",
                    modifyConfiguration = {
                        limit = LIMIT
                        timeWindow = window
                        burstLimit = LIMIT * 2
                    }
                ) {
                    client.call(times = LIMIT + 10).shouldBeLimited()
                }
            }

            context("Whitelisting") {
                // TODO: solve problem with no authentication available in [TestApplicationCall] and remove exclusion
                // more info: https://medium.com/@emirhanemmez/write-test-for-authenticated-requests-in-ktor-630f2fd0ca25
                testRateLimiting(
                    "Should let whitelisted users pass",
                    { whiteListedPrincipals = setOf(UserIdPrincipal(BASIC_AUTH_NAME)) },
                    exclude = ApplicationRateLimiting
                ) { testClient ->
                    testClient.call(
                        times = LIMIT + EXCEED
                    ).shouldNotBeLimited()
                }
            }

            testRateLimiting("Should let whitelisted hosts pass", {
                whiteListedHosts = setOf(LOCALHOST)
            }) { testClient ->
                testClient.call(
                    times = LIMIT + EXCEED
                ).shouldNotBeLimited()
            }

            testRateLimiting("Should let whitelisted user agents pass", {
                whiteListedAgents = setOf(USER_AGENT)
            }) { testClient ->
                testClient.call(
                    times = LIMIT + EXCEED,
                    userAgent = USER_AGENT
                ).shouldNotBeLimited()
            }
        }

        context("Blacklisting") {
            // TODO: solve problem with no authentication available in [TestApplicationCall] and remove exclusion
            // more info: https://medium.com/@emirhanemmez/write-test-for-authenticated-requests-in-ktor-630f2fd0ca25
            testRateLimiting("Should not let blacklisted users pass", {
                blackListedPrincipals = setOf(UserIdPrincipal(BASIC_AUTH_NAME))
            }, exclude = ApplicationRateLimiting) { testClient ->
                testClient.call(
                    times = 1
                ).shouldBeForbidden()
            }

            testRateLimiting("Should not let blacklisted hosts pass", {
                blackListedHosts = setOf(LOCALHOST)
            }) { testClient ->
                testClient.call(
                    times = 1
                ).shouldBeForbidden()
            }

            testRateLimiting("Should not let blacklisted user agents pass", {
                blackListedAgents = setOf(USER_AGENT)
            }) { testClient ->
                testClient.call(
                    times = 1,
                    userAgent = USER_AGENT
                ).shouldBeForbidden()
            }
        }
    }

    @OptIn(KtorServerPluginUnstableAPI::class)
    private suspend fun ContainerScope.testRateLimiting(
        testName: String,
        modifyConfiguration: RateLimitingConfiguration.() -> Unit = {},
        callShouldFail: Boolean = false,
        exclude: Plugin<Application, RateLimitingConfiguration, PluginInstance>? = null,
        test: suspend ApplicationTestBuilder.(HttpClient) -> Unit
    ) {
        withData(
            nameFn = { "${it.key.name}: $testName" },
            ts = listOf(
                RouteRateLimiting,
                ApplicationRateLimiting
            ).let { plugins ->
                exclude?.let { plugins.minus(it) } ?: plugins
            }
        ) { rateLimiter ->
            testApplication {
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
                        configureForTest(rateLimiter, testName, modifyConfiguration)
                    }
                }
                routing {
                    authenticate("auth-basic", strategy = AuthenticationStrategy.Required) {
                        route(LIMITED_PATH) {
                            if (!applicationScope) {
                                install(RouteRateLimiting) {
                                    configureForTest(rateLimiter, testName, modifyConfiguration)
                                }
                            }
                            get {
                                delay(100)
                                if (callShouldFail) {
                                    error("Call failed")
                                } else {
                                    call.respondText(call.principal<UserIdPrincipal>()?.name ?: error("no principal"))
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
                test(client)
            }
        }
    }

    private fun RateLimitingConfiguration.configureForTest(
        rateLimiter: Plugin<Application, RateLimitingConfiguration, PluginInstance>,
        testName: String,
        modifyConfiguration: RateLimitingConfiguration.() -> Unit
    ) {
        limit = LIMIT
        timeWindow = window
        burstLimit = LIMIT
        loggerProvider = { KtorSimpleLogger("io.flax.${rateLimiter.key.name}: $testName") }
        modifyConfiguration()
    }

    private suspend fun HttpClient.call(
        path: String = LIMITED_PATH,
        times: Int = LIMIT,
        delay: Duration = DELAY_MS.milliseconds,
        basicAuthFn: (Int) -> String = { _ -> encodedBasicAuth },
        userAgent: String? = null
    ) =
        coroutineScope {
            (1..times).map { index ->
                async {
                    delay(delay)
                    get(path) {
                        headers.append("Authorization", "Basic ${basicAuthFn(index)}")
                        userAgent?.let { headers.append(HttpHeaders.UserAgent, it) }
                    }
                }
            }.awaitAll()
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
}
