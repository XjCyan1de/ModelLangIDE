package com.github.xjcyan1de.modellangide

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.awt.*
import java.awt.event.*
import java.io.File
import java.io.IOException
import javax.swing.*
import javax.swing.undo.UndoManager
import kotlin.system.exitProcess

const val MAX_X = 2
const val MAX_Y = 1
private val FONT = Font.decode(Font.MONOSPACED)

val BACKGROUND_COLOR = Color(0x2b2b2b)
val BACKGROUND_PRIMARY_COLOR = Color(0x313335)
val TEXT_COLOR = Color(0xbbbbbb)

object GUI : JFrame("Model Language IDE"), CoroutineScope by GlobalScope {
    init {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    }

    private var OnTop = false
    private var xoff = 0
    private var yoff = 0
    private var xOffScreen = 0
    private var yOffScreen = 0
    private var maximised = false
    private var origW = 0
    private var origH = 0
    private var origX = 0
    private var origY = 0

    val undoManager = UndoManager()

    val contentPane = object : JPanel() {
        init {
            layout = GridBagLayout()
            border = null
            background = BACKGROUND_COLOR
            this@GUI.setContentPane(this)
        }

        override fun paintComponent(g: Graphics) {
            super.paintComponent(g)
            g.color = BACKGROUND_COLOR
            g.drawRect(0, 0, width, height)
        }
    }
    val bar: JMenuBar = object : JMenuBar() {
        init {
            border = BorderFactory.createLineBorder(BACKGROUND_PRIMARY_COLOR)

            addMouseListener(object : MouseListener {
                override fun mouseClicked(e: MouseEvent) {}
                override fun mouseEntered(e: MouseEvent) {}
                override fun mouseExited(e: MouseEvent) {}
                override fun mousePressed(e: MouseEvent) {
                    xoff = e.x
                    yoff = e.y
                    xOffScreen = e.xOnScreen
                    yOffScreen = e.yOnScreen
                }

                override fun mouseReleased(e: MouseEvent) {
                    if (e.yOnScreen <= 3) toggleMax(false, MAX_X or MAX_Y)
                    if (this@GUI.y < 0) setLocation(this@GUI.x, 0)
                }
            })
        }

        override fun paintComponent(g: Graphics) {
            super.paintComponent(g)
            g.color = BACKGROUND_PRIMARY_COLOR
            g.fillRect(0, 0, width+5, height+5)
        }
    }
    val fileMenu = JMenu("File").apply {
        foreground = TEXT_COLOR
    }
    val editMenu = JMenu("Edit").apply {
        foreground = TEXT_COLOR
    }
    val viewMenu = JMenu("View").apply {
        foreground = TEXT_COLOR
    }
    val runMenu = JMenu("Run").apply {
        foreground = TEXT_COLOR
    }
    val newItem = JMenuItem("New").apply {
        addActionListener { newFile() }
        accelerator = KeyStroke.getKeyStroke('N'.toInt(), Toolkit.getDefaultToolkit().menuShortcutKeyMask)
    }
    val openItem = JMenuItem("Open").apply {
        addActionListener { openFile() }
        accelerator = KeyStroke.getKeyStroke('O'.toInt(), Toolkit.getDefaultToolkit().menuShortcutKeyMask)
    }
    val saveItem = JMenuItem("Save").apply {
        addActionListener { saveFile() }
        accelerator = KeyStroke.getKeyStroke('S'.toInt(), Toolkit.getDefaultToolkit().menuShortcutKeyMask)
    }
    val saveAsItem = JMenuItem("Save As").apply {
        addActionListener { saveAsFile() }
    }
    val undoItem = JMenuItem("Undo").apply {
        addActionListener { if (undoManager.canUndo()) undoManager.undo() else Toolkit.getDefaultToolkit().beep() }
        accelerator = KeyStroke.getKeyStroke('Z'.toInt(), Toolkit.getDefaultToolkit().menuShortcutKeyMask)
    }
    val redoItem = JMenuItem("Redo").apply {
        addActionListener { if (undoManager.canRedo()) undoManager.redo() else Toolkit.getDefaultToolkit().beep() }
        accelerator = KeyStroke.getKeyStroke('Y'.toInt(), Toolkit.getDefaultToolkit().menuShortcutKeyMask)
    }
    val onTopItem = JCheckBoxMenuItem("Always On Top").apply {
        addActionListener { e: ActionEvent? -> toggleOnTop() }
    }
    val runItem = JMenuItem("Run").apply {
        addActionListener {
            val sb = StringBuilder()
            val environment = Environment {
                sb.appendln(it)
            }

            try {
                val result = Parser(TokenReader(CharReader((this@GUI.textPane.text ?: "").toCharArray()))).parse()
                result.forEach {
                    println(it)
                }

                environment.evaluate(result)
            } catch (e: CharReader.ReaderException) {
                sb.appendln(e.localizedMessage)
                println(e.localizedMessage)
            } catch (e: Exception) {
                sb.appendln(e.localizedMessage)
                e.printStackTrace()
            }

            openDialogWindow(sb.toString(), "Run")
        }
    }
    val closeButton = JLabel(" X ").apply {
        foreground = TEXT_COLOR
        cursor = Cursor(Cursor.HAND_CURSOR)
        border = BorderFactory.createLineBorder(BACKGROUND_COLOR)
        addMouseListener(object : MouseListener {
            override fun mouseEntered(e: MouseEvent) {
                foreground = Color.RED
            }
            override fun mouseExited(e: MouseEvent) {
                foreground = TEXT_COLOR
            }
            override fun mousePressed(e: MouseEvent) {}
            override fun mouseReleased(e: MouseEvent) {}
            override fun mouseClicked(e: MouseEvent) {
                dispose()
                exitProcess(0)
            }
        })
    }
    val minButton = JLabel(" _ ").apply {
        foreground = TEXT_COLOR
        cursor = Cursor(Cursor.HAND_CURSOR)
        border = BorderFactory.createLineBorder(BACKGROUND_COLOR)
        addMouseListener(object : MouseListener {
            override fun mouseEntered(e: MouseEvent) {
                foreground = Color.WHITE
            }
            override fun mouseExited(e: MouseEvent) {
                foreground = TEXT_COLOR
            }
            override fun mousePressed(e: MouseEvent) {}
            override fun mouseReleased(e: MouseEvent) {}
            override fun mouseClicked(e: MouseEvent) {
                state = ICONIFIED
            }
        })
    }
    val maxButton = JLabel(" \u1010 ").apply {
        foreground = TEXT_COLOR
        cursor = Cursor(Cursor.HAND_CURSOR)
        border = BorderFactory.createLineBorder(BACKGROUND_COLOR)
        addMouseListener(object : MouseListener {
            override fun mouseEntered(e: MouseEvent) {
                foreground = Color.WHITE
            }
            override fun mouseExited(e: MouseEvent) {
                foreground = TEXT_COLOR
            }
            override fun mousePressed(e: MouseEvent) {}
            override fun mouseReleased(e: MouseEvent) {}
            override fun mouseClicked(e: MouseEvent) {
                toggleMax(false, MAX_X or MAX_Y)
            }
        })
    }
    val textPane: JTextPane = JTextPane().apply {
        border = null
        font = FONT
        background = BACKGROUND_COLOR
        foreground = TEXT_COLOR
        caretColor = TEXT_COLOR
        document.addUndoableEditListener(undoManager)
    }

    val lineNumberPane: JTextPane = object : JTextPane() {
        private var lastLines = 0

        init {
            background = BACKGROUND_PRIMARY_COLOR
            foreground = TEXT_COLOR
            isFocusable = false
            isEditable = false
            font = FONT
            text = ""
            launch {
                while (true) {
                    updateLineNumbers()
                }
            }
        }

        fun updateLineNumbers() {
            val lines = this@GUI.textPane.text?.split("\n") ?: emptyList()
            if (lastLines != lines.size) {
                val nums = if (lastLines < lines.size) {
                    StringBuilder(this.text).apply {
                        for (i in lastLines + 1..lines.size) {
                            appendln(i)
                        }
                    }
                } else {
                    StringBuilder().apply {
                        for (i in 1..lines.size) {
                            appendln(i)
                        }
                    }
                }
                text = nums.toString()
                lastLines = lines.size
            }
        }

        override fun paintComponent(g: Graphics) {
            super.paintComponent(g)
            g.color = BACKGROUND_COLOR
            g.fillRect(width - 1, 0, 1, height)
        }
    }


    init {
        defaultCloseOperation = EXIT_ON_CLOSE
        isUndecorated = true

        bar.addMouseMotionListener(object : MouseMotionListener {
            override fun mouseMoved(e: MouseEvent) {}
            override fun mouseDragged(e: MouseEvent) {
                if (!maximised) {
                    setLocation(e.x + x - xoff, e.y + y - yoff)
                } else toggleMax(true, MAX_X or MAX_Y)
            }
        })
        jMenuBar = bar

        bar.add(fileMenu)
        bar.add(editMenu)
        bar.add(viewMenu)
        bar.add(runMenu)

        bar.add(Box.createGlue())
        bar.add(minButton)
        bar.add(JLabel(" "))
        bar.add(maxButton)
        bar.add(JLabel(" "))
        bar.add(closeButton)

        fileMenu.add(newItem)
        fileMenu.add(openItem)
        fileMenu.add(saveItem)
        fileMenu.add(saveAsItem)
        editMenu.add(undoItem)
        editMenu.add(redoItem)

        viewMenu.add(onTopItem) //View Items
        runMenu.add(runItem)

        val gbc = GridBagConstraints().apply {
            gridx = 1
            gridy = 1
            fill = GridBagConstraints.BOTH
            anchor = GridBagConstraints.CENTER
            weightx = 1.0
            weighty = 1.0
        }

        val noWrap = JPanel(BorderLayout()).apply {
            border = null
        }
        val scroll = JScrollPane(noWrap).apply {
            border = null
            verticalScrollBar.unitIncrement = 10
        }
        noWrap.add(textPane, BorderLayout.CENTER)
        noWrap.add(lineNumberPane, BorderLayout.WEST)
        contentPane.add(scroll, gbc)

        addWindowListener(object : WindowListener {
            override fun windowClosed(e: WindowEvent) {}
            override fun windowClosing(e: WindowEvent) {}
            override fun windowDeiconified(e: WindowEvent) {}
            override fun windowIconified(e: WindowEvent) {}
            override fun windowOpened(e: WindowEvent) {}
            override fun windowActivated(e: WindowEvent) {
                if (OnTop) e.window.opacity = 1f
            }

            override fun windowDeactivated(e: WindowEvent) {
                if (OnTop) e.window.opacity = 0.8f
            }
        })
        pack()
        setSize(500, 350)
        setLocationRelativeTo(null)
        isVisible = true
    }

    fun toggleOnTop() {
        OnTop = !OnTop
        isAlwaysOnTop = OnTop
    }

    fun toggleMax(drag: Boolean, direction: Int) {
        if (!maximised) {
            origW = width
            origH = height
            origX = x
            origY = y
            when (direction) {
                2 -> extendedState = MAXIMIZED_HORIZ
                1 -> extendedState = MAXIMIZED_VERT
                3 -> {
                    val usableBounds = GraphicsEnvironment.getLocalGraphicsEnvironment().maximumWindowBounds
                    maximizedBounds = usableBounds
                    extendedState = MAXIMIZED_BOTH
                }
            }
            state = NORMAL
            maximised = true
        } else if (!drag) {
            state = NORMAL
            setSize(origW, origH)
            setLocation(origX, if (origY < 0) 0 else origY)
            maximised = false
        } else {
            state = NORMAL
            setSize(origW, origH)
            setLocation(xOffScreen - origW / 2, yOffScreen)
            xoff = xOffScreen - xoff + origW / 2
            yoff = yOffScreen
            maximised = false
        }
    }

    var filePath: String? = null

    fun newFile() {
        filePath = null
        textPane.text = ""
    }

    fun openFile() {
        if (OnTop) isAlwaysOnTop = false
        val dialog = FileDialog(this, "Open", FileDialog.LOAD)
        dialog.isVisible = true
        if (dialog.file != null) {
            filePath = dialog.directory + dialog.file
            textPane.text = File(filePath).readLines().joinToString("\n")
        }
        if (OnTop) isAlwaysOnTop = true
    }

    fun saveFile() {
        if (filePath == null) {
            if (OnTop) isAlwaysOnTop = false
            val dialog = FileDialog(this, "Save", FileDialog.SAVE)
            dialog.isVisible = true
            if (dialog.file != null) {
                filePath = dialog.directory + dialog.file
                try {
                    val file = File(filePath)
                    file.createNewFile()
                    file.bufferedWriter().use {
                        it.write(textPane.text)
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            if (OnTop) isAlwaysOnTop = true
        } else {
            try {
                File(filePath).bufferedWriter().use {
                    it.write(textPane.text)
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    fun saveAsFile() {
        if (OnTop) isAlwaysOnTop = false
        val dialog = FileDialog(this, "Save as", FileDialog.SAVE)
        dialog.isVisible = true
        if (dialog.file != null) {
            filePath = dialog.directory + dialog.file
            try {
                val file = File(filePath)
                file.bufferedWriter().use {
                    it.write(textPane.text)
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        if (OnTop) isAlwaysOnTop = true
    }

    fun openDialogWindow(text: String, title: String = "") {
        val dialog = JDialog(this@GUI, title).apply {
            setSize(300, 100)
            isVisible = true
            setLocationRelativeTo(null)
        }

        val textArea = JTextArea()
        textArea.isEditable = false
        textArea.font = FONT
        textArea.isVisible = true
        textArea.text = text
        textArea.foreground = TEXT_COLOR
        textArea.background = BACKGROUND_COLOR

        dialog.add(textArea)
        dialog.pack()
    }
}
