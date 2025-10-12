package com.ionelchis.miko

import com.ionelchis.miko.ext.substitute
import com.ionelchis.miko.model.TypeKey
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

object DependencyAnalyzer {
    data class AnalysisResult(
        val missingDependencies: List<TypeKey<*>>,
        val cycles: List<List<TypeKey<*>>>,
    )

    private val visited = mutableSetOf<TypeKey<*>>()
    private val visiting = mutableSetOf<TypeKey<*>>()
    private val missing = mutableListOf<TypeKey<*>>()
    private val cycles = mutableListOf<List<TypeKey<*>>>()

    fun analyze(root: TypeKey<*>): AnalysisResult {
        visited.clear()
        visiting.clear()
        missing.clear()
        cycles.clear()

        dfs(root)

        return AnalysisResult(
            missingDependencies = missing.toList(),
            cycles = cycles.toList()
        )
    }

    private fun dfs(key: TypeKey<*>, path: MutableList<TypeKey<*>> = mutableListOf()) {
        if (visiting.contains(key)) {
            // Cycle detected
            val cycleStartIndex = path.indexOf(key)
            val cycle = path.subList(cycleStartIndex, path.size) + key
            cycles.add(cycle.toList())
            return
        }

        if (visited.contains(key)) return

        visiting.add(key)
        path.add(key)

        val provider = Miko.container[key]

        val kClass = key.type.classifier as? KClass<*>
        if (kClass != null) {
            val constructor = Miko.constructorCache.getOrPut(kClass) {
                kClass.primaryConstructor
                    ?: run {
                        // Cannot auto-construct, mark as missing if no provider
                        if (provider == null) missing.add(key)
                        return
                    }
            }

            // Build type parameter substitution
            val substitution = kClass.typeParameters.zip(key.type.arguments).toMap()

            // Recursively visit constructor parameters
            constructor.parameters.forEach { param ->
                val substitutedType = param.type.substitute(substitution)
                val paramKey = TypeKey<Any>(substitutedType, key.qualifier)
                dfs(paramKey, path)
            }
        } else if (provider == null) {
            // Not a class and no provider -> missing
            missing.add(key)
        }

        visiting.remove(key)
        visited.add(key)
        path.removeAt(path.size - 1)
    }
}
