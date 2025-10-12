package com.ionelchis.miko.model

/**
 * Represents the lifecycle or instantiation behavior of a dependency within Miko.
 *
 * Each binding defines whether it should produce a new instance on every request
 * or reuse a single instance across the application's lifetime.
 *
 * @see Singleton
 * @see Factory
 */
sealed interface Scope {

    /**
     * A scope that guarantees a single instance of a dependency.
     *
     * The instance is created lazily upon first resolution and then cached for
     * all subsequent injections.
     *
     * This is the default behavior when auto-constructing dependencies
     * that are not explicitly bound.
     */
    data object Singleton : Scope

    /**
     * A scope that creates a new instance of a dependency for each resolution.
     *
     * Use this when the dependency should not be shared (e.g., when it maintains
     * mutable internal state or is short-lived).
     */
    data object Factory : Scope
}
