# Task Scheduler Plugin for Ktor Server
<a href="file:/Users/ido/IdeaProjects/flax-ktor-plugins/ktor-server-task-scheduler/build/reports/kover/html/index.html">![koverage](https://img.shields.io/badge/93.58-green?logo=kotlin&label=koverage&style=flat)</a>

---

Manage scheduled tasks across instances of your distributed ktor server, using various strategies and a kotlin favoured cron tab

---

## Features:

- **Various Implementations**: Can use [Redis](ktor-server-task-scheduling-redis)(JVM/Native), [JDBC](ktor-server-task-scheduling-jdbc) (JVM) or [MongoDB](ktor-server-task-scheduling-mongodb) (JVM) for lock management, or add your own implementation
by extending [Core](ktor-server-task-scheduling-core)
- **Multiple managers**: Define multiple tasks and assign each to a manager of your choice

- **Kron Schedule builder**: Utilizes [krontab](https://github.com/InsanusMokrassar/krontab) for building schedules using a convenient kotlin DSL 

## How to Use:
- Add a dependency for your chosen task managers or just add core and implement yourself:
```kotlin
    implementation("io.github.flaxoos:ktor-server-task-scheduling-${redis/jdbc/mongodb/core}:$ktor_plugins_version")
```
- Install the plugin and define one or more task managers:
```kotlin
install(TaskScheduling){
    redis{ //<-- this will be the default manager
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

## Important Notes:

- Ensure you have distinct names for task and task manager.

