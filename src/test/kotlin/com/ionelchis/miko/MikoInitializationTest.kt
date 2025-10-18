package com.ionelchis.miko

import com.ionelchis.miko.Miko.inject
import com.ionelchis.miko.model.Scope
import com.ionelchis.miko.model.module
import com.ionelchis.miko.model.moduleLoad
import com.ionelchis.miko.model.typeKey
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import kotlin.test.BeforeTest
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNotSame

class MikoInitializationTest {

    interface Serializer<T>
    class UserSerializer : Serializer<Int>
    class CredentialsSerializer : Serializer<String>

    @BeforeTest
    fun before() = Miko.unloadModules()

    @Test
    fun `should load all given modules`() {
        // Given
        val module1 = module {
            singleton<Serializer<Int>> { UserSerializer() }
        }

        val module2 = module {
            singleton<Serializer<String>> { CredentialsSerializer() }
        }

        // When
        Miko.loadModules(module1, module2)

        // Then
        assertDoesNotThrow {
            val serializer1 by inject<Serializer<Int>>()
            val serializer2 by inject<Serializer<String>>()
        }
    }

    @Test
    fun `not loading a module should throw`() {
        // Given
        val module1 = module {
            singleton<Serializer<Int>> { UserSerializer() }
        }

        val module2 = module {
            singleton<Serializer<String>> { CredentialsSerializer() }
        }

        // When
        Miko.loadModules(module1)

        // Then
        assertDoesNotThrow {
            val serializer1 by inject<Serializer<Int>>()
        }
        assertFails {
            val serializer2 by inject<Serializer<String>>()
        }
    }

    @Test
    fun `unloadModules should clear all the dependencies`() {
        // Given
        val module1 = module {
            singleton<Serializer<Int>> { UserSerializer() }
        }

        val module2 = module {
            singleton<Serializer<String>> { CredentialsSerializer() }
        }

        // When
        Miko.loadModules(module1, module2)
        Miko.unloadModules()

        // Then
        assertFails {
            val serializer1 by inject<Serializer<Int>>()
            val serializer2 by inject<Serializer<String>>()
        }
        assertEquals(0, Miko.container.size)
        assertEquals(0, Miko.constructorCache.size)
    }
}
