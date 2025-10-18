package com.ionelchis.miko

import com.ionelchis.miko.Miko.get
import com.ionelchis.miko.model.moduleLoad
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.BeforeTest
import kotlin.test.assertEquals

class MikoScopeBehaviorTest {

    @BeforeTest
    fun before() = Miko.unloadModules()

    @Test
    fun `singleton should be cached thread-safely`() = runBlocking {
        // Given
        moduleLoad {
            singleton { UUID.randomUUID().toString() }
        }

        // When
        val results = (1..100).map {
            async { get<String>() }
        }.awaitAll()

        // Then
        assertEquals(1, results.toSet().size)
    }

    @Test
    fun `factory should always create new instances`() {
        // Given
        moduleLoad {
            factory { UUID.randomUUID().toString() }
        }

        // When
        val instances = (1..10).map { get<String>() }

        // Then
        assertEquals(10, instances.toSet().size)
    }
}
