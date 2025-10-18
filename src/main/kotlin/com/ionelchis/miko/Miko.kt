package com.ionelchis.miko

import com.ionelchis.miko.Miko.bind
import com.ionelchis.miko.Miko.get
import com.ionelchis.miko.Miko.loadModules
import com.ionelchis.miko.Miko.resolver
import com.ionelchis.miko.Miko.unloadModules
import com.ionelchis.miko.ext.substitute
import com.ionelchis.miko.model.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty
import kotlin.reflect.full.primaryConstructor

/**
 * **Miko** — A lightweight, reflection-based dependency injection container for Kotlin.
 *
 * Miko is inspired by Koin and designed to be simple, type-safe, and fully generic-aware.
 * It supports constructor injection, scopes (Singleton / Factory), and qualifiers for
 * multiple bindings of the same type.
 *
 * Unlike Koin, Miko uses Kotlin's [kotlin.reflect.KType] for internal type keys, meaning it can
 * differentiate bindings such as `Serializer<User>` and `Serializer<Post>`.
 *
 * **Usage**: There are two main ways to initialize dependencies
 *
 * - Initialize modules and load them at app startup:
 * ```
 * val userModule = module {
 *     singleton { HttpClient() }
 *     factory { UserRepository(get()) }
 * }
 *
 * // At app startup:
 * Miko.loadModules(userModule)
 *
 * // Inject dependency where you need it
 * val repo: UserRepository by inject()
 * ```
 *
 * - Or initialize directly:
 * ```
 * moduleLoad {
 *     singleton { HttpClient() }
 *     factory { UserRepository(get()) }
 * }
 *
 * // Inject dependency where you need it

 * val repo: UserRepository by inject()
 * ```
 *
 * @see module
 * @see Scope
 * @see Qualifier
 */
object Miko {
    // region internal
    internal val container = ConcurrentHashMap<TypeKey<*>, Provider<*>>()
    internal val constructorCache = ConcurrentHashMap<KClass<*>, KFunction<*>>()
    // endregion

    // region private
    private val loadedModules = mutableListOf<InjectionModule>()
    private val logger: ((String) -> Unit)? = null
    // endregion

    /**
     * Loads one or more [InjectionModule]s into the Miko container.
     *
     * Each module defines its own bindings via the [InjectionModule.initializer] function.
     * Modules are initialized in the order provided. The function is synchronized to ensure
     * thread-safe module loading and consistent container state.
     *
     * @param modules The modules to load and register into the dependency container.
     *
     * @see unloadModules
     */
    @Synchronized
    fun loadModules(vararg modules: InjectionModule) {
        modules.forEach { module ->
            loadedModules += module
            module.initializer(this)
        }
    }

    /**
     * Unloads all registered modules and clears the dependency container.
     *
     * This removes all bindings, clears the constructor cache, and resets the list
     * of loaded modules. It’s mainly useful in testing scenarios or when resetting
     * the DI context in long-lived applications.
     *
     * @see loadModules
     */
    fun unloadModules() {
        container.clear()
        constructorCache.clear()
        loadedModules.clear()
    }

    // region binders
    /**
     * Registers a new dependency binding into the container.
     *
     * @param key A unique [TypeKey] identifying the dependency (including type and optional qualifier).
     * @param scope The [Scope] determining the lifetime (e.g. [Scope.Singleton] or [Scope.Factory]).
     * @param instantiator A lambda that creates the dependency instance.
     */
    fun <T> bind(
        key: TypeKey<T>,
        scope: Scope,
        instantiator: () -> T,
    ) {
        container[key] = Provider(scope, instantiator)
    }

    /**
     * Inline variant of [bind] that infers the dependency type from the generic parameter.
     *
     * @param scope The scope determining the instance lifetime.
     * @param qualifier Optional [Qualifier] to differentiate multiple bindings of the same type.
     * @param instantiator Lambda used to instantiate the dependency.
     */
    inline fun <reified T> bind(
        scope: Scope,
        qualifier: Qualifier? = null,
        noinline instantiator: () -> T,
    ) = bind(typeKey<T>(qualifier), scope, instantiator)

    /**
     * Registers a factory binding. Each call to [get] or [resolver] will produce a new instance.
     *
     * @param qualifier Optional [Qualifier] for disambiguation.
     * @param instantiator The lambda used to construct new instances on demand.
     */
    inline fun <reified T> factory(
        qualifier: Qualifier? = null,
        noinline instantiator: () -> T,
    ) = bind(Scope.Factory, qualifier, instantiator)

    /**
     * Registers a singleton binding. The instance is created once and cached for the entire app lifetime.
     *
     * @param qualifier Optional [Qualifier] to differentiate bindings of the same type.
     * @param instantiator Lambda used to construct the instance when first requested.
     */
    inline fun <reified T> singleton(
        qualifier: Qualifier? = null,
        noinline instantiator: () -> T,
    ) = bind(Scope.Singleton, qualifier, instantiator)

    inline fun <reified T> get(qualifier: Qualifier? = null): T = resolver(qualifier)
    // endregion


    /**
     * Resolves a dependency by its [TypeKey].
     *
     * If the dependency is not explicitly registered, Miko will attempt to automatically
     * construct it using its primary constructor via reflection. Constructor parameters
     * are resolved recursively, supporting nested injection graphs.
     *
     * @param key The [TypeKey] representing the dependency type and optional qualifier.
     * @return The resolved dependency instance.
     * @throws IllegalStateException If the dependency cannot be constructed or found.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> resolver(key: TypeKey<T>): T {
        container[key]?.let { provider ->
            return provider.provide() as T
        }

        val kClass = key.type.classifier as? KClass<*>
            ?: throw IllegalStateException("Invalid classifier for ${key.type}")

        val primaryConstructor = constructorCache.getOrPut(kClass) {
            kClass.primaryConstructor
                ?: throw IllegalStateException(
                    "Cannot construct $kClass automatically. " +
                            "No registered binding found and no primary constructor available."
                )
        }

        // Build mapping of type parameters -> actual types
        val substitution = kClass.typeParameters.zip(key.type.arguments).toMap()

        // Build constructor parameters
        val parameters = primaryConstructor.parameters.also {
            logger?.invoke("Reflection lookup")
        }.map { parameter ->
            val substitutedType = parameter.type.substitute(substitution)
            resolver(TypeKey<Any>(substitutedType, key.qualifier))
        }.toTypedArray()

        val provider = container.computeIfAbsent(key) {
            Provider(Scope.Singleton) { primaryConstructor.call(*parameters) }
        }
        return provider.provide() as T
    }

    /**
     * Inline helper to resolve a dependency of type [T].
     *
     * @param qualifier Optional [Qualifier] for disambiguation.
     * @return The resolved instance of type [T].
     */
    inline fun <reified T> resolver(qualifier: Qualifier? = null) = resolver(typeKey<T>(qualifier))

    /**
     * Provides a property delegate that lazily resolves and caches a dependency instance.
     *
     * Example:
     * ```
     * import com.ionelchis.miko.Miko.inject
     *
     * class UserRepository {
     *     private val api by inject<UserApi>()
     * }
     * ```
     *
     * @param qualifier Optional [Qualifier] to distinguish bindings.
     * @return A [ReadOnlyProperty] that delegates access to the resolved dependency.
     */
    inline fun <reified T : Any> inject(
        qualifier: Qualifier? = null,
    ): ReadOnlyProperty<Any?, T> {
        return object : ReadOnlyProperty<Any?, T> {
            private val instance = resolver<T>(qualifier)

            override fun getValue(thisRef: Any?, property: KProperty<*>): T = instance
        }
    }
}

