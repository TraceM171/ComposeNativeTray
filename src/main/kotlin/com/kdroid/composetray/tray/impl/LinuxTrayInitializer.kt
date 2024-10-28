package com.kdroid.composetray.tray.impl

import com.kdroid.composetray.lib.linux.appindicator.AppIndicator
import com.kdroid.composetray.lib.linux.appindicator.AppIndicatorCategory
import com.kdroid.composetray.lib.linux.appindicator.AppIndicatorStatus
import com.kdroid.composetray.lib.linux.appindicator.Gtk
import com.kdroid.composetray.lib.linux.gdk.Gdk
import com.kdroid.composetray.lib.linux.gdk.GdkRectangle
import com.kdroid.composetray.lib.linux.gtkstatusicon.GtkStatusIcon
import com.kdroid.composetray.lib.linux.gtkstatusicon.GtkStatusIconActivateCallback
import com.kdroid.composetray.menu.api.TrayMenuBuilder
import com.kdroid.composetray.menu.impl.LinuxTrayMenuBuilderImpl
import com.kdroid.composetray.utils.convertPositionToCorner
import com.kdroid.kmplog.Log
import com.kdroid.kmplog.d
import com.sun.jna.Pointer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicReference

object LinuxTrayInitializer {
    private var currentIndicator = AtomicReference<Pointer?>(null)
    private var currentStatusIcon = AtomicReference<Pointer?>(null)
    private var currentMenu = AtomicReference<Pointer?>(null)
    private var currentMenuBuilder = AtomicReference<LinuxTrayMenuBuilderImpl?>(null)
    private var currentCallback = AtomicReference<GtkStatusIconActivateCallback?>(null)
    private val scope = CoroutineScope(Dispatchers.Default)
    private var isInitialized = AtomicReference(false)

    private var instanceCount = 0

    fun dispose() {
        scope.launch {
            try {
                // Clean the menu builder
                currentMenuBuilder.get()?.dispose()
                currentMenuBuilder.set(null)
                // Clean the indicator
                currentIndicator.get()?.let { indicator ->
                    // Deactivate the indicator before destroying it
                    AppIndicator.INSTANCE.app_indicator_set_status(indicator, AppIndicatorStatus.PASSIVE)
                    // Ensure the menu is destroyed before continuing
                    Gtk.INSTANCE.gtk_widget_hide(currentMenu.get())
                    currentIndicator.set(null)
                }
                // Clean the status icon
                currentStatusIcon.get()?.let { statusIcon ->
                    GtkStatusIcon.INSTANCE.gtk_status_icon_set_visible(statusIcon, 0)
                    currentCallback.get()?.let { callback ->
                        // Disconnect the signal
                        GtkStatusIcon.INSTANCE.g_signal_handlers_disconnect_matched(
                            statusIcon,
                            0,
                            0,
                            null,
                            null,
                            null,
                            null
                        )
                    }
                    currentStatusIcon.set(null)
                    currentCallback.set(null)
                }
                // Clean the menu
                currentMenu.get()?.let { menu ->
                    Gtk.INSTANCE.gtk_widget_destroy(menu)
                    currentMenu.set(null)
                }
                // Force a small delay to ensure everything is cleaned
                Thread.sleep(100)
                // Quit the GTK main loop if it is running
                if (isInitialized.get()) {
                    Gtk.INSTANCE.gtk_main_quit()
                    isInitialized.set(false)
                }
            } catch (e: Exception) {
                Log.d("LinuxTrayInitializer", "Error during dispose: ${e.message}")
            }
        }
    }
    fun initialize(
        iconPath: String,
        tooltip: String,
        primaryAction: (() -> Unit)?,
        primaryActionLinuxLabel: String,
        menuContent: (TrayMenuBuilder.() -> Unit)?
    ) {
        // Clean the previous instance if it exists
        if (isInitialized.get()) {
            dispose()
            // Wait for the cleaning to be completed
            Thread.sleep(200)
        }
        instanceCount++
        val currentInstanceId = "compose-tray-${instanceCount}"
        // Initialize GTK
        if (!isInitialized.get()) {
            Gtk.INSTANCE.gtk_init(0, Pointer.createConstant(0))
        }
        if (menuContent != null) {
            try {
                // Use AppIndicator with menu
                val indicator = AppIndicator.INSTANCE.app_indicator_new(
                    currentInstanceId, iconPath, AppIndicatorCategory.APPLICATION_STATUS
                )
                currentIndicator.set(indicator)
                // Build the menu first
                val menu = Gtk.INSTANCE.gtk_menu_new()
                currentMenu.set(menu)
                val trayMenuBuilder = LinuxTrayMenuBuilderImpl(menu)
                currentMenuBuilder.set(trayMenuBuilder)
                // Configure the menu before attaching it to the indicator
                primaryAction?.let {
                    trayMenuBuilder.Item(primaryActionLinuxLabel) {
                        val display = Gdk.INSTANCE.gdk_display_get_default()
                        val seat = Gdk.INSTANCE.gdk_display_get_default_seat(display)
                        val pointer = Gdk.INSTANCE.gdk_seat_get_pointer(seat)
                        val x = IntArray(1)
                        val y = IntArray(1)
                        Gdk.INSTANCE.gdk_device_get_position(pointer, null, x, y)
                        val monitor = Gdk.INSTANCE.gdk_display_get_primary_monitor(display)
                        val geometry = GdkRectangle()
                        Gdk.INSTANCE.gdk_monitor_get_geometry(monitor, geometry)
                        val width = geometry.width
                        val height = geometry.height
                        Log.d("LinuxTrayInitializer", convertPositionToCorner(x[0], y[0], width, height).toString())
                        Log.d("LinuxTrayInitializer", "Menu item clicked at position: x=${x[0]}, y=${y[0]}")
                        it.invoke()
                    }
                }
                trayMenuBuilder.apply(menuContent)
                // Show the menu before attaching it to the indicator
                Gtk.INSTANCE.gtk_widget_show_all(menu)
                // Set the tooltip
                AppIndicator.INSTANCE.app_indicator_set_title(indicator, tooltip)
                // Now attach the menu to the indicator
                AppIndicator.INSTANCE.app_indicator_set_menu(indicator, menu)
                // Finally, activate the indicator
                AppIndicator.INSTANCE.app_indicator_set_status(indicator, AppIndicatorStatus.ACTIVE)
            } catch (e: Exception) {
                Log.d("LinuxTrayInitializer", "Error initializing AppIndicator: ${e.message}")
                dispose()
                return
            }
        } else if (primaryAction != null) {
            try {
                // Use GtkStatusIcon without menu
                val statusIcon = GtkStatusIcon.INSTANCE.gtk_status_icon_new_from_file(iconPath)
                currentStatusIcon.set(statusIcon)
                val callback = object : GtkStatusIconActivateCallback {
                    override fun invoke(status_icon: Pointer?, event_button: Int, event_time: Int) {
                        primaryAction.invoke()
                    }
                }
                currentCallback.set(callback)
                GtkStatusIcon.INSTANCE.g_signal_connect_data(
                    statusIcon,
                    "activate",
                    callback,
                    null,
                    null,
                    0
                )
                GtkStatusIcon.INSTANCE.gtk_status_icon_set_visible(statusIcon, 1)
            } catch (e: Exception) {
                Log.d("LinuxTrayInitializer", "Error initializing StatusIcon: ${e.message}")
                dispose()
                return
            }
        } else {
            Log.d("LinuxTrayInitializer", "No menu content or primary action provided for tray icon.")
        }
        isInitialized.set(true)
        // Start the GTK main loop
        if (isInitialized.get()) {
            Gtk.INSTANCE.gtk_main()
        }
    }
}