package com.kdroid.composetray.callbacks.windows

import WindowsNativeTrayMenuItem
import com.sun.jna.win32.StdCallLibrary

internal fun interface StdCallCallback : StdCallLibrary.StdCallCallback {
    fun invoke(item: WindowsNativeTrayMenuItem)
}