package me.averi.wynntils.features

import com.wynntils.core.consumers.features.ProfileDefault
import com.wynntils.core.consumers.overlays.annotations.RegisterOverlay
import me.averi.wynntils.SHAMAN_TOTEM_CUSTOM_MODEL_DATA
import me.averi.wynntils.SKYSEER_TOTEM_CUSTOM_MODEL_DATA
import me.averi.wynntils.dx.Feature
import me.averi.wynntils.dx.Setting
import me.averi.wynntils.events.EntityRenderEvent
import me.averi.wynntils.events.EventBus.subscribe
import me.averi.wynntils.events.ItemModelResolveEvent
import me.averi.wynntils.overlays.TotemTimer
import me.averi.wynntils.utils.customModel
import net.minecraft.world.entity.Display.ItemDisplay
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

object ShamanTotemUtils : Feature(ProfileDefault.ENABLED) {
  private val totemScale by Setting(.4f)
  private val totemModel by Setting(TotemModelType.DEFAULT)

  @RegisterOverlay
  @Suppress("unused")
  private val totemTimerOverlay = TotemTimer

  private val totemItem = ItemStack(Items.OAK_BOAT)

  init {
    subscribe(::onRenderEntity)
    subscribe(::onItemModelResolve)
  }

  private fun onRenderEntity(e: EntityRenderEvent) {
    if (!isEnabled || e.entity !is ItemDisplay) return
    if (isTotem(e.entity.itemStack)) {
      e.matrices.translate(0.0, -(1f - totemScale).toDouble(), 0.0)
      e.matrices.scale(totemScale, totemScale, totemScale)
    }
  }

  fun onItemModelResolve(e: ItemModelResolveEvent) {
    if (!isEnabled || !isTotem(e.itemStack)) return
    totemItem.customModel = totemModel.modelId ?: return
    e.returnValue = totemItem
  }

  private fun isTotem(item: ItemStack) =
    item.`is`(Items.OAK_BOAT) && (item.customModel == SHAMAN_TOTEM_CUSTOM_MODEL_DATA || item.customModel == SKYSEER_TOTEM_CUSTOM_MODEL_DATA)

  private enum class TotemModelType(val modelId: Float?) {
    DEFAULT(null),

    @Suppress("unused")
    SHAMAN(SHAMAN_TOTEM_CUSTOM_MODEL_DATA),

    @Suppress("unused")
    SKYSEER(SKYSEER_TOTEM_CUSTOM_MODEL_DATA)
  }
}
