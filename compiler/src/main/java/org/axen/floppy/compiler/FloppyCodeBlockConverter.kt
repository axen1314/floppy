package org.axen.floppy.compiler

import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import org.axen.floppy.annotation.FloppyProvider
import javax.annotation.processing.Messager
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.*
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.Types
import javax.tools.Diagnostic

interface CodeBuilder<T> {
    fun build(p1: RoundEnvironment, target: T): CodeBlock?
}

class AnnotatedElementSetCodeBuilder<T: Element>(
    private val annotationClass: Class<out Annotation>,
    private val builder: CodeBuilder<FloppyHolder>,
    private val messager: Messager? = null
): CodeBuilder<Set<T>> {
    override fun build(
        p1: RoundEnvironment,
        target: Set<T>
    ): CodeBlock? {
        val setBuilder = CodeBlock.builder()
        for (element in target) {
            val annotation = element.getAnnotation(annotationClass)
            buildElement(p1, annotation, element)?.let {
                messager?.printMessage(
                    Diagnostic.Kind.WARNING,
                    it.toString()
                )
                setBuilder.addStatement(it)
            }
        }
        return setBuilder.build()
    }

    private fun buildElement(
        p1: RoundEnvironment,
        annotation: Annotation,
        element: Element
    ): CodeBlock? {
        return when (element.kind) {
            ElementKind.ANNOTATION_TYPE -> {
                val annoCodeBlock = CodeBlock.builder()
                val annotationElement = element as TypeElement
                val annotatedElements = p1.getElementsAnnotatedWith(annotationElement)
                for (annotatedElement in annotatedElements) {
                    val codeBlock = buildElement(p1, annotation, annotatedElement)
                    codeBlock?.let { annoCodeBlock.add(it) }
                }
                annoCodeBlock.build()
            }
            else -> {
                val holder = FloppyHolder(annotation, element)
                builder.build(p1, holder)
            }
        }
    }
}

open class ElementSetCodeBuilder<T: Element>(
    private val annotationClass: Class<out Annotation>,
    private val builder: CodeBuilder<FloppyHolder>,
    private val messager: Messager? = null,
): CodeBuilder<Set<T>> {
    override fun build(
        p1: RoundEnvironment,
        target: Set<T>
    ): CodeBlock? {
        val setBuilder = CodeBlock.builder()
        for (element in target) {
            val annotation = element.getAnnotation(annotationClass)
            builder.build(p1, FloppyHolder(annotation, element))?.let {
                setBuilder.addStatement(it)
            }
        }
        return setBuilder.build()
    }
}

open class ElementCodeBuilder(
    private val classKind: CodeBuilder<FloppyHolder>? = null,
    private val methodKind: CodeBuilder<FloppyHolder>? = null,
    private val messager: Messager? = null,
): CodeBuilder<FloppyHolder> {

    override fun build(
        p1: RoundEnvironment,
        target: FloppyHolder
    ): CodeBlock? {
        val element = target.element
        messager?.printMessage(
            Diagnostic.Kind.WARNING,
            "kind: ${element.kind.name}, name: ${element.simpleName}"
        )
        val code = when(element.kind) {
            ElementKind.CLASS -> onClassKind(p1, target)
            ElementKind.METHOD -> onMethodKind(p1, target)
            else -> null
        }
        return code
    }

    private fun onClassKind(
        p1: RoundEnvironment,
        element: FloppyHolder
    ): CodeBlock? = classKind?.build(p1, element)
    private fun onMethodKind(
        p1: RoundEnvironment,
        element: FloppyHolder
    ): CodeBlock? = methodKind?.build(p1, element)
}

class FloppyClassKindCodeBuilder(
    private val typeUtils: Types,
    private val assignElement: TypeElement,
    private val builder: CodeBuilder<FloppyHolder>,
    private val messager: Messager?,
) : CodeBuilder<FloppyHolder> {

    constructor(
        typeUtils: Types,
        assignElement: TypeElement,
        builder: CodeBuilder<FloppyHolder>
    ) : this(typeUtils, assignElement, builder, null)

    override fun build(
        p1: RoundEnvironment,
        target: FloppyHolder
    ): CodeBlock? {
        val element = target.element
        return if (isAssignable(element)) {
            builder.build(p1, target)
        } else {
            messager?.printMessage(
                Diagnostic.Kind.ERROR,
                "Class ${element.simpleName} should extends from ${assignElement.simpleName}"
            )
            null
        }
    }

    private fun isAssignable(
        target: Element
    ) : Boolean = typeUtils.isAssignable(
        target.asType(), assignElement.asType()
    )
}

class FloppyMethodKindCodeBuilder(
    private val holder: FloppyElementsHolder,
    private val typeUtils: Types,
    private val messager: Messager? = null,
    private val builder: CodeBuilder<FloppyHolder>
) : CodeBuilder<FloppyHolder>  {

    constructor(
        holder: FloppyElementsHolder,
        typeUtils: Types,
        builder: CodeBuilder<FloppyHolder>
    ) : this(holder, typeUtils, null, builder)

    override fun build(
        p1: RoundEnvironment,
        target: FloppyHolder
    ): CodeBlock? {
        val element = target.element as ExecutableElement
        return if (isElementExecutable(element)) {
            builder.build(p1, target)
        } else null
    }

    private fun isElementExecutable(target: ExecutableElement) : Boolean {
        val modifiers = target.modifiers
        if (!modifiers.contains(Modifier.PUBLIC)) {
            messager?.printMessage(
                Diagnostic.Kind.ERROR,
                "Method ${target.simpleName} must be a public method!"
            )
            return false
        }
        if (!modifiers.contains(Modifier.STATIC)) {
            messager?.printMessage(
                Diagnostic.Kind.ERROR,
                "Method ${target.simpleName} must be a static method!"
            )
            return false
        }
        val parameters = target.parameters
        if (parameters.size != 4) {
            messager?.printMessage(
                Diagnostic.Kind.ERROR,
                "Method ${target.simpleName} should only have 4 parameter!"
            )
            return false
        }
        if (!isSameType(parameters[0], holder.contextElement)
            || !isSameType(parameters[1], holder.stringElement)
            || !isSameType(parameters[2], holder.objectElement)
            || !isSameType(parameters[3], holder.handlerElement)
        ) {
            messager?.printMessage(
                Diagnostic.Kind.ERROR,
                "Method ${target.simpleName} parameters' type is not right!"
            )
            return false
        }
        val returnType = target.returnType
        if (!isSameType(returnType, typeUtils.getNoType(TypeKind.VOID))) {
            messager?.printMessage(
                Diagnostic.Kind.ERROR,
                "Method ${target.simpleName}'s return type should be void!!"
            )
            return false
        }
        return true
    }

    private fun isSameType(
        element1: Element,
        element2: Element
    ) : Boolean = isSameType(element1.asType(), element2.asType())

    private fun isSameType(
        mirror1: TypeMirror,
        mirror2: TypeMirror
    ) : Boolean = typeUtils.isSameType(mirror1, mirror2)
}

class FloppyProviderCodeBuilder(
    private val builder: CodeBuilder<FloppyHolder>
) : CodeBuilder<FloppyHolder> {

    override fun build(
        p1: RoundEnvironment,
        target: FloppyHolder
    ): CodeBlock? {
        val annotation = target.annotation as FloppyProvider
        val methodName = annotation.value
        return CodeBlock.builder()
            .add("floppy.define(\$S, ", methodName)
            .add(builder.build(p1, target))
            .add(")")
            .build()
    }
}

class FloppyGatewayCodeBuilder(
    private val builder: CodeBuilder<FloppyHolder>
) : CodeBuilder<FloppyHolder> {

    override fun build(
        p1: RoundEnvironment,
        target: FloppyHolder
    ): CodeBlock? {
        return CodeBlock.builder()
            .add("floppy.setInterceptor(new ")
            .add(builder.build(p1, target))
            .add(")")
            .build()
    }
}

class TypeElementFloppyDelegateBuilder : CodeBuilder<FloppyHolder> {
    override fun build(
        p1: RoundEnvironment,
        target: FloppyHolder
    ): CodeBlock? {
        val element = target.element as TypeElement
        return CodeBlock.builder()
            .add("new \$T()", ClassName.get(element))
            .build()
    }
}

class ExecutableElementFloppyDelegateBuilder(
    private val holder: FloppyElementsHolder
): CodeBuilder<FloppyHolder> {
    override fun build(
        p1: RoundEnvironment,
        target: FloppyHolder
    ): CodeBlock? {
        val element = target.element
        return CodeBlock.builder()
            .add("new \$T() {\n", ClassName.get(holder.delegateElement))
            .add("@Override\n")
            .add("public void invoke(\n")
            .indent().indent()
            .add("\$T context,\n", ClassName.get(holder.contextElement))
            .add("\$T methodName,\n", ClassName.get(holder.stringElement))
            .add("\$T arguments,\n", ClassName.get(holder.objectElement))
            .add("\$T handler\n", ClassName.get(holder.handlerElement))
            .unindent().unindent()
            .add(") {\n")
            .indent().indent()
            .add(
                "${'$'}T.${element.simpleName}(context, methodName, arguments, handler);\n",
                ClassName.get(element.enclosingElement as TypeElement)
            )
            .add("}\n")
            .unindent().unindent()
            .add("}")
            .build()
    }

}

