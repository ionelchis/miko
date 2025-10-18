# Miko — A Lightweight Kotlin Dependency Injection Library (WIP)

**Miko** is a reflection-based dependency injection library for Kotlin and Android.  
It’s inspired by Koin, but designed with **full generic type support**, **lock-free caching**, and **constructor-based injection**.

---

## Features

- **Constructor Injection** — automatic resolution of dependencies from constructors  
- **Generic Type Support** — inject types like `Serializer<User>` without workarounds  
- **Fast, Lock-Free Caching** — singleton creation uses atomic references for thread-safety  
- **Qualifiers** — distinguish multiple bindings of the same type  
- **No Code Generation** — pure Kotlin, no annotation processors or kapt  
- **Reflection-cached** — constructors are cached for fast re-resolution  

---

## Dependency

settings.gradle.kts:
```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

build.gradle.kts:
```kotlin
dependencies {
    implementation("com.github.ionelchis:miko:0.1.2")
}
```


## Example Usage

### Initialization
Wrap dependencies in modules and start the initalization manually:
```kotlin
// Define your dependencies
val userModule = module {
    singleton { HttpClient() }
    factory<Repository<User>> { UserRepository(get()) }
    singleton { UserService(get()) }
}

val validationModule = module {
    factory<Validator<Email>> { EmailValidator() }
    //...
}

// At startup
Miko.loadModules(userModule, validationModule)
```

**OR**

Initialize the dependencies directly from anywhere, without the modules wrapper:
```kotlin
moduleLoad {
    singleton { HttpClient() }
    factory<Repository<User>> { UserRepository(get()) }
    singleton { UserService(get()) }
}
```

### Injection
```kotlin
// Inject anywhere
class UserService(
    private val repo: UserRepository
)

// Resolve manually or via delegation
val service: UserService by inject()
