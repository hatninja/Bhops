package smol.bhops.mixin;

import me.sargunvohra.mcmods.autoconfig1u.AutoConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import smol.bhops.config.BhopsConfig;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {
    private BhopsConfig config = AutoConfig.getConfigHolder(BhopsConfig.class).getConfig();

    @Inject(method = "render", at = @At("TAIL"))
    public void render(MatrixStack matrixStack, float tickDelta, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();

        if(config.showSpeed) {
            String speedString = "";

            PlayerEntity player = MinecraftClient.getInstance().player;
            Vec3d playerVel = player.getVelocity();
            double rawSpeed = new Vec3d(playerVel.getX(), 0.0F, playerVel.getZ()).length();
            speedString += Math.floor((rawSpeed)*100)/100.0D;

            float x = (client.getWindow().getScaledWidth() / 2.0f) - client.textRenderer.getWidth(speedString) / 2.0f;
            float y = client.getWindow().getScaledHeight() / 2.0f + 32;

            int colorRatio = 255 - (int) (Math.min(rawSpeed / config.sv_maxvelocity, 1.0f) * 255.0f);

            client.textRenderer.drawWithShadow(matrixStack, speedString, x, y, (colorRatio << 16) | (0xFF << 8)  | (colorRatio << 0));
        }
    }
}