package io.github.flaxoos.ktor

import org.gradle.api.Project
import kotlin.reflect.KProperty

class ProjectPropertyDelegate {
    operator fun getValue(thisRef: Project, property: KProperty<*>): String =
        thisRef.findProperty(property.name.camelCaseToLowerDots()) as String?
            ?: System.getenv(property.name.camelCaseToUpperUnderscore())
            ?: error(
                "Property ${property.name} not found ${thisRef.name}. " +
                        "Ensure it is defined in gradle.properties (lower case, dot separated) or as a system property " +
                        "(upper case, underscore separated)."
            )

    companion object {
        private fun String.camelCaseToLowerDots(): String {
            return this.replace(Regex("([a-z])([A-Z])"), "$1.$2").lowercase()
        }

        private fun String.camelCaseToUpperUnderscore(): String {
            return this.replace(Regex("([a-z])([A-Z])"), "$1_$2").uppercase()
        }

        fun projectOrSystemEnv() =
            ProjectPropertyDelegate()
    }
}