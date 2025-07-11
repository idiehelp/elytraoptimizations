package com.qkba.elytraoptimization.mixins;

import net.minecraft.entity.MovementType;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(PlayerEntity.class)
public abstract class elytraMixin {
    private static final int FALL_FLYING_FLAG_INDEX = 7;

    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    public void onTravel(Vec3d movementInput, CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity) (Object) this;

        if (!player.isFallFlying() || player.isTouchingWater() || player.isInLava()) {
            return;
        }

        Identifier gravityId = Identifier.of("generic", "gravity");
        Optional<RegistryEntry.Reference<EntityAttribute>> gravityAttrOpt =
                Registries.ATTRIBUTE.getEntry(gravityId);
        double gravity = gravityAttrOpt.map(player::getAttributeValue).orElse(0.08);

        player.limitFallDistance();

        // Pre-compute pitch-based factors
        float pitch = player.getPitch();
        float pitchRad = pitch * 0.017453292519943F;
        double cosPitch = Math.cos(pitchRad);
        double sinPitch = Math.sin(pitchRad);
        double l = cosPitch * cosPitch;

        Vec3d velocity = player.getVelocity();
        Vec3d look = player.getRotationVector();

        double j = velocity.horizontalLength();

        double yDelta = gravity * Math.fma(l, 0.75, -1.0);
        velocity = velocity.add(0.0, yDelta, 0.0);

        if (cosPitch > 0.0) {
            if (velocity.y < 0.0) {
                double m = velocity.y * -0.1 * l;
                velocity = velocity.add(
                        look.x * m / cosPitch,
                        m,
                        look.z * m / cosPitch
                );
            }

            if (pitch < 0.0F) {
                double m = j * -sinPitch * 0.04;
                velocity = velocity.add(
                        -look.x * m / cosPitch,
                        m * 3.2,
                        -look.z * m / cosPitch
                );
            }

            double correctionX = Math.fma(look.x / cosPitch, j, -velocity.x);
            double correctionZ = Math.fma(look.z / cosPitch, j, -velocity.z);
            velocity = velocity.add(correctionX * 0.1, 0.0, correctionZ * 0.1);
        }

        player.setVelocity(velocity.x * 0.99, velocity.y * 0.98, velocity.z * 0.99);
        player.move(MovementType.SELF, player.getVelocity());

        boolean isClient = player.getWorld().isClient;

        if (player.horizontalCollision && !isClient) {
            double m = player.getVelocity().horizontalLength();
            double n = j - m;
            float o = (float) Math.fma(n, 10.0, -3.0);

            if (o > 0.0F) {
                SoundEvent fallSound = ((LivingEntityAccessor) player).callGetFallSound((int) o);
                player.playSound(fallSound, 1.0F, 1.0F);
                player.damage(player.getDamageSources().flyIntoWall(), o);

                ((setFlagAccessor) player).callsetFlag(FALL_FLYING_FLAG_INDEX, false);
            }
        }

        if (player.isOnGround() && !isClient) {
            ((setFlagAccessor) player).callsetFlag(FALL_FLYING_FLAG_INDEX, false);
        }

        ci.cancel();
    }
}