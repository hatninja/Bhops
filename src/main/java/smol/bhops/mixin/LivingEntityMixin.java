package smol.bhops.mixin;

import me.sargunvohra.mcmods.autoconfig1u.AutoConfig;
import net.minecraft.entity.*;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerAbilities;
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import smol.bhops.config.BhopsConfig;

import java.util.Map;

//Reference Functions from Source:
//FullWalkMove( )
//PlayerMove()

//TODO: Speed Display
//TODO: Custom Jump sounds
//TODO: Water Controls

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {
    private BhopsConfig config = AutoConfig.getConfigHolder(BhopsConfig.class).getConfig();

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
    @Shadow public abstract boolean isClimbing();

    private boolean wasOnGround;


    private final double sv_gravity = 0.08D;

    public LivingEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    public void travel(Vec3d movementInput, CallbackInfo ci) {
        //if (!this.canMoveVoluntarily() && !this.isLogicalSideForUpdatingMovement()) { return; }

        //Cancel override if not in plain walking state..
        if (this.isTouchingWater() || this.isInLava() || this.isFallFlying()) { return; }

        //Toggle Bhops
        if (!config.enableBhops) {return;}

        //Enable for Players only
        if (config.exclusiveToPlayers && this.getType() != EntityType.PLAYER) { return; }

        //Disable on creative flying.
        if (this.getType() == EntityType.PLAYER
                && isFlying((PlayerEntity) this.world.getEntityById(super.getEntityId()))) { return; }

        //Reverse multiplication done by the function that calls this one.
        this.sidewaysSpeed /= 0.98F;
        this.forwardSpeed /= 0.98F;
        double sI = movementInput.x / 0.98F;
        double fI = movementInput.z / 0.98F;
        double uI = movementInput.y;

        //Have no jump cooldown, why not?
        this.jumpingCooldown = 0;

        //Get Slipperiness and Movement speed.
        BlockPos blockPos = this.getVelocityAffectingPos();
        float slipperiness = this.world.getBlockState(blockPos).getBlock().getSlipperiness();
        float friction = 1-(slipperiness*slipperiness);

        //
        //Apply Friction
        //
        boolean fullGrounded = this.wasOnGround && this.onGround; //Allows for no friction 1-frame upon landing.
        if (fullGrounded) {
            Vec3d velFin = this.getVelocity();
            Vec3d horFin = new Vec3d(velFin.x,0.0F,velFin.z);
            float speed = (float) horFin.length();
            if (speed > 0.001F) {
                float drop = 0.0F;

                drop += (speed * config.sv_friction * friction);

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
            Vec3d moveDir = movementInputToVelocity(new Vec3d(sI, 0.0F, fI), 1.0F, this.yaw);
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

        this.setVelocity(this.applyClimbingSpeed(this.getVelocity()));
        this.move(MovementType.SELF, this.getVelocity());

        //
        //Ladder Logic
        //
        Vec3d preVel = this.getVelocity();
        if ((this.horizontalCollision || this.jumping) && this.isClimbing()) {
            preVel = new Vec3d(preVel.x * 0.7D, 0.2D, preVel.z * 0.7D);
        }

        //
        //Apply Gravity (If not in Water)
        //
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
        double dx = this.getX() - this.prevX;
        double dy = this instanceof Flutterer ? this.getY() - this.prevY : 0.0D;
        double dz = this.getZ() - this.prevZ;
        float dist = MathHelper.sqrt(dx * dx + dy * dy + dz * dz) * 4.0F;
        if (dist > 1.0F) { dist = 1.0F; }

        this.limbDistance += (dist - this.limbDistance) * 1F;
        this.limbAngle += this.limbDistance;

        //Override original method.
        ci.cancel();
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

    private static Vec3d movementInputToVelocity(Vec3d movementInput, float speed, float yaw) {
        double d = movementInput.lengthSquared();
        Vec3d vec3d = (d > 1.0D ? movementInput.normalize() : movementInput).multiply((double)speed);
        float f = MathHelper.sin(yaw * 0.017453292F);
        float g = MathHelper.cos(yaw * 0.017453292F);
        return new Vec3d(vec3d.x * (double)g - vec3d.z * (double)f, vec3d.y, vec3d.z * (double)g + vec3d.x * (double)f);
    }

    private static boolean isFlying(PlayerEntity player) {
        return player.abilities.flying;
    }
}
