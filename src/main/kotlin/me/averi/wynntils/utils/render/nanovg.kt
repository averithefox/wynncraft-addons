package me.averi.wynntils.utils.render

import me.averi.wynntils.utils.mc
import org.lwjgl.nanovg.NVGColor
import org.lwjgl.nanovg.NanoVG.*
import org.lwjgl.nanovg.NanoVGGL3.*
import java.awt.Color
import kotlin.math.min

private val nvgColor = NVGColor.malloc()

@JvmInline
value class VG(private val vg: Long) {
  init {
    require(vg != -1L)
  }

  constructor(flags: Int) : this(nvgCreate(flags))
  constructor() : this(NVG_ANTIALIAS or NVG_STENCIL_STROKES)

  private fun devicePixelRatio(): Float {
    val window = mc.window
    val w = window.screenWidth.toFloat()
    val fbw = window.width.toFloat()
    return if (w == 0f) 1f else fbw / w
  }

  fun beginFrame(width: Float, height: Float) {
    val devicePixelRatio = devicePixelRatio()
    nvgBeginFrame(vg, width / devicePixelRatio, height / devicePixelRatio, devicePixelRatio)
    nvgTextAlign(vg, NVG_ALIGN_LEFT or NVG_ALIGN_TOP)
  }

  fun endFrame() {
    nvgEndFrame(vg)
  }

  fun drawRoundedRect(x: Float, y: Float, width: Float, height: Float, radius: Float, color: Color) {
    val radius = min(radius, min(width / 2f, height / 2))

    beginPath()

    // top-left corner
    moveTo(x + radius, y)

    // top side + top-right corner
    lineTo(x + width - radius, y)
    arcTo(x + width, y, x + width, y + radius, radius)

    // right side + bottom-right corner
    lineTo(x + width, y + height - radius)
    arcTo(x + width, y + height, x + width - radius, y + height, radius);

    // bottom side + bottom-left corner
    lineTo(x + radius, y + height)
    arcTo(x, y + height, x, y + height - radius, radius);

    // left side + top-left corner
    lineTo(x, y + radius)
    arcTo(x, y, x + radius, y, radius)

    closePath()
    fillColor(color)
    fill()
  }

  fun beginPath() = nvgBeginPath(vg)
  fun moveTo(x: Float, y: Float) = nvgMoveTo(vg, x, y)
  fun lineTo(x: Float, y: Float) = nvgLineTo(vg, x, y)
  fun arcTo(x1: Float, y1: Float, x2: Float, y2: Float, radius: Float) = nvgArcTo(vg, x1, y1, x2, y2, radius)
  fun closePath() = nvgClosePath(vg)


  fun fillColor(color: Color) {
    nvgRGBA(color.red.toByte(), color.green.toByte(), color.blue.toByte(), color.alpha.toByte(), nvgColor)
    nvgFillColor(vg, nvgColor)
  }

  fun fill() = nvgFill(vg)
}

val vg = VG()
