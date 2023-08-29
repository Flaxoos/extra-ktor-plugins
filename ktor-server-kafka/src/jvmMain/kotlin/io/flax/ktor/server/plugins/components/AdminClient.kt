package io.flax.ktor.server.plugins.components

import io.flax.ktor.server.plugins.TopicBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import org.apache.kafka.clients.admin.AdminClient
import java.util.concurrent.Future

internal fun Map<String, Any?>.createKafkaAdminClient(): AdminClient = AdminClient.create(this)

context (CoroutineScope)
internal suspend fun AdminClient.createKafkaTopics(
    topicBuilders: List<TopicBuilder>,
    existingTopicHandler: (String) -> Unit = {},
    topicCreationHandler: Pair<String, Future<Void>>.() -> Unit
) {
    val existingTopics = listTopics().listings().get().map { it.name() }
    val createTopicsResult =
        topicBuilders.partition { it.topicName.name in existingTopics }.let { (existing, new) ->
            existing.forEach {
                existingTopicHandler(it.topicName.name)
            }
            createTopics(new.map { it.build() })
        }

    createTopicsResult.values().map { result ->
        launch(Dispatchers.IO) {
            result.value.get()
            topicCreationHandler(result.toPair())
        }
    }.joinAll()
}
