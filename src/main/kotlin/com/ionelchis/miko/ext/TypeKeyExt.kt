package com.ionelchis.miko.ext

import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.createType

fun KType.substitute(
    map: Map<KTypeParameter, KTypeProjection>,
): KType {
    val classifier = this.classifier
    return when (classifier) {
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
