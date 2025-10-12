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

## Example Usage

Wrap dependencies in modules and start the initalization manually:
```kotlin
// Define your dependencies
val userModule = module {
    singleton { HttpClient() }
    factory<Repository<User>> { UserRepository(get()) }
    singleton { UserService(get()) }
}

// At startup
Miko.init(userModule)
```

**OR**

Initialize the dependencies directly from anywhere, without the modules wrapper:
```kotlin
modulesInit {
    singleton { HttpClient() }
    factory<Repository<User>> { UserRepository(get()) }
    singleton { UserService(get()) }
}
```

```kotlin
// Inject anywhere
class UserViewModel(
    private val service: UserService
)

// Resolve manually or via delegation
val viewModel: UserViewModel by inject()
