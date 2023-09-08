package flaxoos.github.io.plugins

import flaxoos.github.io.domain.User
import io.github.flaxoos.ktor.server.plugins.ratelimiter.RouteRateLimiting
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.response.respondText
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlin.time.Duration.Companion.seconds

fun Application.configureRouting() {
    routing {
        route("/welcome") {
            install(RouteRateLimiting) {
                limit = 10
                timeWindow = 1.seconds
            }
            post<User> { user ->
                this@configureRouting.sendUser(user)
                call.respondText("Hello ${user.username}")
            }
        }
    }
}
