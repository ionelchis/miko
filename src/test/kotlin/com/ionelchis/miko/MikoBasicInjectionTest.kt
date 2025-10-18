package com.ionelchis.miko

import com.ionelchis.miko.Miko.inject
import com.ionelchis.miko.model.Scope
import com.ionelchis.miko.model.moduleLoad
import com.ionelchis.miko.model.typeKey
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import kotlin.test.BeforeTest
import kotlin.test.assertEquals
import kotlin.test.assertNotSame

class MikoBasicInjectionTest {

    data class Engine(val type: String)

    @BeforeTest
    fun before() = Miko.unloadModules()

    @Test
    fun `should resolve singleton dependency`() {
        // Given
        moduleLoad {
            singleton { Engine("V8") }
        }

        // When
        val e1 by inject<Engine>()
        val e2 by inject<Engine>()

        // Then
        assertEquals(e1, e2)
        assertEquals("V8", e1.type)
    }

    @Test
    fun `should be able to resolve singleton bind dependency`() {
        // Given
        moduleLoad {
            bind<Engine>(Scope.Singleton) { Engine("V8") }
        }

        // When
        val e1 by inject<Engine>()
        val e2 by inject<Engine>()

        // Then
        assertEquals(e1, e2)
        assertEquals("V8", e1.type)
    }

    @Test
    fun `should resolve factory dependency`() {
        // Given
        moduleLoad {
            factory { Engine("V6") }
        }

        // When
        val e1 by inject<Engine>()
        val e2 by inject<Engine>()

        // Then
        assertEquals(e1.type, e2.type)
        assertNotSame(e1, e2)
    }

    @Test
    fun `should be able to resolve factory bind dependency`() {
        // Given
        moduleLoad {
            bind<Engine>(Scope.Factory) { Engine("V6") }
        }

        // When
        val e1 by inject<Engine>()
        val e2 by inject<Engine>()

        // Then
        assertEquals(e1.type, e2.type)
        assertNotSame(e1, e2)
    }
}
