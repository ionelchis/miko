package com.ionelchis.miko

import com.ionelchis.miko.ext.substitute
import com.ionelchis.miko.model.InjectionModule
import com.ionelchis.miko.model.Provider
import com.ionelchis.miko.model.Qualifier
import com.ionelchis.miko.model.Scope
import com.ionelchis.miko.model.TypeKey
import com.ionelchis.miko.model.typeKey
import java.util.concurrent.ConcurrentHashMap
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty
import kotlin.reflect.full.primaryConstructor

object Miko {
    internal val container = ConcurrentHashMap<TypeKey<*>, Provider<*>>()
    internal val constructorCache = ConcurrentHashMap<KClass<*>, KFunction<*>>()

    fun init(vararg modules: InjectionModule) {
        for (module in modules) {
            module.initializer(this)
        }
    }

    internal fun clear() {
        container.clear()
        constructorCache.clear()
    }

    fun <T> bind(
        key: TypeKey<T>,
        scope: Scope,
        instantiator: () -> T,
    ) {
        container[key] = Provider(scope, instantiator)
    }

    inline fun <reified T> factory(
        qualifier: Qualifier? = null,
        noinline instantiator: () -> T,
    ) = bind(Scope.Factory, qualifier, instantiator)

    inline fun <reified T> singleton(
        qualifier: Qualifier? = null,
        noinline instantiator: () -> T,
    ) = bind(Scope.Singleton, qualifier, instantiator)

    inline fun <reified T> bind(
        scope: Scope,
        qualifier: Qualifier? = null,
        noinline instantiator: () -> T,
    ) = bind(typeKey<T>(qualifier), scope, instantiator)

    inline fun <reified T> get(qualifier: Qualifier? = null): T = resolver(qualifier)

    @Suppress("UNCHECKED_CAST")
    fun <T> resolver(key: TypeKey<T>): T {
        container[key]?.let { provider ->
            return provider.provide() as T
        }

        val kClass = key.type.classifier as? KClass<*>
            ?: throw IllegalStateException("Invalid classifier for ${key.type}")

        val primaryConstructor = constructorCache.getOrPut(kClass) {
            kClass.primaryConstructor
                ?: throw IllegalStateException("No primary constructor found for $kClass")
        }

        // Build mapping of type parameters -> actual types
        val substitution = kClass.typeParameters.zip(key.type.arguments).toMap()

        // Build constructor parameters
        val parameters = primaryConstructor.parameters.also {
            println("Reflection lookup")
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

