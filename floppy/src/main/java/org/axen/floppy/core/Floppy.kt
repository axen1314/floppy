package org.axen.floppy.core

import android.content.Context
import java.lang.Exception

class Floppy private constructor() {

    companion object {
        val instance: Floppy by lazy { Floppy() }
        const val BINDING_DELEGATE_CLASS: String = "org.axen.floppy.core.FloppyDelegateBinding"
        const val BINDING_INTERCEPTOR_CLASS: String = "org.axen.floppy.core.FloppyInterceptorBinding"
    }

    var interceptor: FloppyDelegate? = null

    val delegates: MutableMap<String, FloppyDelegate> = mutableMapOf()

    init {
        try {
            val dgClass = Class.forName(BINDING_DELEGATE_CLASS)
            val dgGetMethod = dgClass.getDeclaredMethod("get")
            val map = dgGetMethod.invoke(null) as Map<String, FloppyDelegate>
            delegates.putAll(map)
        } catch (e: Exception) {}
        try {
            val itpClass = Class.forName(BINDING_INTERCEPTOR_CLASS)
            val itpGetMethod = itpClass.getDeclaredMethod("get")
            interceptor = itpGetMethod.invoke(null) as FloppyDelegate
        } catch (e: Exception) {}
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

    class FloppyBuilder(private val name: String) {
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
            instance.invoke(context, name, arguments, callback)
        }
    }
}