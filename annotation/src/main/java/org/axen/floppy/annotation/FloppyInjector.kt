package org.axen.floppy.annotation

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE)
@Retention(AnnotationRetention.RUNTIME)
annotation class FloppyInjector(val value: String)
