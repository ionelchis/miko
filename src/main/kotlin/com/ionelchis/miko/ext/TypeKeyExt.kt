package com.ionelchis.miko.ext

import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.createType

/**
 * Performs recursive generic type substitution for a given [KType].
 *
 * This utility replaces generic type parameters with their corresponding concrete
 * [KTypeProjection]s from the provided mapping. It’s primarily used during automatic
 * constructor-based dependency resolution when resolving parameterized types.
 *
 * ### Example
 * Suppose you have:
 * ```
 * class Repository<T>(val serializer: Serializer<T>)
 * ```
 * When resolving `Repository<User>`, this function replaces `T` in the constructor
 * parameter type `Serializer<T>` with the actual `User` type, yielding `Serializer<User>`.
 *
 * ### Behavior
 * - If the current type is a [KTypeParameter], it looks up its substitution in [map].
 * - If it’s a [KClass], it recursively substitutes each of its generic arguments.
 * - If neither, it returns the original type unchanged.
 *
 * @param map A mapping between type parameters and their corresponding projections.
 * Typically created via `kClass.typeParameters.zip(kType.arguments).toMap()`.
 * @return A [KType] where all generic parameters are replaced by concrete types when available.
 */
fun KType.substitute(
    map: Map<KTypeParameter, KTypeProjection>,
): KType {
    return when (val classifier = this.classifier) {
        is KTypeParameter -> map[classifier]?.type ?: this
        is KClass<*> -> {
            val substitutedArgs = this.arguments.map { arg ->
                val argType = arg.type
                if (argType != null) KTypeProjection.invariant(
                    argType.substitute(map)
                )
                else arg
            }
            classifier.createType(substitutedArgs, this.isMarkedNullable)
        }

        else -> this
    }
}
