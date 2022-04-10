package org.axen.floppy.example

import android.content.Context
import android.util.Log
import org.axen.floppy.annotation.FloppyProvider
import org.axen.floppy.core.FloppyDelegate
import org.axen.floppy.core.FloppyHandler

@FloppyProvider("haha")
fun haha(
    context: Context?,
    methodName: String,
    arguments: Any?,
    handler: FloppyHandler?
) {
    Log.e("111", "111")
}

@InitBuildConfig
fun haha2(
    context: Context?,
    methodName: String,
    arguments: Any?,
    handler: FloppyHandler?
) {
    Log.e("111", "111")
}

@FloppyProvider("222")
class FloppyTestDelegate: FloppyDelegate {
    override fun invoke(
        context: Context?,
        method: String,
        arguments: Any?,
        handler: FloppyHandler?
    ) {
        Log.e("111", "111")
    }
}

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@FloppyProvider("2222")
annotation class InitBuildConfig
