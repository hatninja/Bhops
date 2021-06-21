package smol.bhops.mixin;

import me.sargunvohra.mcmods.autoconfig1u.AutoConfig;
import net.minecraft.entity.*;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import smol.bhops.config.BhopsConfig;

import java.util.Map;

//Reference Functions from Source:
//FullWalkMove( )
//PlayerMove()

//TODO: Custom Jump sounds
//TODO: Water Controls
//TODO: Avoid rewrites of gravity & ladder code.

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {
    private BhopsConfig config = AutoConfig.getConfigHolder(BhopsConfig.class).getConfig();

    @Shadow private float movementSpeed;
    @Shadow private int jumpingCooldown;

    @Shadow protected abstract float getJumpVelocity();
    @Shadow public abstract boolean hasStatusEffect(StatusEffect effect);
    @Shadow public abstract StatusEffectInstance getStatusEffect(StatusEffect effect);
    @Shadow protected abstract float getMovementSpeed(float slipperiness);

    private boolean wasOnGround;

    public LivingEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    //Nullify updateVelocity in method_26318 so we can override the movement code.
    @ModifyArg(method="method_26318",at=@At(value="INVOKE",target="Lnet/minecraft/entity/LivingEntity;getMovementSpeed(F)F"))
    private float getMovementSpeedOverride(float slipperiness) {
        if (!config.enableBhops) {
            return getMovementSpeed(slipperiness);
        }
        return 1000.0F;
    }

    //Modify slipperiness value

    //method_26318 is only called in default walking state.
    @Inject(method = "method_26318", at = @At("HEAD"))
    public void method_26318(Vec3d movementInput, float slipperiness, CallbackInfoReturnable<Vec3d> cir) {
        //Toggle Bhops
        if (!config.enableBhops) { return; }
        //Enable for Players only
        if (config.exclusiveToPlayers && this.getType() != EntityType.PLAYER) { return; }
        //Disable on creative flying.
        if (this.getType() == EntityType.PLAYER && isFlying((PlayerEntity) (Object) this)) { return; }

        //Lower jumping cooldown.
        if (this.jumpingCooldown > 1) {
            this.jumpingCooldown = 1;
        }

        //Get friction value from slipperiness.
        float friction = 1-(slipperiness*slipperiness);

        //
        //Apply Friction
        //
        boolean fullGrounded = this.wasOnGround && this.onGround; //Allows for no friction 1-frame upon landing.
        if (fullGrounded) {
            Vec3d velFin = this.getVelocity();
            Vec3d horFin = new Vec3d(velFin.x,0.0F,velFin.z);
            double speed = horFin.lengthSquared();
            if (speed > 0.001D) {
                double drop = 0.0D;

                drop += (speed * config.sv_friction * friction);

                double newspeed = Math.max(speed - drop, 0.0D);
                newspeed /= speed;
                this.setVelocity(horFin.x * newspeed, velFin.y, horFin.z * newspeed);
            }
        }
        this.wasOnGround = this.onGround;

        //
        // Accelerate
        //
        float sI = (float) movementInput.x;
        float fI = (float) movementInput.z;
        if (sI != 0.0F || fI != 0.0F) {
            Vec3d moveDir = movementInputDir(sI, fI, yaw * 0.017453292F);
            Vec3d accelVec = this.getVelocity();

            double projVel = new Vec3d(accelVec.x, 0.0F, accelVec.z).dotProduct(moveDir);
            double accelVel = this.onGround ? config.sv_accelerate : config.sv_airaccelerate;
            float maxVel = this.onGround ? this.movementSpeed * config.maxSpeedMul : config.sv_maxairspeed;

            if (projVel + accelVel > maxVel) {
                accelVel = maxVel - projVel;
            }
            Vec3d accelDir = moveDir.multiply(Math.max(accelVel, 0.0F));

            this.setVelocity(accelVec.add(accelDir));

            //Too much effort to implement a speedcap.
            //if (accelVec.lengthSquared() > (config.sv_maxvelocity * config.sv_maxvelocity)) {
            //    this.setVelocity(this.getVelocity().normalize().multiply(config.sv_maxvelocity));
            //}
        }

        //this.setVelocity(this.applyClimbingSpeed(this.getVelocity()));
        //this.move(MovementType.SELF, this.getVelocity());
    }

    @Inject(method = "jump", at = @At("HEAD"), cancellable = true)
    void jump(CallbackInfo ci) {
        if (!config.enableBhops) {return;}

        Vec3d vecFin = this.getVelocity();
        double yVel = this.getJumpVelocity();
        if (this.hasStatusEffect(StatusEffects.JUMP_BOOST)) {
            yVel += 0.1F * (this.getStatusEffect(StatusEffects.JUMP_BOOST).getAmplifier() + 1);
        }

        this.setVelocity(vecFin.x, yVel, vecFin.z);
        this.velocityDirty = true;

        ci.cancel();
    }

    private static Vec3d movementInputDir(float sideInput,float forwardInput, float yawRad) {
        double sin = MathHelper.sin(yawRad);
        double cos = MathHelper.cos(yawRad);
        return new Vec3d(sideInput * cos - forwardInput * sin, 0.0F, sideInput * sin + forwardInput * cos);
    }

    private static boolean isFlying(PlayerEntity player) {
        return player != null && player.abilities.flying;
    }
}
