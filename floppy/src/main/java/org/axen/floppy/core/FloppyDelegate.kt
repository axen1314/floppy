package org.axen.floppy.core

import android.content.Context

interface FloppyDelegate {
    fun invoke(
        context: Context?,
        method: String,
        arguments: Any?,
        handler: FloppyHandler?
    )
}