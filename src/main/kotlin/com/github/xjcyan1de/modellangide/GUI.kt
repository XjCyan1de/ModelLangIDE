package com.github.xjcyan1de.modellangide

import java.awt.*
import javax.swing.*
import javax.swing.undo.UndoManager

object GUI : JFrame("Model Lang IDE") {
    val undoManager = UndoManager()

    val contentPane = JPanel().apply {
        layout = GridBagLayout()
    }
    val text = JTextPane().apply {
        background = Color(0x2b2b2b)
        foreground = Color(0xbbbbbb)
        caretColor = Color(0xbbbbbb)
        font = Font(Font.MONOSPACED, Font.TRUETYPE_FONT, 20)
        document.addUndoableEditListener(undoManager)
    }
    val output = JTextArea().apply {
        background = Color(0x2b2b2b)
        foreground = Color(0xbbbbbb)
        caretColor = Color(0xbbbbbb)
        font = Font(Font.MONOSPACED, Font.TRUETYPE_FONT, 20)
        isEditable = false
    }

    init {
//        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())

        defaultCloseOperation = EXIT_ON_CLOSE
        isUndecorated = false

        setContentPane(contentPane)

        val scroll = JScrollPane(JPanel(BorderLayout()).apply {
            add(text, BorderLayout.CENTER)
        })

        val scroll2 = JScrollPane(JPanel(BorderLayout()).apply {
            add(output, BorderLayout.CENTER)
        })

        contentPane.add(scroll, GridBagConstraints().apply {
            gridx = 1
            gridy = 1
            fill = GridBagConstraints.BOTH
            anchor = GridBagConstraints.CENTER
            weightx = 1.0
            weighty = 1.0
        })
        contentPane.add(scroll2, GridBagConstraints().apply {
            gridx = 1
            gridy = 2
            fill = GridBagConstraints.BOTH
            anchor = GridBagConstraints.CENTER
            weightx = 1.0
            weighty = 1.0
        })

        pack()
        setSize(500, 350)
        isVisible = true
    }
}