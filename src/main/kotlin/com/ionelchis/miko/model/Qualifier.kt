package com.ionelchis.miko.model

/**
 * Marker interface used to differentiate multiple bindings of the same type.
 *
 * A [Qualifier] is an identifier (usually a name or annotation) that allows
 * multiple instances of the same type to coexist in the dependency graph.
 * For example, you can have two serializers:
 * one qualified as `"Json"` and another as `"Xml"`.
 *
 * @see Named
 */
interface Qualifier {
    /** The unique name associated with this qualifier. */
    val name: String
}

/**
 * Simple [Qualifier] implementation that uses a string name to distinguish bindings.
 *
 * Example:
 * ```
 * Miko.singleton<Validator<String>>(Named("Email")) { EmailValidator() }
 * Miko.singleton<Validator<String>>(Named("Password")) { PasswordValidator() }
 * ```
 *
 * @property name A human-readable identifier for the qualified binding.
 */
data class Named(override val name: String) : Qualifier
