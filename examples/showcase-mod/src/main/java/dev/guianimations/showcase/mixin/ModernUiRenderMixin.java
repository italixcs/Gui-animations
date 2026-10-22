package dev.guianimations.showcase.mixin;

import dev.guianimations.showcase.render.ExternalRenderState;
import icyllis.modernui.mc.UIManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Exact ModernUI 3.13.0.5 hook, before its compositor makes further Minecraft draw calls. */
@Mixin(value = UIManager.class, remap = false)
public abstract class ModernUiRenderMixin {
    @Inject(method = "render(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V",
            at = @At(value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/opengl/GlStateManager;_disableCull()V",
                    shift = At.Shift.AFTER, remap = false), require = 1, remap = false)
    private void guianimations$externalGlFinished(CallbackInfo callback) {
        ExternalRenderState.changed();
    }
}
