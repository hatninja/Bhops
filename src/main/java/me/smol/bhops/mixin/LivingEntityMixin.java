package me.smol.bhops.mixin;

import net.minecraft.block.Blocks;
import net.minecraft.entity.*;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

//Reference Functions from Source:
//FullWalkMove( )
//PlayerMove()

//TODO: Fix how angle is handled in acceleration code. If you press forward while your velocity is sideways, you are not capped.
//

//TODO: Speed Display
//TODO: Custom Jump sounds
//TODO: Water Controls

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {

    @Shadow private float movementSpeed;
    @Shadow public float flyingSpeed;
    @Shadow public float sidewaysSpeed;
    @Shadow public float forwardSpeed;
    @Shadow private int jumpingCooldown;
    @Shadow public float limbDistance;
    @Shadow public float lastLimbDistance;
    @Shadow public float limbAngle;
    @Shadow public boolean jumping;
    @Shadow @Final private Map<StatusEffect, StatusEffectInstance> activeStatusEffects;

    @Shadow protected abstract Vec3d applyClimbingSpeed(Vec3d velocity);
    @Shadow protected abstract float getJumpVelocity();
    @Shadow public abstract boolean hasStatusEffect(StatusEffect effect);
    @Shadow public abstract StatusEffectInstance getStatusEffect(StatusEffect effect);
    @Shadow public abstract boolean isFallFlying();

    private boolean wasOnGround;
    private final double sv_gravity = 0.08D;

    private final float sv_stopspeed = 0.0F;
    private final float sv_friction = 0.4F;
    private final float sv_accelerate = 0.1F;
    private final float sv_airaccelerate = 0.2F;
    private final float maxSpeedMul = 2.5F;

    public LivingEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    public void travel(Vec3d movementInput, CallbackInfo ci) {
        this.jumpingCooldown = 1;

        //Reverse multiplication done by the function that calls this one.
        this.sidewaysSpeed /= 0.98F;
        this.forwardSpeed /= 0.98F;
        double sI = movementInput.x / 0.98F;
        double fI = movementInput.z / 0.98F;
        double uI = movementInput.y;

        //if (!this.canMoveVoluntarily() && !this.isLogicalSideForUpdatingMovement()) { return; }

        //Cancel override if not in plain walking state.
        //TODO: Water controls-
        if (this.isTouchingWater() || this.isInLava() || this.isFallFlying()) { return; }

        //Get Slipperiness and Movement speed.
        BlockPos blockPos = this.getVelocityAffectingPos();
        float slipperiness = this.world.getBlockState(blockPos).getBlock().getSlipperiness();
        float friction = 1-slipperiness;
        //
        //Apply Friction
        //
        if (this.wasOnGround && this.onGround) {
            Vec3d velFin = this.getVelocity();
            Vec3d horFin = new Vec3d(velFin.x,0.0F,velFin.z);
            float speed = (float) horFin.length();
            if (speed > 0.001F) {
                float drop = 0.0F;

                float control = Math.max(speed, sv_stopspeed);
                drop += (control * sv_friction * friction);

                float newspeed = Math.max(speed - drop, 0.0F);
                newspeed /= speed;
                this.setVelocity(
                        horFin.x * newspeed,
                        velFin.y,
                        horFin.z * newspeed
                );
            }
        }
        this.wasOnGround = this.onGround;

        //
        // Accelerate
        //
        if (sI != 0.0F || fI != 00) {
            Vec3d forward = movementInputToVelocity(new Vec3d(0.0F, 0.0F, fI), 1.0F, this.yaw);
            Vec3d right = movementInputToVelocity(new Vec3d(sI, 0.0F, 0.0F), 1.0F, this.yaw);
            Vec3d moveDir = movementInputToVelocity(new Vec3d(sI, 0.0F, fI), 1.0F, this.yaw);

            Vec3d accelVec = this.getVelocity();

            double projVel = new Vec3d(accelVec.x, 0.0F, accelVec.z).dotProduct(moveDir);
            double accelVel = this.onGround ? sv_accelerate : sv_airaccelerate;
            float maxVel = this.movementSpeed * maxSpeedMul;

            if (projVel + accelVel > maxVel) {
                accelVel = maxVel - projVel;
            }
            Vec3d accelDir = moveDir.multiply(Math.max(accelVel, 0.001F));

            this.setVelocity(this.getVelocity().add(accelDir));
        }

        this.setVelocity(this.applyClimbingSpeed(this.getVelocity()));
        this.move(MovementType.SELF, this.getVelocity());

        //
        //Apply Gravity (If not in Water)
        //
        Vec3d preVel = this.getVelocity();
        double yVel = preVel.y;
        double gravity = sv_gravity;
        if (preVel.y <= 0.0D && this.hasStatusEffect(StatusEffects.SLOW_FALLING)) {
            gravity = 0.01D;
            this.fallDistance = 0.0F;
        }
        if (this.hasStatusEffect(StatusEffects.LEVITATION)) {
            yVel += (0.05D * (double)(this.getStatusEffect(StatusEffects.LEVITATION).getAmplifier() + 1) - preVel.y) * 0.2D;
            this.fallDistance = 0.0F;
        } else if (this.world.isClient && !this.world.isChunkLoaded(blockPos)) {
            yVel = 0.0D;
        } else if (!this.hasNoGravity()) {
            yVel -= gravity;
        }
        this.setVelocity(preVel.x,yVel,preVel.z);

        //
        //Update limbs.
        //
        this.lastLimbDistance = this.limbDistance;
        double d = this.getX() - this.prevX;
        double e = this instanceof Flutterer ? this.getY() - this.prevY : 0.0D;
        double f = this.getZ() - this.prevZ;
        float g = MathHelper.sqrt(d * d + e * e + f * f) * 4.0F;
        if (g > 1.0F) {
            g = 1.0F;
        }

        this.limbDistance += (g - this.limbDistance) * 0.4F;
        this.limbAngle += this.limbDistance;

        //Override original method.
        ci.cancel();
    }

    @Inject(method = "jump", at = @At("HEAD"), cancellable = true)
    void jump(CallbackInfo ci) {
        float f = this.getJumpVelocity();
        if (this.hasStatusEffect(StatusEffects.JUMP_BOOST)) {
            f += 0.1F * (float)(this.getStatusEffect(StatusEffects.JUMP_BOOST).getAmplifier() + 1);
        }

        Vec3d vec3d = this.getVelocity();
        this.setVelocity(vec3d.x, (double)f, vec3d.z);
        if (this.isSprinting()) {
            float g = this.yaw * 0.017453292F;
            this.setVelocity(this.getVelocity().add((double)(-MathHelper.sin(g) * 0.2F), 0.0D, (double)(MathHelper.cos(g) * 0.2F)));
        }

        this.velocityDirty = true;
        ci.cancel();
    }

    private static Vec3d movementInputToVelocity(Vec3d movementInput, float speed, float yaw) {
        double d = movementInput.lengthSquared();
        Vec3d vec3d = (d > 1.0D ? movementInput.normalize() : movementInput).multiply((double)speed);
        float f = MathHelper.sin(yaw * 0.017453292F);
        float g = MathHelper.cos(yaw * 0.017453292F);
        return new Vec3d(vec3d.x * (double)g - vec3d.z * (double)f, vec3d.y, vec3d.z * (double)g + vec3d.x * (double)f);
    }

   /* public Vec3d method_26318(Vec3d vec3d, float f) {
      this.updateVelocity(this.getMovementSpeed(f), vec3d);
      this.setVelocity(this.applyClimbingSpeed(this.getVelocity()));
      this.move(MovementType.SELF, this.getVelocity());
      Vec3d vec3d2 = this.getVelocity();
      if ((this.horizontalCollision || this.jumping) && this.isClimbing()) {
         vec3d2 = new Vec3d(vec3d2.x, 0.2D, vec3d2.z);
      }

      return vec3d2;
   }*/
}
