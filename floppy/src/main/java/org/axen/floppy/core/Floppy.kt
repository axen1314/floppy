package org.axen.floppy.core

import android.content.Context
import java.lang.Exception

class Floppy private constructor() {

    companion object {
        val instance: Floppy by lazy { Floppy() }
        private const val BINDING_PACKAGE: String = "org.axen.floppy.binding"
        private const val BINDING_DELEGATE_CLASS: String = "$BINDING_PACKAGE.FloppyDelegateBinding"
        private const val BINDING_INTERCEPTOR_CLASS: String = "$BINDING_PACKAGE.FloppyInterceptorBinding"
    }

    var interceptor: FloppyDelegate? = null

    private val delegates: MutableMap<String, FloppyDelegate> = mutableMapOf()

    init {
        try {
            val dgClass = Class.forName(BINDING_DELEGATE_CLASS)
            val dgGetMethod = dgClass.getDeclaredMethod("bind", Floppy::class.java)
            dgGetMethod.invoke(null, this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            val itpClass = Class.forName(BINDING_INTERCEPTOR_CLASS)
            val itpGetMethod = itpClass.getDeclaredMethod("bind", Floppy::class.java)
            itpGetMethod.invoke(null, this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun builder(name: String): FloppyBuilder {
        return FloppyBuilder(name)
    }

    private fun invoke(
        context: Context? = null,
        method: String,
        arguments: Any? = null,
        callback: FloppyCallback? = null
    ) {
        val delegate: FloppyDelegate? = delegates[method]
        val handler = if (callback == null) null else FloppyHandler(callback);
        delegate?.let {

            val result = it.invoke(context, method, arguments, handler)
            callback?.onSuccess(result)
        } ?: interceptor?.invoke(context, method, arguments, handler)
    }

    fun getDelegate(method: String): FloppyDelegate? = delegates[method]

    fun addDelegate(method: String, delegate: FloppyDelegate) {
        delegates[method] = delegate
    }

    fun removeDelegate(method: String) {
        delegates.remove(method)
    }

    class FloppyBuilder(private val method: String) {
        private var context: Context? = null
        private var arguments: Any? = null
        private var callback: FloppyCallback? = null

        fun context(context: Context?) : FloppyBuilder {
            this.context = context
            return this
        }

        fun arguments(arguments: Any?) : FloppyBuilder {
            this.arguments = arguments
            return this
        }

        fun callback(callback: FloppyCallback?) : FloppyBuilder {
            this.callback = callback
            return this
        }

        fun invoke() {
            instance.invoke(context, method, arguments, callback)
        }
    }
}