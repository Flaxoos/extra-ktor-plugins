# Kafka Plugin for Ktor Server

<a href="file:/Users/ido/IdeaProjects/flax-ktor-plugins/ktor-server-kafka/build/reports/kover/html/index.html">![koverage](https://img.shields.io/badge/71.05-yellow?logo=kotlin&label=koverage&style=flat)</a>

---

Integrate Kafka effortlessly into your Ktor application with this powerful Kafka Plugin. This plugin provides an easy setup for Kafka clients, allowing you to configure and manage Kafka Admin, Producer, and Consumer instances directly in your Ktor server.

--- 

## Features

- **Streamlined Configuration**: Install Kafka client configurations either through application configuration files or directly in code.
- **Admin Client**: Easy setup and topic creation for your Kafka admin client.
- **Producer Client**: Initialize Kafka producer instances effortlessly.
- **Consumer Client**: Configure and manage Kafka consumer instances, including polling logic and record handling.
- **Built in Avro4k support**: Avro schemas are supported by default. There's no need to define key/value serializers. Schemas can be registered automatically. Avro records to and from conversion methods

## How to Use

### From Code

The plugin provides a DSL that enables comprehensive Kafka configuration, adhering to the classes and properties defined in [org.apache.kafka.common.config](https://kafka.apache.org/21/javadoc/index.html?org/apache/kafka/common/config/package-summary.html), the DSL offers a fluent, programmatic way to set up your Kafka settings right within your Ktor application.

```kotlin
install(Kafka) {
    schemaRegistryUrl = listOf("my.schemaRegistryUrl")
     topic(named("my-topic")) {
         partitions = 1
         replicas = 1
         configs {
             messageTimestampType = CreateTime
         }
     }
     common { // <-- Define common configs
         bootstrapServers = listOf("my-kafka")
         retries = 1
         clientId = "my-client-id"
     }
     admin { } // <-- Creates an admin client
     producer { // <-- Creates a producer
         clientId = "my-client-id" 
     } 
     consumer { // <-- Creates a consumer
         groupId = "my-group-id"
         clientId = "my-client-id-override" //<-- Override common configurations
     } 
     consumerConfig {
         consumerRecordHandler(named("my-topic")) { record ->
             myService.save(record)
         }
     }
     registerSchemas {
         using { // <-- optionally provide a client, by default CIO is used
             HttpClient()
         }
         MyRecord::class at named("my-topic") // <-- Will register schema upon startup
     }
}
```

### From Configuration File

Alternatively, You can easily install the Kafka plugin using an application configuration file:

```kotlin
install(KafkaFromFileConfig.Kafka) {
    consumerConfig {
        consumerRecordHandler("my-topic") { record ->
            myService.save(record)
        }
    }
    registerSchemas {
        MyRecord::class at named("my-topic") // <-- Will register schema upon startup
    }
}
```
The above will look for the config in `ktor.kafka` by default.

You can also specify a different path if needed:

```kotlin
install(KafkaFromFileConfig.Kafka("ktor.my.kafka")){
    ...
}
```

Example file configuration:
```hocon
ktor {
  kafka {
    schema.registry.url = ["SCHEMA_REGISTRY_URL"]
    common {
      "bootstrap.servers" = ["BOOTSTRAP_SERVERS"]
      # Additional configuration
    }
    admin {
      ##Additional configuration
    }
    consumer {
      "group.id" = "my-group-id"
      # Additional configuration
    }
    producer {
      "client.id" = "my-client-id"
      # Additional configuration
    }
    topics = [
      {
        name = my-topic
        partitions = 1
        replicas = 1
        configs {
          "message.timestamp.type" = CreateTime
          # Additional configuration
        }
      }
    ]
  }
}
```

### Access Kafka Clients

After installation, you can easily access the initialized Kafka clients throughout your Ktor application:

```kotlin
val adminClient = application.kafkaAdminClient
val producer = application.kafkaProducer
val consumer = application.kafkaConsumer
```

## Important Notes

- Make sure you define a consumer configuration when you initialize a consumer client, or the consumer job will not start automatically.

- Ensure that the `pollFrequency` for consumers is set appropriately, depending on your use-case.

- Always verify topic creation and monitor client status for optimal Kafka integration.

- This plugin works asynchronously, so it's advised to monitor the logs for setup completion and error notifications.


## Acknowledgements

This project uses code from [gAmUssA/ktor-kafka](https://github.com/gAmUssA/ktor-kafka), which is available under the MIT License. This project expands on concepts that were introduced there in a few ways:

- The Topic DSL idea was expanded to allow for configuration of all components in a similar manner
- The consumer behaviour can be configured in the plugin setup
- The producer and consumer are created automatically and exposed via the server attributes
- The consumer can return a flow of records
