import com.github.xjcyan1de.modellangide.CharReader
import com.github.xjcyan1de.modellangide.Environment
import com.github.xjcyan1de.modellangide.Parser
import com.github.xjcyan1de.modellangide.TokenReader
import java.awt.*
import java.awt.event.*
import java.awt.image.BufferedImage
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import javax.swing.*
import javax.swing.undo.UndoManager
import kotlin.concurrent.thread
import kotlin.system.exitProcess

var MAX_X = 2
var MAX_Y = 1
private val FONT = Font("JetBrains Mono", 0, 14)

val BACKGROUND_COLOR = Color(0x2b2b2b)
val BACKGROUND_PRIMARY_COLOR = Color(0x313335)
val TEXT_COLOR = Color(0xbbbbbb)

object Scratch : JFrame("Model Language IDE") {
    private val contentPane: JPanel
    private val bar: JMenuBar = object : JMenuBar() {
        init {
//            border = BorderFactory.createTitledBorder(null, title, 2, 3, FONT, TEXT_COLOR)
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
                    if (this@Scratch.y < 0) setLocation(this@Scratch.x, 0)
                }
            })
        }

        override fun paintComponent(g: Graphics) {
            super.paintComponent(g)
            g.color = BACKGROUND_PRIMARY_COLOR
            g.fillRect(0-10, 0-10, width+10, height+10)
        }
    }
    private val file: JMenu
    private val edit: JMenu
    private val view: JMenu
    private val run: JMenu
    private val newItem: JMenuItem
    private val openItem: JMenuItem
    private val Save: JMenuItem
    private val saveAsItem: JMenuItem
    private val undoItem: JMenuItem
    private val Redo: JMenuItem
    private val onTop: JCheckBoxMenuItem
    private val XButton: JLabel
    private val minButton: JLabel
    private val MaxButton: JLabel
    private val undo = UndoManager()
    private val text: JTextPane = JTextPane().apply {
        border = null
        font = FONT
        background = BACKGROUND_COLOR
        foreground = TEXT_COLOR
        caretColor = TEXT_COLOR
        document.addUndoableEditListener(undo)
    }

    private var lineNumber: JTextPane = object : JTextPane() {
        init {
            background = BACKGROUND_PRIMARY_COLOR
            foreground = TEXT_COLOR
            isFocusable = false
            isEditable = false
            font = FONT
            text = ""
            thread {
                var lastLines = 0
                while (true) {
                    val lines = this@Scratch.text?.text?.split("\n") ?: emptyList()
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

    var screenSize = Toolkit.getDefaultToolkit().screenSize
    var width = screenSize.getWidth()
    var height = screenSize.getHeight()
    private var icon = BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB)


    fun setTitleAll(title: String) {
        this.title = title
//        bar.border = BorderFactory.createTitledBorder(null, title, 2, 3, font, TEXT_COLOR)
        setTitle(title)
    }

    fun toggleOnTop() {
        OnTop = !OnTop
        isAlwaysOnTop = OnTop
    }

    fun toggleMax(drag: Boolean, direction: Int) {
        if (!maximised) {
            origW = getWidth()
            origH = getHeight()
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

    //IO
    var changed = false
    var filePath: String? = null
    fun New() {
        filePath = null
        text.text = ""
        setTitleAll("Model Language IDE - New File")
    }

    fun Open() {
        if (OnTop) isAlwaysOnTop = false
        val dialog = FileDialog(this, "Open", FileDialog.LOAD)
        dialog.isVisible = true
        if (dialog.file != null) {
            filePath = dialog.directory + dialog.file
            text.text = File(filePath).readLines().joinToString("\n")
            setTitleAll("Model Language IDE - " + dialog.file)
        }
        if (OnTop) isAlwaysOnTop = true
    }

    fun Save() {
        if (filePath == null) {
            if (OnTop) isAlwaysOnTop = false
            val dialog = FileDialog(this, "Save", FileDialog.SAVE)
            dialog.isVisible = true
            if (dialog.file != null) {
                filePath = dialog.directory + dialog.file
                val fw: FileWriter
                try {
                    val file = File(filePath)
                    file.createNewFile()
                    fw = FileWriter(filePath)
                    val bw = BufferedWriter(fw)
                    bw.write(text.text)
                    bw.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
                setTitleAll("Model Language IDE - " + dialog.file)
            }
            if (OnTop) isAlwaysOnTop = true
        } else {
            val fw: FileWriter
            try {
                fw = FileWriter(filePath)
                val bw = BufferedWriter(fw)
                bw.write(text.text)
                bw.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    fun SaveAs() {
        if (OnTop) isAlwaysOnTop = false
        val dialog = FileDialog(this, "Save as", FileDialog.SAVE)
        dialog.isVisible = true
        if (dialog.file != null) {
            filePath = dialog.directory + dialog.file
            val fw: FileWriter
            try {
                val file = File(filePath)
                if (file.exists()) file.createNewFile()
                fw = FileWriter(filePath)
                val bw = BufferedWriter(fw)
                bw.write(text.text)
                bw.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            setTitleAll("Model Language IDE - " + dialog.file)
        }
        if (OnTop) isAlwaysOnTop = true
    }

    init {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())

        defaultCloseOperation = EXIT_ON_CLOSE
        title = title
        isUndecorated = true

        bar.addMouseMotionListener(object : MouseMotionListener {
            override fun mouseMoved(e: MouseEvent) {}
            override fun mouseDragged(e: MouseEvent) {
                if (!maximised) {
                    setLocation(e.x + x - xoff, e.y + y - yoff)
                } else toggleMax(true, MAX_X or MAX_Y)
            }
        })
        bar.background = BACKGROUND_PRIMARY_COLOR
        jMenuBar = bar

        file = JMenu("File")
        file.font = FONT
        file.foreground = TEXT_COLOR
        bar.add(file)
        edit = JMenu("Edit")
        edit.font = FONT
        edit.foreground = TEXT_COLOR
        bar.add(edit)
        view = JMenu("View").apply {
            font = FONT
            foreground = TEXT_COLOR
        }
        bar.add(view)
        run = JMenu("Run").apply {
            font = FONT
            foreground = TEXT_COLOR
        }
        bar.add(run)

        XButton = JLabel(" X ")
        XButton.foreground = TEXT_COLOR
        XButton.cursor = Cursor(Cursor.HAND_CURSOR)
        XButton.border = BorderFactory.createLineBorder(BACKGROUND_COLOR)
        XButton.addMouseListener(object : MouseListener {
            override fun mouseEntered(e: MouseEvent) {}
            override fun mouseExited(e: MouseEvent) {}
            override fun mousePressed(e: MouseEvent) {}
            override fun mouseReleased(e: MouseEvent) {}
            override fun mouseClicked(e: MouseEvent) {
                dispose()
                exitProcess(0)
            }
        })
        MaxButton = JLabel(" \u1010 ")
        MaxButton.foreground = TEXT_COLOR
        MaxButton.cursor = Cursor(Cursor.HAND_CURSOR)
        MaxButton.border = BorderFactory.createLineBorder(BACKGROUND_COLOR)
        MaxButton.addMouseListener(object : MouseListener {
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
        bar.add(MaxButton)
        bar.add(JLabel(" "))
        bar.add(XButton)
        newItem = JMenuItem("New")
        file.add(newItem) //File Items
        newItem.addActionListener { New() }
        newItem.accelerator = KeyStroke.getKeyStroke('N'.toInt(), Toolkit.getDefaultToolkit().menuShortcutKeyMask)
        openItem = JMenuItem("Open")
        file.add(openItem)
        openItem.addActionListener { Open() }
        openItem.accelerator = KeyStroke.getKeyStroke('O'.toInt(), Toolkit.getDefaultToolkit().menuShortcutKeyMask)
        Save = JMenuItem("Save")
        file.add(Save)
        Save.addActionListener { Save() }
        Save.accelerator = KeyStroke.getKeyStroke('S'.toInt(), Toolkit.getDefaultToolkit().menuShortcutKeyMask)
        saveAsItem = JMenuItem("Save As")
        file.add(saveAsItem)
        saveAsItem.addActionListener { SaveAs() }
        undoItem = JMenuItem("Undo")
        edit.add(undoItem) //Edit Items
        undoItem.addActionListener { if (undo.canUndo()) undo.undo() else Toolkit.getDefaultToolkit().beep() }
        undoItem.accelerator = KeyStroke.getKeyStroke('Z'.toInt(), Toolkit.getDefaultToolkit().menuShortcutKeyMask)
        Redo = JMenuItem("Redo")
        edit.add(Redo)
        Redo.addActionListener { if (undo.canRedo()) undo.redo() else Toolkit.getDefaultToolkit().beep() }
        Redo.accelerator = KeyStroke.getKeyStroke('Y'.toInt(), Toolkit.getDefaultToolkit().menuShortcutKeyMask)
        onTop = JCheckBoxMenuItem("Always On Top")
        view.add(onTop) //View Items
        onTop.addActionListener { e: ActionEvent? -> toggleOnTop() }
        run.add(JMenuItem("Run").apply {
            addActionListener {


                val dialog = JDialog(this@Scratch,"Run").apply {
                    setSize(300,100)
                    isVisible = true
                    setLocationRelativeTo(null)
                }

                val text = JTextArea()
                text.isEditable = false
                text.font = FONT
                text.isVisible = true

                dialog.add(text)

                val environment = Environment() {
                    text.text = text.text + it + "\n"
                }
                try {
                    val result = Parser(TokenReader(CharReader((this@Scratch.text.text ?: "").toCharArray()))).parse()
                    environment.evaluate(result)
                } catch (e: Exception) {
                    text.text = text.text + e.localizedMessage
                    e.printStackTrace()
                }
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
        noWrap.add(text, BorderLayout.CENTER)
        noWrap.add(lineNumber, BorderLayout.WEST)
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
                var size = getWidth() - e.x
                if (size < 105) {
                    size = 105
                    return
                }
                setSize(size, getHeight())
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
                var size = getWidth() + e.x
                if (size < 105) size = 105
                setSize(size, getHeight())
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
                var size = getHeight() + e.y
                if (size < 75) size = 75
                setSize(getWidth(), size)
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
                var sizex = getWidth() + e.x
                var sizey = getHeight() + e.y
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
                var sizex = getWidth() - e.x
                var sizey = getHeight() + e.y
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
}
