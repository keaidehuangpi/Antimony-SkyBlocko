package com.greencat.antimony.common.mixins;

import akka.pattern.Patterns;
import com.greencat.Antimony;
import com.greencat.antimony.common.function.FPSAccelerator;
import com.greencat.antimony.utils.Utils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.BlockSapling;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Mixin(Block.class)
public abstract class MixinBlock {
    @Shadow
    protected double minX,minY,minZ,maxX,maxY,maxZ;
    @Shadow public abstract Vec3 modifyAcceleration(World p_modifyAcceleration_1_, BlockPos p_modifyAcceleration_2_, Entity p_modifyAcceleration_3_, Vec3 p_modifyAcceleration_4_);

    @Inject(method = "isCollidable", at = @At("HEAD"), cancellable = true)
    private void isCollidable(CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
        if(Antimony.NoSaplingBound && (Block.getIdFromBlock((Block) (Object) this) == Block.getIdFromBlock(Blocks.sapling))){
            callbackInfoReturnable.setReturnValue(false);
        }
        if(Antimony.NoTreeBound && ((Block.getIdFromBlock((Block) (Object) this) == Block.getIdFromBlock(Blocks.leaves)) || (Block.getIdFromBlock((Block) (Object) this) == Block.getIdFromBlock(Blocks.leaves2)) || (Block.getIdFromBlock((Block) (Object) this) == Block.getIdFromBlock(Blocks.log)) || (Block.getIdFromBlock((Block) (Object) this) == Block.getIdFromBlock(Blocks.log2)))){
            callbackInfoReturnable.setReturnValue(false);
        }
    }
}
