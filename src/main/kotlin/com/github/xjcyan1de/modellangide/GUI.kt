import com.github.xjcyan1de.modellangide.CharReader
import com.github.xjcyan1de.modellangide.Environment
import com.github.xjcyan1de.modellangide.Parser
import com.github.xjcyan1de.modellangide.TokenReader
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
private val FONT = Font("JetBrains Mono", 0, 14) // Шрифты не заружаются :c

val BACKGROUND_COLOR = Color(0x2b2b2b)
val BACKGROUND_PRIMARY_COLOR = Color(0x313335)
val TEXT_COLOR = Color(0xbbbbbb)

object GUI : JFrame("Model Language IDE"), CoroutineScope by GlobalScope {
    val contentPane: JPanel
    val bar: JMenuBar = object : JMenuBar() {
        init {
            border = null

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
            g.fillRect(0, 0, width, height)
        }
    }
    val fileMenu: JMenu
    val editMenu: JMenu
    val viewMenu: JMenu
    val runMenu: JMenu
    val newItem: JMenuItem
    val openItem: JMenuItem
    val saveItem: JMenuItem
    val saveAsItem: JMenuItem
    val undoItem: JMenuItem
    val redoItem: JMenuItem
    val onTopItem: JCheckBoxMenuItem
    val closeButton: JLabel
    val minButton: JLabel
    val maxButton: JLabel
    val undoManager = UndoManager()
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

    init {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())

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

        fileMenu = JMenu("File")
        fileMenu.font = FONT
        fileMenu.foreground = TEXT_COLOR
        bar.add(fileMenu)
        editMenu = JMenu("Edit")
        editMenu.font = FONT
        editMenu.foreground = TEXT_COLOR
        bar.add(editMenu)
        viewMenu = JMenu("View").apply {
            font = FONT
            foreground = TEXT_COLOR
        }
        bar.add(viewMenu)
        runMenu = JMenu("Run").apply {
            font = FONT
            foreground = TEXT_COLOR
        }
        bar.add(runMenu)

        closeButton = JLabel(" X ")
        closeButton.foreground = TEXT_COLOR
        closeButton.cursor = Cursor(Cursor.HAND_CURSOR)
        closeButton.border = BorderFactory.createLineBorder(BACKGROUND_COLOR)
        closeButton.addMouseListener(object : MouseListener {
            override fun mouseEntered(e: MouseEvent) {}
            override fun mouseExited(e: MouseEvent) {}
            override fun mousePressed(e: MouseEvent) {}
            override fun mouseReleased(e: MouseEvent) {}
            override fun mouseClicked(e: MouseEvent) {
                dispose()
                exitProcess(0)
            }
        })
        maxButton = JLabel(" \u1010 ")
        maxButton.foreground = TEXT_COLOR
        maxButton.cursor = Cursor(Cursor.HAND_CURSOR)
        maxButton.border = BorderFactory.createLineBorder(BACKGROUND_COLOR)
        maxButton.addMouseListener(object : MouseListener {
            override fun mouseEntered(e: MouseEvent) {}
            override fun mouseExited(e: MouseEvent) {}
            override fun mousePressed(e: MouseEvent) {}
            override fun mouseReleased(e: MouseEvent) {}
            override fun mouseClicked(e: MouseEvent) {
                toggleMax(false, MAX_X or MAX_Y)
            }
        })
        minButton = JLabel(" _ ")
        minButton.foreground = TEXT_COLOR
        minButton.cursor = Cursor(Cursor.HAND_CURSOR)
        minButton.border = BorderFactory.createLineBorder(BACKGROUND_COLOR)
        minButton.addMouseListener(object : MouseListener {
            override fun mouseEntered(e: MouseEvent) {}
            override fun mouseExited(e: MouseEvent) {}
            override fun mousePressed(e: MouseEvent) {}
            override fun mouseReleased(e: MouseEvent) {}
            override fun mouseClicked(e: MouseEvent) {
                state = ICONIFIED
            }
        })
        bar.add(Box.createGlue())
        bar.add(minButton)
        bar.add(JLabel(" "))
        bar.add(maxButton)
        bar.add(JLabel(" "))
        bar.add(closeButton)
        newItem = JMenuItem("New")
        fileMenu.add(newItem) //File Items
        newItem.addActionListener { newFile() }
        newItem.accelerator = KeyStroke.getKeyStroke('N'.toInt(), Toolkit.getDefaultToolkit().menuShortcutKeyMask)
        openItem = JMenuItem("Open")
        fileMenu.add(openItem)
        openItem.addActionListener { openFile() }
        openItem.accelerator = KeyStroke.getKeyStroke('O'.toInt(), Toolkit.getDefaultToolkit().menuShortcutKeyMask)
        saveItem = JMenuItem("Save")
        fileMenu.add(saveItem)
        saveItem.addActionListener { saveFile() }
        saveItem.accelerator = KeyStroke.getKeyStroke('S'.toInt(), Toolkit.getDefaultToolkit().menuShortcutKeyMask)
        saveAsItem = JMenuItem("Save As")
        fileMenu.add(saveAsItem)
        saveAsItem.addActionListener { saveAsFile() }
        undoItem = JMenuItem("Undo")
        editMenu.add(undoItem) //Edit Items
        undoItem.addActionListener { if (undoManager.canUndo()) undoManager.undo() else Toolkit.getDefaultToolkit().beep() }
        undoItem.accelerator = KeyStroke.getKeyStroke('Z'.toInt(), Toolkit.getDefaultToolkit().menuShortcutKeyMask)
        redoItem = JMenuItem("Redo")
        editMenu.add(redoItem)
        redoItem.addActionListener { if (undoManager.canRedo()) undoManager.redo() else Toolkit.getDefaultToolkit().beep() }
        redoItem.accelerator = KeyStroke.getKeyStroke('Y'.toInt(), Toolkit.getDefaultToolkit().menuShortcutKeyMask)
        onTopItem = JCheckBoxMenuItem("Always On Top")
        viewMenu.add(onTopItem) //View Items
        onTopItem.addActionListener { e: ActionEvent? -> toggleOnTop() }
        runMenu.add(JMenuItem("Run").apply {
            addActionListener {
                val sb = StringBuilder()

                val environment = Environment {
                    sb.appendln(it)
                }

                try {
                    val result = Parser(TokenReader(CharReader((this@GUI.textPane.text ?: "").toCharArray()))).parse()
                    environment.evaluate(result)
                } catch (e: CharReader.ReaderException) {
                    sb.appendln(e.localizedMessage)
                    println(e.localizedMessage)
                } catch (e: Exception) {
                    sb.appendln(e.localizedMessage)
                    e.printStackTrace()
                }

                openDialogWindow(sb.toString(),"Run")
            }
        })
        contentPane = object : JPanel() {
            init {
                layout = GridBagLayout()
                border = null
            }
            override fun paintComponent(g: Graphics) {
                super.paintComponent(g)
                g.color = BACKGROUND_COLOR
                g.drawRect(0,0,width+5, height+5)
            }
        }
        setContentPane(contentPane)
        contentPane.background = BACKGROUND_COLOR
        val gbc = GridBagConstraints()
        gbc.gridx = 1
        gbc.gridy = 1
        gbc.fill = GridBagConstraints.BOTH
        gbc.anchor = GridBagConstraints.CENTER
        gbc.weightx = 1.0
        gbc.weighty = 1.0
        val noWrap = JPanel(BorderLayout())
        val scroll = JScrollPane(noWrap)
        scroll.border = null
        scroll.verticalScrollBar.unitIncrement = 10
        noWrap.add(textPane, BorderLayout.CENTER)
        noWrap.add(lineNumberPane, BorderLayout.WEST)
        contentPane.add(scroll, gbc)

        gbc.gridx = 0
        gbc.gridy = 1
        gbc.anchor = GridBagConstraints.LINE_START
        gbc.weightx = -1.0
        val dragLeft = JSeparator(1)
        dragLeft.cursor = Cursor(Cursor.W_RESIZE_CURSOR)
        dragLeft.addMouseMotionListener(object : MouseMotionListener {
            override fun mouseMoved(e: MouseEvent) {}
            override fun mouseDragged(e: MouseEvent) {
                var size = width - e.x
                if (size < 105) {
                    size = 105
                }
                setSize(size, height)
                setLocation(x + e.x, y)
            }
        })
        contentPane.add(dragLeft, gbc)
        gbc.gridx = 2
        gbc.gridy = 1
        gbc.anchor = GridBagConstraints.LINE_END
        gbc.weightx = -1.0
        val dragRight = JSeparator(1)
        dragRight.cursor = Cursor(Cursor.E_RESIZE_CURSOR)
        dragRight.addMouseMotionListener(object : MouseMotionListener {
            override fun mouseMoved(e: MouseEvent) {}
            override fun mouseDragged(e: MouseEvent) {
                var size = width + e.x
                if (size < 105) size = 105
                setSize(size, height)
            }
        })
        contentPane.add(dragRight, gbc)
        gbc.gridx = 1
        gbc.gridy = 2
        gbc.anchor = GridBagConstraints.PAGE_END
        gbc.weighty = -1.0
        val dragDown = JSeparator(0)
        dragDown.cursor = Cursor(Cursor.S_RESIZE_CURSOR)
        dragDown.addMouseMotionListener(object : MouseMotionListener {
            override fun mouseMoved(e: MouseEvent) {}
            override fun mouseDragged(e: MouseEvent) {
                var size = height + e.y
                if (size < 75) size = 75
                setSize(width, size)
                if (e.yOnScreen >= Toolkit.getDefaultToolkit().screenSize.height - 1) toggleMax(false, MAX_Y)
            }
        })
        contentPane.add(dragDown, gbc)
        gbc.gridx = 2
        gbc.gridy = 2
        gbc.anchor = GridBagConstraints.LAST_LINE_END
        gbc.weightx = -0.1
        gbc.weighty = -0.1
        val dragSE = JLabel("")
        dragSE.cursor = Cursor(Cursor.SE_RESIZE_CURSOR)
        dragSE.addMouseMotionListener(object : MouseMotionListener {
            override fun mouseMoved(e: MouseEvent) {}
            override fun mouseDragged(e: MouseEvent) {
                var sizex = width + e.x
                var sizey = height + e.y
                if (sizex < 105) sizex = 105
                if (sizey < 75) sizey = 75
                setSize(sizex, sizey)
            }
        })
        contentPane.add(dragSE, gbc)
        gbc.gridx = 0
        gbc.gridy = 2
        gbc.anchor = GridBagConstraints.LAST_LINE_START
        gbc.weightx = -0.1
        gbc.weighty = -0.1
        val dragSW = JLabel("")
        dragSW.cursor = Cursor(Cursor.SW_RESIZE_CURSOR)
        dragSW.addMouseMotionListener(object : MouseMotionListener {
            override fun mouseMoved(e: MouseEvent) {}
            override fun mouseDragged(e: MouseEvent) {
                var sizex = width - e.x
                var sizey = height + e.y
                var movex = x + e.x
                if (sizex < 105) {
                    sizex = 105
                    movex = x
                }
                if (sizey < 75) sizey = 75
                setSize(sizex, sizey)
                setLocation(movex, y)
            }
        })
        contentPane.add(dragSW, gbc)
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
        val dialog = JDialog(this@GUI,title).apply {
            setSize(300,100)
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
