package com.ionelchis.miko

import com.ionelchis.miko.Miko.inject
import com.ionelchis.miko.model.Named
import com.ionelchis.miko.model.Scope
import com.ionelchis.miko.model.moduleLoad
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import kotlin.test.BeforeTest

class MikoQualifierTest {

    @BeforeTest
    fun before() = Miko.unloadModules()

    @Test
    fun `should resolve dependencies by qualifier`() {
        // Given
        moduleLoad {
            singleton(Named("json")) { "JsonSerializer" }
            singleton(Named("xml")) { "XmlSerializer" }
        }

        // When
        val json by inject<String>(Named("json"))
        val xml by inject<String>(Named("xml"))

        // Then
        assertEquals("JsonSerializer", json)
        assertEquals("XmlSerializer", xml)
        assertNotEquals(json, xml)
    }

    @Test
    fun `should resolve bind dependencies by qualifier`() {
        // Given
        moduleLoad {
            bind(Scope.Singleton,Named("json")) { "JsonSerializer" }
            bind(Scope.Singleton, Named("xml")) { "XmlSerializer" }
        }

        // When
        val json by inject<String>(Named("json"))
        val xml by inject<String>(Named("xml"))

        // Then
        assertEquals("JsonSerializer", json)
        assertEquals("XmlSerializer", xml)
        assertNotEquals(json, xml)
    }
}
