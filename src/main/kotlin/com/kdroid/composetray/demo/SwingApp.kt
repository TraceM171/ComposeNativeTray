package com.kdroid.composetray.demo

import java.awt.*
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.*

fun main() {
    SwingAppDemo()
    MemoryLogger.logMemoryUsage()
}

private fun SwingAppDemo() {
    // Create the main window
    val frame = JFrame("Swing App with Native System Tray").apply {
        setSize(500, 300)
        minimumSize = Dimension(400, 200)
        defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        setLocationRelativeTo(null) // Center the window
    }

    // Create the main panel with an empty border for better spacing
    val panel = JPanel().apply {
        layout = GridBagLayout()
        border = BorderFactory.createEmptyBorder(20, 20, 20, 20)
    }

    val constraints = GridBagConstraints().apply {
        gridx = 0
        gridy = 0
        insets = Insets(10, 10, 10, 10)
        fill = GridBagConstraints.HORIZONTAL
        weightx = 1.0
    }

    // Add components to the main panel with custom fonts and colors
    val label = JLabel("Name:").apply {
        font = Font("Arial", Font.BOLD, 16)
    }
    panel.add(label, constraints)

    val textField = JTextField().apply {
        font = Font("Arial", Font.PLAIN, 16)
        background = Color(230, 240, 255)
    }
    constraints.gridx = 1
    panel.add(textField, constraints)

    val button = JButton("Submit").apply {
        font = Font("Arial", Font.BOLD, 16)
        background = Color(100, 149, 237)
        foreground = Color.WHITE
        isFocusPainted = false
    }
    constraints.gridx = 0
    constraints.gridy = 1
    constraints.gridwidth = 2
    panel.add(button, constraints)

    val resultLabel = JLabel("").apply { // Initially empty
        font = Font("Arial", Font.ITALIC, 16)
        foreground = Color(34, 139, 34)
    }
    constraints.gridy = 2
    panel.add(resultLabel, constraints)

    // Add an action to the button
    button.addActionListener {
        val text = textField.text
        if (text.isNotEmpty()) {
            resultLabel.text = "Hello, $text!"
        }
    }

    // Add a listener to resize components
    frame.addComponentListener(object : ComponentAdapter() {
        override fun componentResized(e: ComponentEvent) {
            panel.revalidate()
            panel.repaint()
        }
    })

    // Add the main panel to the window
    frame.contentPane.add(panel, BorderLayout.CENTER)

    // Make the window visible
    frame.isVisible = true
}



