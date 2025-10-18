package com.ionelchis.miko

import com.ionelchis.miko.Miko.inject
import org.junit.jupiter.api.Test
import kotlin.test.BeforeTest
import kotlin.test.assertFails

class MikoErrorHandlingTest {

    class NoPrimaryConstructor private constructor()

    @BeforeTest
    fun before() = Miko.unloadModules()

    @Test
    fun `should throw when dependency cannot be constructed`() {
        assertFails {
            val instance by inject<NoPrimaryConstructor>()
        }
    }
}
