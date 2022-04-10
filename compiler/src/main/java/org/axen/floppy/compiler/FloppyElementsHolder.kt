package org.axen.floppy.compiler

import org.axen.floppy.compiler.FloppyConstants.CONTEXT_CLASS
import org.axen.floppy.compiler.FloppyConstants.FLOPPY_DELEGATE_CLASS
import org.axen.floppy.compiler.FloppyConstants.FLOPPY_HANDLER_CLASS
import javax.lang.model.element.TypeElement
import javax.lang.model.util.Elements

class FloppyElementsHolder(elementUtils: Elements) {
    val objectElement: TypeElement = elementUtils.getTypeElement(Object::class.java.name)
    val stringElement: TypeElement = elementUtils.getTypeElement(String::class.java.name)

    val contextElement: TypeElement = elementUtils.getTypeElement(CONTEXT_CLASS)

    val delegateElement: TypeElement = elementUtils.getTypeElement(FLOPPY_DELEGATE_CLASS)
    val handlerElement: TypeElement = elementUtils.getTypeElement(FLOPPY_HANDLER_CLASS)

}