plugins {
    id("ktor-server-plugin-conventions")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation("org.jetbrains.kotlinx:atomicfu:0.21.0")
            }
        }
        commonTest{
            dependencies{
                implementation(libs.mockk)
            }
        }
    }
}

koverReport {
    defaults {
        verify {
            rule {
                isEnabled = true
                minBound(70)
            }
            onCheck = true
        }
    }
}
