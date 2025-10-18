package com.ionelchis.miko

import com.ionelchis.miko.ext.substitute
import com.ionelchis.miko.model.InjectionModule
import com.ionelchis.miko.model.Provider
import com.ionelchis.miko.model.Qualifier
import com.ionelchis.miko.model.Scope
import com.ionelchis.miko.model.TypeKey
import com.ionelchis.miko.model.module
import com.ionelchis.miko.model.typeKey
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

    @Synchronized
    fun loadModules(vararg modules: InjectionModule) {
        modules.forEach { module ->
            loadedModules += module
            module.initializer(this)
        }
    }

    fun unloadModules() {
        container.clear()
        constructorCache.clear()
        loadedModules.clear()
    }

    // region binders
    fun <T> bind(
        key: TypeKey<T>,
        scope: Scope,
        instantiator: () -> T,
    ) {
        container[key] = Provider(scope, instantiator)
    }

    inline fun <reified T> bind(
        scope: Scope,
        qualifier: Qualifier? = null,
        noinline instantiator: () -> T,
    ) = bind(typeKey<T>(qualifier), scope, instantiator)

    inline fun <reified T> factory(
        qualifier: Qualifier? = null,
        noinline instantiator: () -> T,
    ) = bind(Scope.Factory, qualifier, instantiator)

    inline fun <reified T> singleton(
        qualifier: Qualifier? = null,
        noinline instantiator: () -> T,
    ) = bind(Scope.Singleton, qualifier, instantiator)

    inline fun <reified T> get(qualifier: Qualifier? = null): T = resolver(qualifier)
    // endregion

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

    inline fun <reified T> resolver(qualifier: Qualifier? = null) = resolver(typeKey<T>(qualifier))


    inline fun <reified T : Any> inject(
        qualifier: Qualifier? = null,
    ): ReadOnlyProperty<Any?, T> {
        return object : ReadOnlyProperty<Any?, T> {
            private val instance = resolver<T>(qualifier)

            override fun getValue(thisRef: Any?, property: KProperty<*>): T = instance
        }
    }
}

