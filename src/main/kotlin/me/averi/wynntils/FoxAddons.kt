package me.averi.wynntils

import me.averi.wynntils.utils.render.NVGPiPRenderer
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.rendering.v1.SpecialGuiElementRegistry

object FoxAddons : ClientModInitializer {
  override fun onInitializeClient() {
    SpecialGuiElementRegistry.register { ctx -> NVGPiPRenderer(ctx.vertexConsumers()) }
  }
}
