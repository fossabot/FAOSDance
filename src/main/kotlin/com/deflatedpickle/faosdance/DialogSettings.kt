package com.deflatedpickle.faosdance

import java.awt.*
import java.awt.datatransfer.DataFlavor
import java.awt.dnd.DnDConstants
import java.awt.dnd.DropTarget
import java.awt.dnd.DropTargetDropEvent
import java.io.File
import javax.swing.*
import javax.swing.filechooser.FileNameExtensionFilter
import kotlin.math.roundToInt

class DialogSettings(owner: Frame) : JDialog(owner, "FAOSDance Settings", true) {
    private val gridBagLayout = GridBagLayout()

    init {
        this.isResizable = false

        createWidgets()
        this.size = Dimension(440, 460)

        this.layout = gridBagLayout
    }

    fun createWidgets() {
        contentPane.removeAll()

        if (GlobalValues.sheet != null) {
            this.add(JLabel("Action:").also {
                gridBagLayout.setConstraints(it, GridBagConstraints().apply {
                    anchor = GridBagConstraints.EAST
                })
            })

            this.add(JComboBox<String>(GlobalValues.sheet!!.spriteMap.keys.toTypedArray()).apply {
                selectedIndex = GlobalValues.sheet!!.spriteMap.keys.indexOf(selectedItem as String)
                addActionListener { GlobalValues.currentAction = (it.source as JComboBox<*>).selectedItem as String }

                gridBagLayout.setConstraints(this, GridBagConstraints().apply {
                    fill = GridBagConstraints.HORIZONTAL
                    weightx = 1.0
                })
            })
        }

        this.add(JButton("Open").apply {
            dropTarget = object : DropTarget() {
                override fun drop(dtde: DropTargetDropEvent) {
                    // super.drop(dtde)

                    dtde.acceptDrop(DnDConstants.ACTION_COPY)
                    val droppedFiles = dtde.transferable.getTransferData(DataFlavor.javaFileListFlavor) as List<File>

                    if (droppedFiles.size == 1) {
                        val tempSheet = SpriteSheet(droppedFiles[0].absolutePath.substringBeforeLast("."))

                        if (tempSheet.loadedImage && tempSheet.loadedText) {
                            GlobalValues.configureSpriteSheet(tempSheet)
                            GlobalValues.currentPath = droppedFiles[0].parentFile.absolutePath

                            createWidgets()
                        }
                    }
                }
            }

            addActionListener {
                val fileChooser = JFileChooser(GlobalValues.currentPath)
                fileChooser.addChoosableFileFilter(
                    FileNameExtensionFilter(
                        "PNG (*.png)",
                        "png"
                    ).also { fileChooser.fileFilter = it })
                fileChooser.addChoosableFileFilter(FileNameExtensionFilter("JPEG (*.jpg; *.jpeg)", "jpg", "jpeg"))
                val returnValue = fileChooser.showOpenDialog(owner)

                if (returnValue == JFileChooser.APPROVE_OPTION) {
                    val tempSheet = SpriteSheet(fileChooser.selectedFile.absolutePath.substringBeforeLast("."))

                    if (tempSheet.loadedImage && tempSheet.loadedText) {
                        GlobalValues.configureSpriteSheet(tempSheet)
                        GlobalValues.currentPath = fileChooser.selectedFile.parentFile.absolutePath

                        createWidgets()
                    }
                }
            }

            gridBagLayout.setConstraints(this, GridBagConstraints().apply {
                if (GlobalValues.sheet == null) {
                    fill = GridBagConstraints.BOTH
                    weightx = 1.0
                    weighty = 1.0
                }

                gridwidth = GridBagConstraints.REMAINDER
            })
        })

        if (GlobalValues.sheet != null) {
            addComponentSliderSpinner<Int>(
                this,
                gridBagLayout,
                JLabel("Frames Per Second:"),
                GlobalValues.fps,
                144,
                1
            ).third.addChangeListener {
                GlobalValues.fps = when {
                    (it.source as JSpinner).model.value is Int -> (it.source as JSpinner).model.value as Int
                    (it.source as JSpinner).model.value is Double -> ((it.source as JSpinner).model.value as Double).roundToInt()
                    else -> 0
                }
                GlobalValues.timer!!.delay = 1000 / GlobalValues.fps
            }

            this.add(JCheckBox("Visible").apply {
                isSelected = GlobalValues.isVisible

                addActionListener { GlobalValues.isVisible = this.isSelected }
            })
            this.add(JCheckBox("Solid").apply {
                isSelected = GlobalValues.isSolid

                addActionListener { GlobalValues.isSolid = this.isSelected }
            })
            this.add(JCheckBox("Always On Top").apply {
                isSelected = GlobalValues.isTopLevel

                addActionListener {
                    GlobalValues.isTopLevel = this.isSelected
                    GlobalValues.frame!!.isAlwaysOnTop = GlobalValues.isTopLevel
                }

                gridBagLayout.setConstraints(
                    this,
                    GridBagConstraints().apply { gridwidth = GridBagConstraints.REMAINDER })
            })

            this.add(JPanel().apply {
                this.border = BorderFactory.createTitledBorder("Animation")
                this.layout = GridBagLayout()

                val rewind = JCheckBox("Rewind").apply {
                    addActionListener {
                        GlobalValues.rewind = this.isSelected
                    }
                }
                val play = JCheckBox("Play").apply {
                    isSelected = true

                    addActionListener {
                        GlobalValues.play = this.isSelected
                        rewind.isEnabled = isSelected
                    }
                }
                GlobalValues.animationControls = addComponentSliderSpinner<Int>(
                    this,
                    this.layout as GridBagLayout,
                    JPanel().apply {
                        this.add(play)
                        this.add(rewind)
                    },
                    GlobalValues.animFrame,
                    7,
                    0
                ).apply {
                    third.addChangeListener {
                        GlobalValues.animFrame = when {
                            (it.source as JSpinner).model.value is Int -> (it.source as JSpinner).model.value as Int
                            (it.source as JSpinner).model.value is Double -> ((it.source as JSpinner).model.value as Double).roundToInt()
                            else -> 0
                        }

                        GlobalValues.frame!!.revalidate()
                        GlobalValues.frame!!.repaint()
                    }
                }

                gridBagLayout.setConstraints(this, GridBagConstraints().apply {
                    fill = GridBagConstraints.BOTH
                    weightx = 1.0
                    gridwidth = GridBagConstraints.REMAINDER
                })
            })

            this.add(JPanel().apply {
                this.border = BorderFactory.createTitledBorder("Location")
                this.layout = GridBagLayout()

                val effectiveSize = GlobalValues.getEffectiveScreenSize(this@DialogSettings)

                val gridLayout = this.layout as GridBagLayout

                var comboBox: JComboBox<String>? = null

                val xComponents = addComponentSliderSpinner<Double>(
                    this,
                    this.layout as GridBagLayout,
                    JLabel("X:"),
                    GlobalValues.xPosition,
                    effectiveSize.width,
                    0.0
                ).apply {
                    third.addChangeListener {
                        comboBox!!.selectedIndex = HookPoint.values().size - 1
                        when {
                            (it.source as JSpinner).model.value is Int -> GlobalValues.frame!!.setLocation(
                                (it.source as JSpinner).model.value as Int,
                                GlobalValues.frame!!.y
                            )
                            (it.source as JSpinner).model.value is Double -> GlobalValues.frame!!.setLocation(
                                ((it.source as JSpinner).model.value as Double).roundToInt(),
                                GlobalValues.frame!!.y
                            )
                        }
                    }
                }
                val xEntry = xComponents.third

                val yComponents = addComponentSliderSpinner<Double>(
                    this,
                    this.layout as GridBagLayout,
                    JLabel("Y:"),
                    GlobalValues.yPosition,
                    effectiveSize.height,
                    0.0
                ).apply {
                    third.addChangeListener {
                        comboBox!!.selectedIndex = HookPoint.values().size - 1
                        when {
                            (it.source as JSpinner).model.value is Int -> GlobalValues.frame!!.setLocation(
                                GlobalValues.frame!!.x,
                                (it.source as JSpinner).model.value as Int
                            )
                            (it.source as JSpinner).model.value is Double -> GlobalValues.frame!!.setLocation(
                                GlobalValues.frame!!.x,
                                ((it.source as JSpinner).model.value as Double).roundToInt()
                            )
                        }
                    }
                }
                val yEntry = yComponents.third

                this.add(JComboBox<String>(HookPoint.values().map { enumItem ->
                    enumItem.name
                        .replace("_", " ")
                        .toLowerCase().split(" ")
                        .joinToString(" ") { subStr -> subStr.capitalize() }
                }.toTypedArray()).apply {
                    comboBox = this
                    selectedIndex = HookPoint.values().size - 1
                    addActionListener {
                        when (HookPoint.valueOf(
                            ((it.source as JComboBox<*>).selectedItem as String).toUpperCase().replace(
                                " ",
                                "_"
                            )
                        )) {
                            HookPoint.TOP_LEFT -> {
                                val selected = this.selectedIndex
                                xEntry.value = 0.0
                                yEntry.value = 0.0
                                this.selectedIndex = selected
                            }
                            HookPoint.TOP_CENTRE -> {
                                val selected = this.selectedIndex
                                xEntry.value = effectiveSize.width / 2.0
                                yEntry.value = 0.0
                                this.selectedIndex = selected
                            }
                            HookPoint.TOP_RIGHT -> {
                                val selected = this.selectedIndex
                                xEntry.value = effectiveSize.width.toDouble()
                                yEntry.value = 0.0
                                this.selectedIndex = selected
                            }
                            HookPoint.MIDDLE_LEFT -> {
                                val selected = this.selectedIndex
                                xEntry.value = 0.0
                                yEntry.value = effectiveSize.height / 2.0
                                this.selectedIndex = selected
                            }
                            HookPoint.MIDDLE_CENTRE -> {
                                val selected = this.selectedIndex
                                xEntry.value = effectiveSize.width / 2.0
                                yEntry.value = effectiveSize.height / 2.0
                                this.selectedIndex = selected
                            }
                            HookPoint.MIDDLE_RIGHT -> {
                                val selected = this.selectedIndex
                                xEntry.value = effectiveSize.width.toDouble()
                                yEntry.value = effectiveSize.height / 2.0
                                this.selectedIndex = selected
                            }
                            HookPoint.BOTTOM_LEFT -> {
                                val selected = this.selectedIndex
                                xEntry.value = 0.0
                                yEntry.value = effectiveSize.height.toDouble()
                                this.selectedIndex = selected
                            }
                            HookPoint.BOTTOM_CENTRE -> {
                                val selected = this.selectedIndex
                                xEntry.value = effectiveSize.width / 2.0
                                yEntry.value = effectiveSize.height.toDouble()
                                this.selectedIndex = selected
                            }
                            HookPoint.BOTTOM_RIGHT -> {
                                val selected = this.selectedIndex
                                xEntry.value = effectiveSize.width.toDouble()
                                yEntry.value = effectiveSize.height.toDouble()
                                this.selectedIndex = selected
                            }
                            HookPoint.CUSTOM -> {
                            }
                        }
                    }

                    gridLayout.setConstraints(this, GridBagConstraints().apply {
                        fill = GridBagConstraints.HORIZONTAL
                        weightx = 1.0
                        gridwidth = GridBagConstraints.REMAINDER
                    })
                })

                gridBagLayout.setConstraints(this, GridBagConstraints().apply {
                    fill = GridBagConstraints.BOTH
                    weightx = 1.0
                    gridwidth = GridBagConstraints.REMAINDER
                })
            })

            this.add(JPanel().apply {
                this.border = BorderFactory.createTitledBorder("Size")
                this.layout = GridBagLayout()

                addComponentSliderSpinner<Double>(
                    this,
                    this.layout as GridBagLayout,
                    JLabel("Width:"),
                    GlobalValues.xMultiplier,
                    GlobalValues.maxSize,
                    0.1
                ).third.addChangeListener {
                    GlobalValues.xMultiplier = (it.source as JSpinner).model.value as Double
                    GlobalValues.resize(Direction.HORIZONTAL)
                }

                addComponentSliderSpinner<Double>(
                    this,
                    this.layout as GridBagLayout,
                    JLabel("Height:"),
                    GlobalValues.yMultiplier,
                    GlobalValues.maxSize,
                    0.1
                ).third.addChangeListener {
                    GlobalValues.yMultiplier = (it.source as JSpinner).model.value as Double
                    GlobalValues.resize(Direction.VERTICAL)
                }

                gridBagLayout.setConstraints(this, GridBagConstraints().apply {
                    fill = GridBagConstraints.BOTH
                    weightx = 1.0
                    gridwidth = GridBagConstraints.REMAINDER
                })
            })

            this.add(JPanel().apply {
                // TODO: Add a visibility setting for the reflection
                this.border = BorderFactory.createTitledBorder("Reflection")
                this.layout = GridBagLayout()

                addComponentSliderSpinner<Double>(
                    this,
                    this.layout as GridBagLayout,
                    JLabel("Padding:"),
                    GlobalValues.reflectionPadding,
                    100.0,
                    -100.0
                ).third.addChangeListener {
                    GlobalValues.reflectionPadding = (it.source as JSpinner).model.value as Double
                }

                this.add(JPanel().apply {
                    this.border = BorderFactory.createTitledBorder("Fade")
                    this.layout = GridBagLayout()

                    addComponentSliderSpinner<Double>(
                        this,
                        this.layout as GridBagLayout,
                        JLabel("Height:"),
                        GlobalValues.fadeHeight,
                        0.9,
                        0.1
                    ).third.addChangeListener {
                        GlobalValues.fadeHeight = ((it.source as JSpinner).model.value as Double).toFloat()
                    }

                    addComponentSliderSpinner<Double>(
                        this,
                        this.layout as GridBagLayout,
                        JLabel("Opacity:"),
                        GlobalValues.fadeOpacity,
                        0.9,
                        0.1
                    ).third.addChangeListener {
                        GlobalValues.fadeOpacity = ((it.source as JSpinner).model.value as Double).toFloat()
                    }
                }.also {
                    (this.layout as GridBagLayout).setConstraints(it, GridBagConstraints().apply {
                        fill = GridBagConstraints.BOTH
                        weightx = 1.0
                        gridwidth = GridBagConstraints.REMAINDER
                    })
                })

                gridBagLayout.setConstraints(this, GridBagConstraints().apply {
                    fill = GridBagConstraints.BOTH
                    weightx = 1.0
                    gridwidth = GridBagConstraints.REMAINDER
                })
            })
        }

        revalidate()
        repaint()
    }

    private inline fun <reified T : Number> addComponentSliderSpinner(
        parent: Container,
        gridBagLayout: GridBagLayout,
        component: JComponent,
        defaultValue: Number,
        maxNumber: Number,
        minNumber: Number
    ): Triple<JComponent, JSlider, JSpinner> {
        val slider = when (T::class) {
            Int::class -> JSlider(minNumber.toInt(), maxNumber.toInt(), defaultValue.toInt())
            Double::class -> JDoubleSlider(minNumber.toDouble(), maxNumber.toDouble(), defaultValue.toDouble(), 100.0)
            else -> JSlider()
        }

        val stepSize = when (T::class) {
            Int::class -> 1 as T
            Double::class -> 0.01 as T
            else -> 0 as T
        }

        val spinner = JSpinner(
            SpinnerNumberModel(
                defaultValue.toDouble(),
                minNumber.toDouble(),
                maxNumber.toDouble(),
                stepSize
            )
        )

        slider.value = when (T::class) {
            Int::class -> defaultValue.toInt()
            Double::class -> (defaultValue.toFloat() * 100).toInt()
            else -> 0
        }
        spinner.value = defaultValue

        slider.addChangeListener {
            spinner.value = when (T::class) {
                Int::class -> slider.value
                Double::class -> slider.value.toDouble() / 100
                else -> 0
            }
        }
        spinner.addChangeListener {
            slider.value = when (T::class) {
                Int::class -> when {
                    spinner.value is Int -> spinner.value as Int
                    spinner.value is Double -> (spinner.value as Double).roundToInt()
                    else -> 0
                }
                Double::class -> (spinner.value as Double * 100).toInt()
                else -> 0
            }
        }

        parent.add(component)
        parent.add(slider)
        parent.add(spinner)

        gridBagLayout.setConstraints(component, GridBagConstraints().apply {
            anchor = GridBagConstraints.EAST
        })

        gridBagLayout.setConstraints(slider, GridBagConstraints().apply {
            fill = GridBagConstraints.HORIZONTAL
            weightx = 1.0
        })

        gridBagLayout.setConstraints(spinner, GridBagConstraints().apply {
            gridwidth = GridBagConstraints.REMAINDER
        })

        return Triple(component, slider, spinner)
    }
}