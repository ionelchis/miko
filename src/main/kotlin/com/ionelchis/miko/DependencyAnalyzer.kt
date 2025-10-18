package com.ionelchis.miko

import com.ionelchis.miko.annotation.ExperimentalMikoApi
import com.ionelchis.miko.ext.substitute
import com.ionelchis.miko.model.TypeKey
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

/**
 * A static analysis tool for detecting dependency graph issues within the [Miko] DI container.
 *
 * This analyzer performs a recursive traversal of all known bindings and auto-constructible
 * dependencies, starting from a given root [TypeKey]. It detects:
 *
 * 1. **Missing dependencies** — when a class requires another dependency that has
 *    no registered provider and cannot be auto-constructed.
 * 2. **Cyclic dependencies** — when a set of components depend on each other in
 *    a circular manner (e.g., `A -> B -> A`).
 *
 * The analysis does not instantiate anything; it uses only reflection and the
 * registered container bindings in [Miko].
 *
 * ### Example
 * ```kotlin
 * data class Engine(val fuelPump: FuelPump)
 * data class FuelPump(val sensor: Sensor)
 * data class Sensor(val engine: Engine) // <-- cycle
 *
 * val result = DependencyAnalyzer.analyze(typeKey<Engine>())
 *
 * println(result.cycles.size) // 1
 * println(result.missingDependencies) // empty
 * ```
 *
 * ### Implementation details
 * - The analyzer uses a DFS traversal strategy.
 * - It caches constructors using [Miko.constructorCache] for performance.
 * - Cycle detection is based on a pair of sets: `visiting` (active recursion stack)
 *   and `visited` (fully analyzed nodes).
 * - Generic parameter substitution is performed via `KType.substitute`.
 *
 * @see AnalysisResult for the output model containing missing dependencies and cycles.
 */
@ExperimentalMikoApi
object DependencyAnalyzer {
    /**
     * Holds the results of a dependency graph analysis.
     *
     * @property missingDependencies A list of [TypeKey]s that have no registered provider
     * and cannot be auto-constructed.
     * @property cycles A list of dependency cycles, where each cycle is represented
     * as an ordered list of [TypeKey]s forming the cycle path.
     */
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
