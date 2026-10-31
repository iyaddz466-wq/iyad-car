package com.carmod;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * A drivable car. The driver's client simulates the movement (same model vanilla boats use)
 * and the server validates it through the normal vehicle-move packets.
 *
 * Controls: W = gas, S = brake / reverse, A / D = steer, Shift = get out.
 */
public class CarEntity extends Entity {
    private static final EntityDataAccessor<Integer> MODEL =
            SynchedEntityData.defineId(CarEntity.class, EntityDataSerializers.INT);

    /** Forward speed in blocks per tick (only meaningful on the instance that controls the car). */
    private double speed;
    private float deltaYaw;

    /** Client-side visuals (all instances). */
    public float wheelAngle;
    public float wheelAngleO;
    public double kmh;

    public CarEntity(EntityType<? extends CarEntity> type, Level level) {
        super(type, level);
    }

    // ---------------------------------------------------------------- data

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(MODEL, 0);
    }

    public void setModelIndex(int index) {
        this.entityData.set(MODEL, index);
    }

    public int getModelIndex() {
        int i = this.entityData.get(MODEL);
        return Mth.clamp(i, 0, CarCatalog.ALL.size() - 1);
    }

    public CarSpec getSpec() {
        return CarCatalog.ALL.get(getModelIndex());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setModelIndex(tag.getInt("Model"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Model", getModelIndex());
    }

    // ------------------------------------------------------------- basics

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    /** Lets the car climb up to one full block (slabs, stairs, curbs). */
    @Override
    public float maxUpStep() {
        return 1.0f;
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(CarMod.CAR_ITEMS.get(getModelIndex()));
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide || isRemoved()) {
            return true;
        }
        if (source.getEntity() instanceof Player player && !hasPassenger(player)) {
            if (!player.getAbilities().instabuild) {
                spawnAtLocation(getPickResult());
            }
            discard();
        }
        return true;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (player.isSecondaryUseActive()) {
            return InteractionResult.PASS;
        }
        if (!level().isClientSide) {
            return player.startRiding(this) ? InteractionResult.CONSUME : InteractionResult.PASS;
        }
        return InteractionResult.SUCCESS;
    }

    // ---------------------------------------------------------- passengers

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().size() < getSpec().body().seats;
    }

    @Override
    public LivingEntity getControllingPassenger() {
        return getFirstPassenger() instanceof Player player ? player : null;
    }

    @Override
    protected void positionRider(Entity passenger, Entity.MoveFunction callback) {
        if (!hasPassenger(passenger)) {
            return;
        }
        BodyType body = getSpec().body();
        int index = Math.max(0, getPassengers().indexOf(passenger));

        // seat layout: driver front-left (+X is the left side), then front-right, rear-left, rear-right
        double side = (index % 2 == 0) ? 0.4 : -0.4;
        double forward = (index < 2) ? 0.35 + body.cabinZ * 0.5 : -0.65 + body.cabinZ * 0.5;
        if (body.seats <= 2) {
            forward = body.cabinZ * 0.5;
        }
        Vec3 offset = new Vec3(side, 0.0, forward).yRot(-getYRot() * ((float) Math.PI / 180.0f));
        callback.accept(passenger, getX() + offset.x, getY() + 0.3, getZ() + offset.z);

        passenger.setYRot(passenger.getYRot() + deltaYaw);
        passenger.setYHeadRot(passenger.getYHeadRot() + deltaYaw);
        clampRotation(passenger);
    }

    protected void clampRotation(Entity entity) {
        if (entity instanceof LivingEntity living) {
            living.setYBodyRot(getYRot());
        }
        float diff = Mth.wrapDegrees(entity.getYRot() - getYRot());
        float clamped = Mth.clamp(diff, -105.0f, 105.0f);
        entity.yRotO += clamped - diff;
        entity.setYRot(entity.getYRot() + clamped - diff);
        entity.setYHeadRot(entity.getYRot());
    }

    @Override
    public void onPassengerTurned(Entity passenger) {
        clampRotation(passenger);
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        int index = Math.max(0, getPassengers().indexOf(passenger));
        double side = (index % 2 == 0) ? 2.0 : -2.0;
        Vec3 offset = new Vec3(side, 0.0, 0.0).yRot(-getYRot() * ((float) Math.PI / 180.0f));
        return new Vec3(getX() + offset.x, getY(), getZ() + offset.z);
    }

    // ---------------------------------------------------------------- tick

    @Override
    public void tick() {
        super.tick();

        if (isControlledByLocalInstance()) {
            drive();
        } else {
            setDeltaMovement(Vec3.ZERO);
            speed = 0.0;
            deltaYaw = 0.0f;
        }

        updateVisuals();
        checkInsideBlocks();
    }

    private void drive() {
        CarSpec spec = getSpec();
        double top = spec.topSpeed();

        LivingEntity driver = getControllingPassenger();
        float throttle = driver != null ? driver.zza : 0.0f;
        float steer = driver != null ? driver.xxa : 0.0f;

        boolean inWater = isInWater();
        boolean grounded = onGround() || inWater;

        if (grounded) {
            double accel = top * 0.022 * Math.max(0.12, 1.0 - Math.abs(speed) / top);
            if (throttle > 0.0f) {
                speed += speed < 0.0 ? 0.10 : accel;
            } else if (throttle < 0.0f) {
                if (speed > 0.0) {
                    speed -= 0.10;            // brake
                } else {
                    speed -= accel * 0.5;     // reverse
                }
            } else {
                speed *= 0.985;               // coast
            }
            speed = Mth.clamp(speed, -top * 0.3, top);
            if (Math.abs(speed) < 0.004 && throttle == 0.0f) {
                speed = 0.0;
            }
            if (inWater) {
                speed *= 0.92;
            }
        } else {
            speed *= 0.998;
        }

        // steering: needs speed, and gets lazier at top speed
        double absSpeed = Math.abs(speed);
        double turnRate = 5.0 * Math.min(1.0, absSpeed / 0.35) * (1.0 - 0.6 * Math.min(1.0, absSpeed / top));
        float turn = grounded ? (float) (-steer * turnRate * Math.signum(speed)) : 0.0f;
        deltaYaw = turn;
        setYRot(getYRot() + turn);

        double rad = Math.toRadians(getYRot());
        double mx = -Math.sin(rad) * speed;
        double mz = Math.cos(rad) * speed;

        double vy = getDeltaMovement().y;
        if (inWater) {
            vy = (vy + 0.035) * 0.75;
        } else if (onGround()) {
            vy = -0.05;
        } else {
            vy = (vy - 0.08) * 0.98;
        }

        setDeltaMovement(mx, vy, mz);
        move(MoverType.SELF, getDeltaMovement());

        if (horizontalCollision) {
            speed *= 0.45;
        }
    }

    /** Wheel rotation + speedometer value, computed from actual movement so it works for every viewer. */
    private void updateVisuals() {
        double dx = getX() - xo;
        double dz = getZ() - zo;
        double rad = Math.toRadians(getYRot());
        double along = dx * -Math.sin(rad) + dz * Math.cos(rad);
        double dist = Math.sqrt(dx * dx + dz * dz);

        wheelAngleO = wheelAngle;
        wheelAngle += (float) (along / getSpec().body().wheelRadius * 57.29578);
        kmh = kmh * 0.6 + (dist * 72.0) * 0.4;
    }
}
