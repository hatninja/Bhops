package smol.bhops.mixin;

import me.sargunvohra.mcmods.autoconfig1u.AutoConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import smol.bhops.config.BhopsConfig;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {
    private final BhopsConfig config = AutoConfig.getConfigHolder(BhopsConfig.class).getConfig();

    @Inject(method = "render", at = @At("TAIL"))
    public void render(DrawContext context, float tickDelta, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        PlayerEntity player = client.player;

        if (config.showSpeed && player != null) {
            Vec3d playerVel = player.getVelocity();
            double rawSpeed = new Vec3d(playerVel.getX(), 0.0D, playerVel.getZ()).length();
            String speedString = String.format("%.2f", rawSpeed);

            int x = (client.getWindow().getScaledWidth() / 2) - client.textRenderer.getWidth(speedString) / 2;
            int y = client.getWindow().getScaledHeight() / 2 + 32;

            int colorRatio = 255 - (int) (Math.min(rawSpeed / config.sv_maxvelocity, 1.0f) * 255.0f);

            context.drawTextWithShadow(client.textRenderer, speedString, x, y, (colorRatio << 16) | (0xFF << 8) | colorRatio);
        }
    }
}
