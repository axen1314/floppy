package org.axen.floppy.compiler

import com.google.auto.service.AutoService
import com.squareup.javapoet.*
import org.axen.floppy.annotation.BindFloppyMethod
import org.axen.floppy.annotation.BindFloppyInterceptor
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.util.Types


@AutoService(Processor::class)
class FloppyProcessor: AbstractProcessor() {
    companion object {
        const val FLOPPY_PACKAGE: String = "org.axen.floppy.core"
        const val FLOPPY_BINDING_PACKAGE: String = "org.axen.floppy.binding"
        const val FLOPPY_DELEGATE_CLASS: String = "${FLOPPY_PACKAGE}.FloppyDelegate"
        const val FLOPPY_FLOPPY_CLASS: String = "Floppy"
        const val FLOPPY_DELEGATE_BINDING_CLASS_NAME: String = "FloppyDelegateBinding"
        const val FLOPPY_INTERCEPTOR_BINDING_CLASS_NAME: String = "FloppyInterceptorBinding"
    }

    private var filer: Filer? = null
    private var messager: Messager? = null
    private var typeUtils: Types? = null
    private var delegate: Element? = null

    override fun init(processingEnv: ProcessingEnvironment?) {
        super.init(processingEnv)
        filer = processingEnv?.filer
        messager = processingEnv?.messager
        typeUtils = processingEnv?.typeUtils
        delegate = processingEnv?.elementUtils?.getTypeElement(FLOPPY_DELEGATE_CLASS)
    }

    override fun process(p0: MutableSet<out TypeElement>?, p1: RoundEnvironment?): Boolean {
        filer?.let { filer ->
            delegate?.let {
                brewProvider(p1, it)?.writeTo(filer)
                brewInterceptor(p1, it)?.writeTo(filer)
            }
        }
        return true
    }

    override fun getSupportedAnnotationTypes(): MutableSet<String> {
        return mutableSetOf(
            BindFloppyMethod::class.java.canonicalName,
            BindFloppyInterceptor::class.java.canonicalName
        )
    }

    override fun getSupportedSourceVersion(): SourceVersion {
        return SourceVersion.latestSupported()
    }

    private fun brewProvider(p1: RoundEnvironment?, delegate: Element): JavaFile? {
        val elements = p1?.getElementsAnnotatedWith(BindFloppyMethod::class.java)
        if (!elements.isNullOrEmpty()) {
            val codeBlock = CodeBlock.builder()
            for (element in elements) {
                if (element.kind != ElementKind.CLASS) {
                    throw FloppyException("Only support for field of annotation: $element")
                }
                val isAssignable = typeUtils?.isAssignable(element.asType(), delegate.asType())
                if (isAssignable == true) {
                    val annotation = element.getAnnotation(BindFloppyMethod::class.java)
                    val methodName = annotation.value
                    codeBlock.addStatement(
                        "floppy.addDelegate(\$S, new \$T())",
                        methodName,
                        ClassName.get(element as TypeElement)
                    )
                }
            }
            val parameterName = ClassName.get(FLOPPY_PACKAGE, FLOPPY_FLOPPY_CLASS)
            val method = MethodSpec
                .methodBuilder("bind")
                .addParameter(parameterName, "floppy")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addCode(codeBlock.build())
            val type = TypeSpec
                .classBuilder(FLOPPY_DELEGATE_BINDING_CLASS_NAME)
                .addMethod(method.build())
                .build()
            return JavaFile.builder(FLOPPY_BINDING_PACKAGE, type).build()
        }
        return null
    }

    private fun brewInterceptor(p1: RoundEnvironment?, delegate: Element): JavaFile? {
        val elements = p1?.getElementsAnnotatedWith(BindFloppyInterceptor::class.java)
        if (!elements.isNullOrEmpty()) {
            if (elements.size > 1) {
                throw FloppyException("BindFloppyInterceptor should have only one!")
            }
            val codeBlock = CodeBlock.builder()
            for (element in elements) {
                if (element.kind != ElementKind.CLASS) {
                    throw FloppyException("Only support for field of annotation: $element")
                }
                val isAssignable = typeUtils?.isAssignable(element.asType(), delegate.asType())
                if (isAssignable == true) {
                    codeBlock.addStatement(
                        "floppy.setInterceptor(new \$T())",
                        ClassName.get(element as TypeElement)
                    )
                    break
                }
            }
            val parameterName = ClassName.get(FLOPPY_PACKAGE, FLOPPY_FLOPPY_CLASS)
            val method = MethodSpec
                .methodBuilder("bind")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(parameterName, "floppy")
                .addCode(codeBlock.build())
            val type = TypeSpec
                .classBuilder(FLOPPY_INTERCEPTOR_BINDING_CLASS_NAME)
                .addMethod(method.build())
                .build()
            return JavaFile.builder(FLOPPY_PACKAGE, type).build()
        }
        return null
    }
}