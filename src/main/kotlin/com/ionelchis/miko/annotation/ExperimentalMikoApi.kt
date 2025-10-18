package com.ionelchis.miko.annotation

@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "This API is experimental and not stable at the moment."
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
annotation class ExperimentalMikoApi
