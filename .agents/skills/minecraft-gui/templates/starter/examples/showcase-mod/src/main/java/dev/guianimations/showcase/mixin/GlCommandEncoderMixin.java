package dev.guianimations.showcase.mixin;

import com.mojang.blaze3d.opengl.GlProgram;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import dev.guianimations.showcase.render.ExternalRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Rebind once after external GL work; retain Minecraft's normal shader/uniform setup. */
@Mixin(targets = "com.mojang.blaze3d.opengl.GlCommandEncoder", remap = false)
public abstract class GlCommandEncoderMixin {
    @Shadow private GlProgram lastProgram;
    @Shadow private RenderPipeline lastPipeline;
    @Unique private long guianimations$seenGeneration;

    @Inject(method = "trySetup", at = @At("HEAD"), require = 1, remap = false)
    private void guianimations$restoreCacheCoherence(CallbackInfoReturnable<Boolean> callback) {
        long generation = ExternalRenderState.generation();
        if (guianimations$seenGeneration != generation) {
            lastProgram = null;
            lastPipeline = null;
            guianimations$seenGeneration = generation;
        }
    }
}
