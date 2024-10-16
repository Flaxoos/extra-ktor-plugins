package io.github.flaxoos.ktor.server.plugins.kafka.components

import com.sksamuel.avro4k.Avro
import io.github.flaxoos.ktor.server.plugins.kafka.TopicName
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.serializer
import org.apache.avro.generic.GenericRecord
import kotlin.reflect.KClass
import kotlin.reflect.typeOf

fun createSchemaRegistryClient(
    schemaRegistryUrl: String,
    timeoutMs: Long,
    clientProvider: () -> HttpClient,
) = SchemaRegistryClient(clientProvider(), schemaRegistryUrl, timeoutMs)

class SchemaRegistryClient(
    providedClient: HttpClient,
    schemaRegistryUrl: String,
    timeoutMs: Long,
) {
    val client =
        providedClient.config {
            install(ContentNegotiation) { json() }
            install(HttpTimeout) {
                requestTimeoutMillis = timeoutMs
            }
            defaultRequest {
                url(schemaRegistryUrl)
            }
        }

    inline fun <reified T : Any> registerSchemas(
        application: Application,
        schemas: MutableMap<KClass<out T>, TopicName>,
    ) {
        schemas.forEach {
            registerSchema(application, it.key, it.value)
        }
    }

    /**
     * Register a schema to the schema registry using ktor client
     *
     * @param klass the class to register, must be annotated with [Serializable]
     * @param topicName the topic name to associate the schema with
     * @param onConflict the function to run if a schema with the same name already exists, defaults to do
     */
    @OptIn(InternalSerializationApi::class)
    inline fun <reified T : Any> registerSchema(
        application: Application,
        klass: KClass<out T>,
        topicName: TopicName,
        noinline onConflict: () -> Unit = {},
    ) {
        val schema = Avro.default.schema(klass.serializer())
        val payload = mapOf("schema" to schema.toString()) // Creating a map to form the payload
        application.launch(Dispatchers.IO) {
            client
                .post("subjects/${topicName.value}.${schema.name}/versions") {
                    contentType(ContentType.Application.Json)
                    setBody(payload)
                }.let {
                    if (it.status == HttpStatusCode.Conflict) {
                        onConflict()
                    }
                    if (!it.status.isSuccess()) {
                        application.log.error(
                            "Failed registering schema to schema registry at ${it.call.request.url}:\n${it.status} " +
                                "${it.bodyAsText()}:\nschema: $payload",
                        )
                    }
                }
        }
    }
}

/**
 * converts a [GenericRecord] to a [T]
 *
 * @param record [GenericRecord]
 * @return the resulting [T] must be annotated with [Serializable]
 * @throws [SerializationException] if serialization fails
 */
inline fun <reified T> fromRecord(record: GenericRecord): T = Avro.default.fromRecord(serializer(typeOf<T>()), record) as T

/**
 * converts a [T] to a [GenericRecord]
 *
 * @receiver the [T] to convert, must be annotated with [Serializable]
 * @return the resulting [GenericRecord]
 * @throws [SerializationException] if serialization fails
 */
inline fun <reified T> T.toRecord(): GenericRecord = Avro.default.toRecord(serializer(), this)
