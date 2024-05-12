package com.greencat.antimony.common.mixins;

import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetHandlerPlayClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = {NetHandlerPlayClient.class})
public abstract class MixinNetHandlerPlayClient {
    @Shadow
    private
    WorldClient clientWorldController;
}