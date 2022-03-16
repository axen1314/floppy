package org.axen.floppy.core

class FloppyHandler(private val callback: FloppyCallback) {
    fun success(value: Any?) {
        callback.onSuccess(value)
    }
}