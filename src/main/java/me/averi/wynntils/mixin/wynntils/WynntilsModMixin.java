package me.averi.wynntils.mixin.wynntils;

import com.wynntils.core.WynntilsMod;
import me.averi.wynntils.events.EventBus;
import net.neoforged.bus.api.Event;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = WynntilsMod.class, remap = false)
public class WynntilsModMixin {
  @Inject(method = "postEvent", at = @At(value = "INVOKE", target = "Lnet/neoforged/bus/api/IEventBus;post(Lnet/neoforged/bus/api/Event;)Lnet/neoforged/bus/api/Event;", shift = At.Shift.AFTER))
  private static <T extends Event> void postEvent(T event, CallbackInfoReturnable<Boolean> cir) {
    EventBus.publish(event);
  }
}
