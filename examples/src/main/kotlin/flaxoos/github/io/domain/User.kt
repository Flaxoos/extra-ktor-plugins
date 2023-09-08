package flaxoos.github.io.domain

import com.sksamuel.avro4k.AvroNamespace
import kotlinx.serialization.Serializable

@Suppress("PROVIDED_RUNTIME_TOO_LOW")
@Serializable
@AvroNamespace("flaxoos.github.io.domain")
data class User(val id: String, val username: String)
