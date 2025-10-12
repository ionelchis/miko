package com.ionelchis.miko

import com.ionelchis.miko.Miko.inject
import com.ionelchis.miko.model.Qualifier
import com.ionelchis.miko.model.module
import com.ionelchis.miko.model.modulesInit
import com.ionelchis.miko.model.typeKey

interface Serializer<T> {
    fun serialize()
}

data class EntityA(
    val id: Int
)

class EntityASerializer : Serializer<EntityA> {
    override fun serialize() {
        println("Serializing EntityA")
    }
}

data class EntityB(
    val id: Int
)

class EntityBSerializer : Serializer<EntityB> {
    override fun serialize() {
        println("Serializing EntityB")
    }
}

class UseCaseA(
    val serializerA: Serializer<EntityA>
)

class UseCaseB(
    val useCaseA: UseCaseA,
    val serializerB: Serializer<EntityB>
)

class SomeRepository<T>(
    val serializer: Serializer<T>
)

class A(
    val b: B,
    val ss: Serializer<ULong>
)

class C(
    val a: A
)

class B(
    val c: C
)


fun main() {
//    module {
//        factory<Serializer<EntityA>> { EntityASerializer() }
//        factory<Serializer<EntityB>> { EntityBSerializer() }
//
//        singleton<UseCaseA> {
//            UseCaseA(get())
//        }
//    }
//
//    val useCaseB: UseCaseB by inject()
//    val useCaseA: UseCaseA by inject()
//    val serializerA: Serializer<EntityA> by inject()
//
//    useCaseB.useCaseA.serializerA.serialize()
//    useCaseB.serializerB.serialize()
//
//    useCaseA.serializerA.serialize()
//
//    println(useCaseA == useCaseB.useCaseA)
//    println(useCaseA.serializerA == useCaseB.useCaseA.serializerA)
//    println(useCaseA.serializerA == serializerA)
//
//    for (i in 0..10) {
//        val repoA by inject<SomeRepository<EntityA>>()
//        val repoB by inject<SomeRepository<EntityB>>()
//    }


    val validatorModule = module {
        factory<Serializer<String>>(Password) {
            object : Serializer<String> {
                override fun serialize() {
                    println("Serialize string password")
                }
            }
        }

        factory<Serializer<String>>(Email) {
            object : Serializer<String> {
                override fun serialize() {
                    println("Serialize string email")
                }
            }
        }
    }

    Miko.init(validatorModule)

    val passwordSerializer by inject<Serializer<String>>(Password)
    val emailSerializer by inject<Serializer<String>>(Email)

    passwordSerializer.serialize()
    emailSerializer.serialize()

    val result = DependencyAnalyzer.analyze(typeKey<A>())
    println("Missing dependencies:")
    result.missingDependencies.forEach { println(" - ${it.type} ${it.qualifier?.name}") }

    println("Cycles detected:")
    result.cycles.forEach { cycle ->
        println(" - " + cycle.joinToString(" -> ") { it.type.toString() })
    }

}

object Password : Qualifier {
    override val name: String = "password"
}


object Email : Qualifier {
    override val name: String = "password"
}
