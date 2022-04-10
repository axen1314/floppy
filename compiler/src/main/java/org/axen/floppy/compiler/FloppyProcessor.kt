package org.axen.floppy.compiler

import com.google.auto.service.AutoService
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.TypeSpec
import org.axen.floppy.annotation.FloppyGateway
import org.axen.floppy.annotation.FloppyProvider
import org.axen.floppy.compiler.FloppyConstants.FLOPPY_BINDING_PACKAGE
import org.axen.floppy.compiler.FloppyConstants.FLOPPY_DELEGATE_BINDING_CLASS_NAME
import org.axen.floppy.compiler.FloppyConstants.FLOPPY_FLOPPY_CLASS
import org.axen.floppy.compiler.FloppyConstants.FLOPPY_INTERCEPTOR_BINDING_CLASS_NAME
import org.axen.floppy.compiler.FloppyConstants.FLOPPY_PACKAGE
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.util.Types


@AutoService(Processor::class)
class FloppyProcessor: AbstractProcessor() {

    private lateinit var filer: Filer
    private lateinit var messager: Messager
    private lateinit var typeUtils: Types
    private lateinit var holder: FloppyElementsHolder

    private lateinit var pConverter: AnnotatedElementSetCodeBuilder<Element>
    private lateinit var gConverter: ElementSetCodeBuilder<Element>

    override fun init(processingEnv: ProcessingEnvironment?) {
        super.init(processingEnv)
        processingEnv?.let {
            filer = it.filer
            messager = it.messager
            typeUtils = it.typeUtils
            holder = FloppyElementsHolder(it.elementUtils)
            pConverter = AnnotatedElementSetCodeBuilder(
                FloppyProvider::class.java,
                ElementCodeBuilder(
                    classKind = FloppyClassKindCodeBuilder(
                        it.typeUtils,
                        holder.delegateElement,
                        FloppyProviderCodeBuilder(
                            TypeElementFloppyDelegateBuilder()
                        )
                    ),
                    methodKind = FloppyMethodKindCodeBuilder(
                        holder,
                        it.typeUtils,
                        messager,
                        FloppyProviderCodeBuilder(
                            ExecutableElementFloppyDelegateBuilder(
                                holder
                            )
                        )
                    ),
                    messager
                ),
                messager,
            )
            gConverter = ElementSetCodeBuilder(
                FloppyGateway::class.java,
                ElementCodeBuilder(
                    classKind = FloppyClassKindCodeBuilder(
                        it.typeUtils,
                        holder.delegateElement,
                        FloppyGatewayCodeBuilder(
                            TypeElementFloppyDelegateBuilder()
                        )
                    ),
                    methodKind = FloppyMethodKindCodeBuilder(
                        holder,
                        it.typeUtils,
                        messager,
                        FloppyGatewayCodeBuilder(
                            ExecutableElementFloppyDelegateBuilder(
                                holder
                            )
                        )
                    ),
                    messager
                ),
                messager,
            )
        }
    }

    override fun process(p0: MutableSet<out TypeElement>?, p1: RoundEnvironment?): Boolean {
        brewProviders(p1)?.writeTo(filer)
        brewGateway(p1)?.writeTo(filer)
        return true
    }

    override fun getSupportedAnnotationTypes(): MutableSet<String> {
        return mutableSetOf(
            FloppyProvider::class.java.canonicalName,
            FloppyGateway::class.java.canonicalName
        )
    }

    override fun getSupportedSourceVersion(): SourceVersion {
        return SourceVersion.latestSupported()
    }

    private fun brewProviders(p1: RoundEnvironment?): JavaFile? {
        val elements = p1?.getElementsAnnotatedWith(FloppyProvider::class.java)
        if (!elements.isNullOrEmpty()) {
            return pConverter.build(p1, elements)?.let {
                if (!it.isEmpty) {
                    val parameterName = ClassName.get(FLOPPY_PACKAGE, FLOPPY_FLOPPY_CLASS)
                    val method = MethodSpec
                        .methodBuilder("bind")
                        .addParameter(parameterName, "floppy")
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .addCode(it)
                    val type = TypeSpec
                        .classBuilder(FLOPPY_DELEGATE_BINDING_CLASS_NAME)
                        .addMethod(method.build())
                        .build()
                    return JavaFile.builder(FLOPPY_BINDING_PACKAGE, type).build()
                }
                null
            }

        }
        return null
    }

    private fun brewGateway(p1: RoundEnvironment?): JavaFile? {
        val elements = p1?.getElementsAnnotatedWith(FloppyGateway::class.java)
        if (!elements.isNullOrEmpty()) {
            if (elements.size > 1) {
                throw FloppyException("BindFloppyInterceptor should have only one!")
            }
            return gConverter.build(p1, elements)?.let {
                if (!it.isEmpty) {
                    val parameterName = ClassName.get(FLOPPY_PACKAGE, FLOPPY_FLOPPY_CLASS)
                    val method = MethodSpec
                        .methodBuilder("bind")
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .addParameter(parameterName, "floppy")
                        .addCode(it)
                    val type = TypeSpec
                        .classBuilder(FLOPPY_INTERCEPTOR_BINDING_CLASS_NAME)
                        .addMethod(method.build())
                        .build()
                    return JavaFile.builder(FLOPPY_PACKAGE, type).build()
                }
                null
            }
        }
        return null
    }
}