package com.ionelchis.miko.model

import kotlin.reflect.KType
import kotlin.reflect.typeOf

/**
 * Unique key representing a registered type within the DI container.
 *
 * Combines both the Kotlin [KType] and an optional [Qualifier], allowing
 * generic and qualified types to coexist without conflict.
 *
 * Equality and hash code are based on both the [type] and [qualifier],
 * ensuring that:
 * ```
 * TypeKey<List<String>>() != TypeKey<List<Int>>()
 * TypeKey<Foo>(Named("A")) != TypeKey<Foo>(Named("B"))
 * ```
 *
 * @param T The actual Kotlin type represented by this key.
 * @property type The reflective type information for [T].
 * @property qualifier Optional qualifier used to distinguish bindings.
 *
 * @see Qualifier
 * @see typeKey
 */
data class TypeKey<T>(
    val type: KType,
    val qualifier: Qualifier? = null
) {
    override fun equals(other: Any?) =
        other is TypeKey<*> &&
                type == other.type &&
                qualifier == other.qualifier

    override fun hashCode() = 31 * type.hashCode() + (qualifier?.hashCode() ?: 0)
}

/**
 * Utility function to create a [TypeKey] from a reified generic type [T].
 *
 * Example:
 * ```
 * val key = typeKey<List<String>>(Named("Special"))
 * ```
 *
 * This function preserves full generic type information (using [typeOf])
 * and supports qualifiers, which makes it more powerful than `KClass`
 * based keys used by frameworks like Koin.
 *
 * @param qualifier Optional qualifier to associate with this type key.
 * @return A new [TypeKey] representing the reified type [T].
 */
inline fun <reified T> typeKey(qualifier: Qualifier? = null) = TypeKey<T>(typeOf<T>(), qualifier)
