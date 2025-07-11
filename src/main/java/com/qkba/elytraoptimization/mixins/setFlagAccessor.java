package com.qkba.elytraoptimization.mixins;

import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Entity.class)
public interface setFlagAccessor {
    @Invoker("setFlag")
    void callsetFlag(int index, boolean value);
}