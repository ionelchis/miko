package com.ionelchis.miko

import com.ionelchis.miko.Miko.inject
import com.ionelchis.miko.model.moduleLoad
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import kotlin.test.BeforeTest

class MikoReflectionInjectionTest {

    data class Serializer<T>(val type: String)
    data class Repository<T>(val serializer: Serializer<T>)
    data class User(val id: Int)
    data class UserUseCase(val repo: Repository<User>)

    @BeforeTest
    fun before() = Miko.unloadModules()

    @Test
    fun `should resolve generic types via reflection`() {
        // Given
        moduleLoad {
            singleton { Serializer<User>("UserSerializer") }
        }

        // When
        val repo by inject<Repository<User>>()
        val useCase by inject<UserUseCase>()

        // Then
        assertEquals(repo.serializer.type, "UserSerializer")
        assertSame(repo, useCase.repo)
    }
}
