package io.github.flaxoos.ktor

import kotlin.reflect.KProperty
import org.gradle.api.Project

class ProjectPropertyDelegate {
    operator fun getValue(
        thisRef: Project,
        property: KProperty<*>,
    ): String =
        thisRef.findProperty(property.name.camelCaseToLowerDots()) as String?
            ?: System.getenv(property.name.camelCaseToUpperUnderscore())
            ?: "".also {
                thisRef.logger.warn(
                    "Property ${property.name} not found ${thisRef.name}. " +
                        "Ensure it is defined in gradle.properties (lower case, dot separated) or as a system property " +
                        "(upper case, underscore separated).",
                )
            }

    companion object {
        private fun String.camelCaseToLowerDots(): String = this.replace(Regex("([a-z])([A-Z])"), "$1.$2").lowercase()

        private fun String.camelCaseToUpperUnderscore(): String = this.replace(Regex("([a-z])([A-Z])"), "$1_$2").uppercase()

        fun projectOrSystemEnv() = ProjectPropertyDelegate()
    }
}
