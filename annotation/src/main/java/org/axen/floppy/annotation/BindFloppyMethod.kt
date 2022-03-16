package org.axen.floppy.annotation

import kotlin.annotation.Target
import kotlin.annotation.Retention

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class BindFloppyMethod(val value: String)
