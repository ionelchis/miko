package com.ionelchis.miko

import com.ionelchis.miko.Miko.inject
import com.ionelchis.miko.model.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

internal class MikoTest {

    internal class Foo


    @BeforeTest
    fun before() {

    }

    @AfterTest
    fun after() {
        Miko.clear()
    }

    @Test
    fun `singleton returns instance`() {
        // Given
        val module = module {
            singleton<Foo> { Foo() }
        }

        Miko.init(module)

        // When
        val foo1 by inject<Foo>()
        val foo2 by inject<Foo>()

        // Then
        assertEquals(foo1, foo2)
    }
}
