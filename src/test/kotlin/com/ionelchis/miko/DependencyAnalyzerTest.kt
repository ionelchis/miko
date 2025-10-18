package com.ionelchis.miko

import com.ionelchis.miko.Miko.get
import com.ionelchis.miko.annotation.ExperimentalMikoApi
import com.ionelchis.miko.model.moduleLoad
import com.ionelchis.miko.model.typeKey
import org.junit.jupiter.api.Test
import kotlin.test.BeforeTest
import kotlin.test.assertTrue

@OptIn(ExperimentalMikoApi::class)
class DependencyAnalyzerTest {

    data class Engine(val fuel: Fuel)
    data class Fuel(val octane: Int)
    data class Car(val engine: Engine)

    data class A(val b: B)
    data class B(val c: C)
    data class C(val a: A)

    @BeforeTest
    fun before() = Miko.unloadModules()


//    @Test
    fun `should detect missing dependency`() {
        // Given
        // Only register Car, but not Engine or Fuel
        moduleLoad {
            singleton { Car(get()) }
        }

        // When
        val result = DependencyAnalyzer.analyze(typeKey<Car>())

        // Then
        assertTrue(result.missingDependencies.isNotEmpty(), "Should detect missing dependencies")
        assertTrue(result.cycles.isEmpty(), "Should not detect cycles")
        println("Missing: ${result.missingDependencies}")
    }

    @Test
    fun `should detect circular dependency`() {
        // Given

        // When
        val result = DependencyAnalyzer.analyze(typeKey<A>())

        // Then
        assertTrue(result.cycles.isNotEmpty(), "Should detect at least one cycle")
        assertTrue(result.cycles.first().any { it.type.classifier == A::class })
        println("Detected cycles: ${result.cycles}")
    }

    @Test
    fun `should detect no issues in a valid dependency tree`() {
        // Given
        moduleLoad {
            singleton { Fuel(95) }
            singleton { Engine(get()) }
            singleton { Car(get()) }
        }

        // When
        val result = DependencyAnalyzer.analyze(typeKey<Car>())

        // Then
        assertTrue(result.missingDependencies.isEmpty(), "No missing dependencies expected")
        assertTrue(result.cycles.isEmpty(), "No cycles expected")
    }

    class X(val y: Y)
    class Y(val z: Z)
    class Z(val x: X)
    class W(val t: T)
    class T(val u: U)
    class U()

//    @Test
    fun `should report both missing and cyclic dependencies in mixed graph`() {
        // Given
        moduleLoad {
            singleton { W(get()) } // but no T or U bound
        }

        // When
        val result1 = DependencyAnalyzer.analyze(typeKey<X>())
        val result2 = DependencyAnalyzer.analyze(typeKey<W>())

        // Then
        assertTrue(result1.cycles.isNotEmpty(), "Cycle should exist in X-Y-Z-X")
        assertTrue(result2.missingDependencies.isNotEmpty(), "Missing dependencies T and U should be reported")
    }

//    @Test
    fun `should not crash with primitive or non-class types`() {
        val result = DependencyAnalyzer.analyze(typeKey<Int>())
        assertTrue(result.missingDependencies.isNotEmpty(), "Int should be reported as missing")
    }
}