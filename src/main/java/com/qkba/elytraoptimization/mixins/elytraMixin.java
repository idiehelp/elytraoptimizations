package com.qkba.elytraoptimization.mixins;

import net.minecraft.entity.MovementType;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.registry.Registries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.Optional;

@Mixin(PlayerEntity.class)
public abstract class elytraMixin {
    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    public void onTravel(Vec3d movementInput, CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity)(Object)this;

        if (!player.isFallFlying() || player.isTouchingWater() || player.isInLava()) return;

        Identifier gravityId = Identifier.of("generic", "gravity");
        Optional<RegistryEntry.Reference<EntityAttribute>> gravityAttrOpt = Registries.ATTRIBUTE.getEntry(gravityId);

        double gravity = gravityAttrOpt
                .map(player::getAttributeValue)
                .orElse(0.08); // fallback if not found

        float pitch = player.getPitch();
        float pitchRad = pitch * 0.017453292519943295769236907684886127134428718885417F;
        double cosPitch = Math.cos(pitchRad);
        double sinPitch = Math.sin(pitchRad);
        double l = cosPitch * cosPitch;

        Vec3d velocity = player.getVelocity();
        Vec3d look = player.getRotationVector();

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

            double j = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);

            if (pitch < 0.0F) {
                double m = Math.fma(-Math.fma(j, sinPitch, 0.0), 0.04, 0.0);
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

        ci.cancel();
    }
}