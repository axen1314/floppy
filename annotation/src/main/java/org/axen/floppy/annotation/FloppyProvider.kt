package org.axen.floppy.annotation

import java.lang.annotation.Inherited
import kotlin.annotation.Target
import kotlin.annotation.Retention

@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.ANNOTATION_CLASS
)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Inherited
annotation class FloppyProvider(val value: String)
