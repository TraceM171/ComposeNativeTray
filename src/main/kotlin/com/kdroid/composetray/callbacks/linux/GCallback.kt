package com.kdroid.composetray.callbacks.linux

import com.sun.jna.Callback
import com.sun.jna.Pointer

internal interface GCallback : Callback {
    fun callback(widget: Pointer, data: Pointer?)
}
