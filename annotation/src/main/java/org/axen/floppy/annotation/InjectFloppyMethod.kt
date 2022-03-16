package org.axen.floppy.annotation

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class InjectFloppyMethod(val value: String)
