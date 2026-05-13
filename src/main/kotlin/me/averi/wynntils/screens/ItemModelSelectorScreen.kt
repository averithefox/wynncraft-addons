package me.averi.wynntils.screens

import com.mojang.blaze3d.platform.cursor.CursorTypes
import com.wynntils.core.consumers.screens.WynntilsScreen
import com.wynntils.utils.MathUtils
import com.wynntils.utils.mc.McUtils
import com.wynntils.utils.render.RenderUtils
import com.wynntils.utils.render.Texture
import me.averi.wynntils.dx.ItemModelSetting
import me.averi.wynntils.utils.*
import me.averi.wynntils.utils.render.NVGPiPRenderer
import me.averi.wynntils.utils.render.vg
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import java.awt.Color
import kotlin.math.*
import kotlin.properties.Delegates

private const val SCROLL_FACTOR = 10f
private const val CONTENT_AREA_WIDTH = 322
private const val CONTENT_AREA_HEIGHT = 134
private const val SCROLL_AREA_HEIGHT = 121
private const val ITEM_MARGIN = 4f

private const val HOVER_SCALE = 2f
private const val HOVER_ANIM_SECONDS = 0.12f

class ItemModelSelectorScreen(val previousScreen: Screen, val setting: ItemModelSetting) :
  WynntilsScreen(Component.literal("Item Model Selector")) {
  private var offsetX by Delegates.notNull<Float>()
  private var offsetY by Delegates.notNull<Float>()

  private val contentAreaX
    get() = offsetX + 9f
  private val contentAreaY
    get() = offsetY + 8f

  private var isDraggingScroll = false
  private var scrollY by Delegates.notNull<Float>()
  private var scrollOffset = 0f

  private var hoverAnim = FloatArray(setting.modelRange.toIntRange().count())

  private val maxCols = floor((CONTENT_AREA_WIDTH + ITEM_MARGIN) / (16 + ITEM_MARGIN))
  private val horizontalPadding = (CONTENT_AREA_WIDTH - maxCols * (16f + ITEM_MARGIN) + ITEM_MARGIN) / 2f
  private val verticalPadding = max(horizontalPadding, 8f)

  private val totalContentHeight =
    ceil(setting.modelRange.toIntRange().count() / maxCols) * (16f + ITEM_MARGIN) - ITEM_MARGIN + verticalPadding * 2f
  private val maxScrollOffset = abs(CONTENT_AREA_HEIGHT - totalContentHeight)

  override fun doInit() {
    offsetX = (width - Texture.SECRETS_BACKGROUND.width()) / 2f
    offsetY = (height - Texture.SECRETS_BACKGROUND.height()) / 2f
  }

  override fun onClose() {
    McUtils.setScreen(previousScreen)
  }

  override fun doRender(ctx: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
    NVGPiPRenderer.draw(ctx, 0, 0, ctx.guiWidth(), ctx.guiHeight()) {
      vg.beginFrame(ctx.guiWidth().toFloat(), ctx.guiHeight().toFloat())
      vg.drawRoundedRect(100f, 100f, 100f, 100f, 10f, Color.RED)
      vg.endFrame()
    }

    RenderUtils.drawTexturedRect(ctx, Texture.SECRETS_BACKGROUND, offsetX, offsetY)

    RenderUtils.enableScissor(ctx, offsetX.toInt() + 9, offsetY.toInt() + 8, CONTENT_AREA_WIDTH, CONTENT_AREA_HEIGHT)

    val animStep = mc.deltaTracker.gameTimeDeltaTicks / (HOVER_ANIM_SECONDS * 20f)

    forEachItemCell(mouseX.toDouble(), mouseY.toDouble()) { index, model, centerX, centerY, isMouseOver ->
      val target = if (isMouseOver) 1f else 0f
      if (index < hoverAnim.size) hoverAnim[index] = moveToward(hoverAnim[index], target, animStep)
      val linear = if (index < hoverAnim.size) hoverAnim[index] else 0f
      val eased = easeOutCirc(linear)
      val scale = 1f + (HOVER_SCALE - 1f) * eased

      if (setting.modelValue == model) ctx.drawCircle(centerX, centerY, 9f, 0x40_FFFFFF)
      ctx.renderItem(itemStackWithModel(model), centerX, centerY, scale)
    }

    RenderUtils.disableScissor(ctx)

    renderScroll(ctx)

    if (isDraggingScroll) {
      ctx.requestCursor(CursorTypes.RESIZE_NS)
    }

    InventoryScreen.renderEntityInInventoryFollowsMouse(
      ctx,
      contentAreaX.toInt() - 90,
      contentAreaY.toInt(),
      contentAreaX.toInt(),
      contentAreaY.toInt() + 100,
      30,
      0.0625f,
      mouseX.toFloat(),
      mouseY.toFloat(),
      mc.player!!
    )
  }

  override fun mouseScrolled(mouseX: Double, mouseY: Double, deltaX: Double, deltaY: Double): Boolean {
    scrollOffset = min(max(scrollOffset - deltaY.toFloat() * SCROLL_FACTOR, 0f), maxScrollOffset)
    return true
  }

  override fun doMouseClicked(event: MouseButtonEvent, isDoubleClick: Boolean): Boolean {
    if (event.isRight) return false
    val isOverScroll = isInside(
      offsetX + 336,
      scrollY,
      offsetX + 336 + Texture.SCROLL_BUTTON.width(),
      scrollY + Texture.SCROLL_BUTTON.height(),
      event.x,
      event.y
    )
    if (!isDraggingScroll && isOverScroll) {
      isDraggingScroll = true
      return true
    }
    if (isInsideContentArea(event.x, event.y)) {
      forEachItemCell(event.x, event.y) { _, model, _, _, isMouseOver ->
        if (!isMouseOver) return@forEachItemCell
        setting.modelValue = if (setting.modelValue != model) model else null
        return true
      }
    }
    return false
  }

  override fun mouseReleased(event: MouseButtonEvent): Boolean {
    isDraggingScroll = false
    return false
  }

  override fun mouseDragged(event: MouseButtonEvent, dragX: Double, dragY: Double): Boolean {
    if (isDraggingScroll) {
      val scrollAreaStartY = offsetY + 7 + 17
      val scrollAreaHeight = SCROLL_AREA_HEIGHT - Texture.SCROLL_BUTTON.height()
      val newScrollOffset =
        MathUtils.map(event.y.toFloat(), scrollAreaStartY, scrollAreaStartY + scrollAreaHeight, 0f, maxScrollOffset)
      scrollOffset = min(max(newScrollOffset, 0f), maxScrollOffset)
      return true
    }
    return false
  }

  private inline fun forEachItemCell(
    mouseX: Double,
    mouseY: Double,
    action: (index: Int, model: Float, centerX: Float, centerY: Float, isMouseOver: Boolean) -> Unit
  ) {
    setting.modelRange.toIntRange().forEachIndexed { index, model ->
      val col = index % maxCols
      val row = floor(index / maxCols)
      val centerX = contentAreaX + horizontalPadding + 16f / 2f + col * (16f + ITEM_MARGIN)
      val centerY = contentAreaY - scrollOffset + verticalPadding + 16f / 2f + row * (16f + ITEM_MARGIN)
      val isMouseOver = isInsideContentArea(mouseX, mouseY) && isInside(
        centerX - 8, centerY - 8, centerX + 8, centerY + 8, mouseX, mouseY
      )
      action(index, model.toFloat(), centerX, centerY, isMouseOver)
    }
  }

  private fun renderScroll(ctx: GuiGraphics) {
    scrollY = offsetY + 7 + MathUtils.map(scrollOffset, 0f, maxScrollOffset, 0f, 135f - Texture.SCROLL_BUTTON.height())
    RenderUtils.drawTexturedRect(ctx, Texture.SCROLL_BUTTON, offsetX + 336, scrollY)
  }

  private fun isInsideContentArea(x: Number, y: Number) =
    isInside(contentAreaX, contentAreaY, contentAreaX + CONTENT_AREA_WIDTH, contentAreaY + CONTENT_AREA_HEIGHT, x, y)
}
