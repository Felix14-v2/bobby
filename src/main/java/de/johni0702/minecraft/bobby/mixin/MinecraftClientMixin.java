package de.johni0702.minecraft.bobby.mixin;

import de.johni0702.minecraft.bobby.FakeChunkManager;
import de.johni0702.minecraft.bobby.FakeChunkStorage;
import de.johni0702.minecraft.bobby.ext.ClientChunkManagerExt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.Option;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.Util;
import net.minecraft.util.profiler.Profiler;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
    @Shadow private Profiler profiler;

    @Shadow @Final public GameOptions options;

    @Shadow @Nullable public ClientWorld world;

    @Inject(method = "render", at = @At(value = "CONSTANT", args = "stringValue=tick"))
    private void bobbyUpdate(CallbackInfo ci) {
        if (world == null) {
            return;
        }
        FakeChunkManager bobbyChunkManager = ((ClientChunkManagerExt) world.getChunkManager()).bobby_getFakeChunkManager();
        if (bobbyChunkManager == null) {
            return;
        }

        profiler.push("bobbyUpdate");

        int maxFps = options.maxFps;
        long frameTime = 1_000_000_000 / (maxFps == Option.FRAMERATE_LIMIT.getMax() ? 120 : maxFps);
        // Arbitrarily choosing 1/4 of frame time as our max budget, that way we're hopefully not noticeable.
        long frameBudget = frameTime / 4;
        long timeLimit = Util.getMeasuringTimeNano() + frameBudget;
        bobbyChunkManager.update(false, () -> Util.getMeasuringTimeNano() < timeLimit);

        profiler.pop();
    }

    @Inject(method = "disconnect(Lnet/minecraft/client/gui/screen/Screen;)V", at = @At("RETURN"))
    private void bobbyClose(CallbackInfo ci) {
        FakeChunkStorage.closeAll();
    }
}
