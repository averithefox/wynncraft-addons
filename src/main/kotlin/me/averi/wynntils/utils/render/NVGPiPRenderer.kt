package me.averi.wynntils.utils.render

import com.mojang.blaze3d.opengl.GlConst
import com.mojang.blaze3d.opengl.GlDevice
import com.mojang.blaze3d.opengl.GlStateManager
import com.mojang.blaze3d.opengl.GlTexture
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import me.averi.wynntils.utils.pose
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.navigation.ScreenRectangle
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer
import net.minecraft.client.gui.render.state.pip.PictureInPictureRenderState
import net.minecraft.client.renderer.MultiBufferSource
import org.joml.Matrix3x2f
import org.lwjgl.opengl.GL33C

class NVGPiPRenderer(vertexConsumers: MultiBufferSource.BufferSource) :
  PictureInPictureRenderer<NVGPiPRenderer.NVGRenderState>(vertexConsumers) {
  override fun renderToTexture(state: NVGRenderState, poseStack: PoseStack) {
    val colorTex = RenderSystem.outputColorTextureOverride ?: return
    val bufferManager = (RenderSystem.getDevice() as? GlDevice)?.directStateAccess() ?: return
    val glDepthTex = (RenderSystem.outputDepthTextureOverride?.texture() as? GlTexture) ?: return

    val (width, height) = colorTex.let { it.getWidth(0) to it.getHeight(0) }
    (colorTex.texture() as? GlTexture)?.getFbo(bufferManager, glDepthTex)?.apply {
      GlStateManager._glBindFramebuffer(GlConst.GL_FRAMEBUFFER, this)
      GlStateManager._viewport(0, 0, width, height)
    }

    GL33C.glBindSampler(0, 0)
    vg.beginFrame(width.toFloat(), height.toFloat())
    state.renderContent()
    vg.endFrame()

    GlStateManager._disableDepthTest()
    GlStateManager._disableCull()
    GlStateManager._enableBlend()
    GlStateManager._blendFuncSeparate(770, 771, 1, 0)
  }

  override fun getTranslateY(height: Int, scaleFactor: Int) = height / 2f
  override fun getRenderStateClass() = NVGRenderState::class.java
  override fun getTextureLabel() = "nvg_renderer"

  companion object {
    fun draw(ctx: GuiGraphics, x: Int, y: Int, width: Int, height: Int, renderContent: () -> Unit) {
      val scissor = ctx.scissorStack.peek()
      val pose = Matrix3x2f(ctx.pose)
      val bounds = createBounds(x, y, x + width, y + height, pose, scissor)

      val state = NVGRenderState(x, y, width, height, pose, scissor, bounds, renderContent)
      ctx.guiRenderState.submitPicturesInPictureState(state)
    }

    private fun createBounds(
      x0: Int, y0: Int, x1: Int, y1: Int, pose: Matrix3x2f, scissorArea: ScreenRectangle?
    ): ScreenRectangle? {
      val screenRect = ScreenRectangle(x0, y0, x1 - x0, y1 - y0).transformMaxBounds(pose)
      return if (scissorArea != null) scissorArea.intersection(screenRect) else screenRect
    }
  }

  data class NVGRenderState(
    private val x: Int,
    private val y: Int,
    private val width: Int,
    private val height: Int,
    private val pose: Matrix3x2f,
    private val scissor: ScreenRectangle?,
    private val bounds: ScreenRectangle?,
    val renderContent: () -> Unit
  ) : PictureInPictureRenderState {
    override fun bounds() = bounds
    override fun x0() = x
    override fun x1() = x + width
    override fun y0() = y
    override fun y1() = y + height
    override fun scale() = 1f
    override fun scissorArea() = scissor
  }
}
