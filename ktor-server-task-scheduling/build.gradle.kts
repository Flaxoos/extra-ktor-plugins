import io.github.flaxoos.ktor.commonMainDependencies

plugins {
    id("ktor-server-plugin-conventions")
}

kotlin {
    sourceSets {
        commonMainDependencies {
            api(projects.ktorServerTaskScheduling.ktorServerTaskSchedulingCore)
            api(projects.ktorServerTaskScheduling.ktorServerTaskSchedulingRedis)
            api(projects.ktorServerTaskScheduling.ktorServerTaskSchedulingJdbc)
            api(projects.ktorServerTaskScheduling.ktorServerTaskSchedulingMongodb)
        }
    }
}