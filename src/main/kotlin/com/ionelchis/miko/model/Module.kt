package com.ionelchis.miko.model

import com.ionelchis.miko.Miko

/**
 * Represents a logical grouping of dependency bindings within the [com.ionelchis.miko.Miko] container.
 *
 * A module encapsulates related dependencies (for example, all network-related
 * components or all view models) and provides an [initializer] block that registers
 * them into the container using the DSL provided by [com.ionelchis.miko.Miko].
 *
 * Modules make it easy to structure large applications, separating dependency
 * definitions by feature or layer while keeping registration code clean and testable.
 *
 * Example:
 * ```
 * val networkModule = module {
 *     singleton { HttpClient() }
 *     factory { ApiService(get()) }
 * }
 * ```
 *
 * @property initializer A lambda with receiver of type [com.ionelchis.miko.Miko], which defines
 * the dependency bindings to be registered when this module is loaded.
 *
 * @see module
 */
data class InjectionModule(
    val initializer: Miko.() -> Unit,
)

/**
 * DSL entry point for creating a new [InjectionModule].
 *
 * The [module] function provides a concise, readable way to group dependency
 * definitions together. Each module can later be loaded into [Miko] via a
 * `loadModules(...)` or similar API (depending on your setup).
 *
 * Example:
 * ```
 * val userModule = module {
 *     singleton<UserRepository> { UserRepositoryImpl(get()) }
 *     factory { GetUserUseCase(get()) }
 * }
 * ```
 *
 * You can then register all modules together:
 * ```
 * Miko.init(networkModule, userModule)
 * ```
 *
 * @param initialize A lambda that defines dependency bindings inside the [Miko] DSL.
 * @return A new [InjectionModule] that can be registered into [Miko].
 *
 * @see InjectionModule
 */
fun module(initialize: Miko.() -> Unit): InjectionModule {
    return InjectionModule(initialize)
}

fun modulesInit(initialize: Miko.() -> Unit) {
    Miko.initialize()
}
