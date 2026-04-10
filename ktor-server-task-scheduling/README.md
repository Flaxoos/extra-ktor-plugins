# Task Scheduler Plugin for Ktor Server

<a href="file:/Users/ido/IdeaProjects/flax-ktor-plugins/ktor-server-task-scheduler/build/reports/kover/html/index.html">![koverage](https://img.shields.io/badge/93.58-green?logo=kotlin&label=koverage&style=flat)</a>

---

Manage scheduled tasks across instances of your distributed ktor server, using various strategies and a kotlin flavoured
cron tab

---

## Features:

- **Zero Configuration Default**: Works out of the box with an in-memory task manager - perfect for single-instance setups or initial designs that can easily migrate to distributed implementations later
- **Various Implementations**: Can use InMemory (JVM/Native, included by default in [Core](ktor-server-task-scheduling-core)), [Redis](ktor-server-task-scheduling-redis) (JVM/Native), [JDBC](ktor-server-task-scheduling-jdbc) (JVM) or [MongoDB](ktor-server-task-scheduling-mongodb) (JVM)
  for lock management, or add your own implementation by extending [Core](ktor-server-task-scheduling-core)
- **Multiple managers**: Define multiple tasks and assign each to a manager of your choice
- **Kron Schedule builder**: Utilizes [krontab](https://github.com/InsanusMokrassar/krontab) for building schedules
  using a convenient kotlin DSL

## How to Use:

### Quick Start (Single Instance)

For single-instance setups, simply install the plugin with the core dependency and define your tasks. An in-memory task manager will be automatically configured:

```kotlin
dependencies {
    implementation("io.github.flaxoos:ktor-server-task-scheduling-core:$ktor_plugins_version")
}
```

```kotlin
install(TaskScheduling) {
    task {
        name = "My task"
        task = { taskExecutionTime ->
            log.info("My task is running: $taskExecutionTime")
        }
        kronSchedule = {
            hours {
                from 0 every 12
            }
            minutes {
                from 15 every 30
            }
        }
        concurrency = 2
    }
}
```

This will setup an in-memory task manager that will execute the task twice concurrently at the defined [kron](https://github.com/InsanusMokrassar/krontab) schedule

### Distributed Setup

For distributed deployments across multiple instances, add a dependency for your chosen task manager backend (or implement your own):

```kotlin
    implementation("io.github.flaxoos:ktor-server-task-scheduling-${redis/jdbc/mongodb}:$ktor_plugins_version")
```

Install the plugin and define one or more task managers:

```kotlin
install(TaskScheduling){
    redis{ //<-- this will be the default manager, as it is unnamed
        ...
    }
    jdbc("my jdbc manager"){
        ...
    }
}
```

- Configure some tasks and assign them to the managers

```kotlin
install(TaskScheduling) {
    ...
    task { // if no taskManagerName is provided, the task would be assigned to the default manager
        name = "My task"
        task = { taskExecutionTime ->
            log.info("My task is running: $taskExecutionTime")
        }
        kronSchedule = {
            hours {
                from 0 every 12
            }
            minutes {
                from 15 every 30
            }
        }
        concurrency = 2
    }
    
    task(taskManagerName = "my jdbc manager") {
        name = "My Jdbc task"
        ...
    }
}
```

### Migration Path

The default in-memory manager makes it easy to start development without additional infrastructure. When you're ready to scale to multiple instances, simply:

1. Add a distributed task manager dependency (Redis/JDBC/MongoDB)
2. Configure the distributed manager in your plugin installation
3. No changes needed to your task definitions!

## Important Notes:

- Ensure you have distinct names for task and task manager.

