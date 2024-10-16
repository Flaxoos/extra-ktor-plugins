package io.github.flaxoos.ktor.server.plugins.kafka.components

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.common.KafkaFuture

internal fun Map<String, Any?>.createKafkaAdminClient(): AdminClient = AdminClient.create(this)

internal class CoroutineScopedAdminClient(
    val adminClient: AdminClient,
    val scope: CoroutineScope,
) {
    companion object {
        fun CoroutineScope.CoroutineScopedAdminClient(adminClient: AdminClient) = CoroutineScopedAdminClient(adminClient, this)
    }
}

@Suppress("ForbiddenVoid")
internal suspend fun CoroutineScopedAdminClient.createKafkaTopics(
    topicBuilders: List<NewTopic>,
    existingTopicHandler: (NewTopic) -> Unit = {},
    topicCreationHandler: Pair<String, KafkaFuture<Void>>.() -> Unit,
) {
    val existingTopics =
        adminClient
            .listTopics()
            .listings()
            .get()
            .map { it.name() }
    val createTopicsResult =
        topicBuilders.partition { it.name() in existingTopics }.let { (existing, new) ->
            existing.forEach {
                existingTopicHandler(it)
            }
            adminClient.createTopics(new)
        }

    createTopicsResult
        .values()
        .map { result ->
            scope.launch(Dispatchers.IO) {
                result.value.get()
                topicCreationHandler(result.toPair())
            }
        }.joinAll()
}
