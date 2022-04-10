package org.axen.floppy.compiler

import javax.lang.model.element.Element

data class FloppyHolder (
    val annotation: Annotation,
    val element: Element
)