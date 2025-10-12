package com.ionelchis.miko.model

import java.util.concurrent.atomic.AtomicReference

/**
 * Internal wrapper that manages how a dependency instance is created and cached
 * based on its [Scope].
 *
 * A [Provider] is responsible for supplying instances of a given type [T].
 * It either:
 * - Creates a **new instance** on each request (for [Scope.Factory]).
 * - Lazily initializes and caches a **single instance** (for [Scope.Singleton]).
 *
 * This class is **thread-safe** for [Scope.Singleton] bindings using
 * an [AtomicReference] to ensure that only one instance is created even under
 * concurrent access.
 *
 * @param T The type of object this provider supplies.
 * @property scope Defines whether the provider should act as a factory or a singleton.
 * @property instantiator A function that constructs a new instance of [T].
 *
 * @see Scope.Singleton
 * @see Scope.Factory
 */
internal class Provider<T>(
    private val scope: Scope,
    private val instantiator: () -> T,
) {
    /**
     *  Atomic reference used to cache the singleton instance in a thread-safe manner.
     */
    private val instanceRef = AtomicReference<T?>(null)

    /**
     * Provides an instance of [T] according to the configured [scope].
     *
     * - For [Scope.Factory], a new instance is created each time.
     * - For [Scope.Singleton], the same instance is returned across calls.
     *   The first thread to call this method will initialize and store the instance.
     *
     * @return An instance of [T] consistent with the provider's [scope].
     */
    fun provide(): T {
        return when (scope) {
            Scope.Factory -> instantiator()
            Scope.Singleton -> instanceRef.get() ?: run {
                val created = instantiator()
                if (instanceRef.compareAndSet(null, created)) {
                    created
                } else {
                    instanceRef.get()!! // another thread set it
                }
            }

        }
    }
}
