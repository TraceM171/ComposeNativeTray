package com.kdroid.composetray.tray.impl

import com.kdroid.composetray.lib.linux.AppIndicator
import com.kdroid.composetray.lib.linux.AppIndicatorCategory
import com.kdroid.composetray.lib.linux.AppIndicatorStatus
import com.kdroid.composetray.lib.linux.Gtk
import com.kdroid.composetray.menu.impl.LinuxTrayMenuImpl
import com.kdroid.composetray.menu.TrayMenu
import com.sun.jna.Pointer

object LinuxTrayInitializer {
    fun initialize(iconPath: String, menuContent: TrayMenu.() -> Unit) {
        // Initialize GTK
        Gtk.INSTANCE.gtk_init(0, Pointer.createConstant(0))

        // Create the indicator
        val indicator = AppIndicator.INSTANCE.app_indicator_new(
            "compose-tray",
            iconPath,
            AppIndicatorCategory.APPLICATION_STATUS
        )

        AppIndicator.INSTANCE.app_indicator_set_status(indicator, AppIndicatorStatus.ACTIVE)

        // Build the menu
        val menu = Gtk.INSTANCE.gtk_menu_new()
        LinuxTrayMenuImpl(menu).apply(menuContent)
        AppIndicator.INSTANCE.app_indicator_set_menu(indicator, menu)
        Gtk.INSTANCE.gtk_widget_show_all(menu)

        // Start the GTK loop in a separate thread
        Thread {
            Gtk.INSTANCE.gtk_main()
        }.start()
    }
}