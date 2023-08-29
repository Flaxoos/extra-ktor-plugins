package io.flax.ktor.server.plugins

import com.typesafe.config.ConfigFactory
import io.flax.ktor.server.plugins.components.effectiveStreamProperties
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.server.config.*

class HoconConfigExtractionTest : FunSpec() {

    private val config = ApplicationConfig("kafka-config-map.conf")

    init {

        test("should extract config value based on path - config from file") {
            config.config("ktor.kafka.producer")
                .property("key.serializer").getString() shouldBe "org.apache.kafka.common.serialization.LongSerializer"
        }


        test("should extract config for streams based on path - config from file") {
            config.config("ktor.kafka.streams")
                .property("application.id").getString() shouldBe "ktor-stream"
        }


        test("effectiveConfig should produce correct config for streams") {
            effectiveStreamProperties(config)

            config.config("ktor.kafka.streams").property("application.id").getString() shouldBe "ktor-stream"
            config.config("ktor.kafka.properties").property("schema.registry.url").getString() shouldBe "http://localhost:8081"
        }


        test("should extract configs value based on path - inline config") {
            val string = """
            ktor {
              kafka {
                producer {
                  value.serializer = KafkaJsonSchemaSerializer
                  key {
                    serializer =  LongSerializer
                  }
                }
              }
            }
        """.trimIndent()

            val config = HoconApplicationConfig(ConfigFactory.parseString(string))
            val path = "ktor.kafka.producer"
            val childConfig = config.config(path)

            childConfig.property("key.serializer").getString() shouldBe "LongSerializer"
            childConfig.property("value.serializer").getString() shouldBe "KafkaJsonSchemaSerializer"
        }
    }

}
