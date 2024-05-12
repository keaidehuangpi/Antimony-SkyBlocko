package com.greencat.antimony.core;

import com.greencat.antimony.core.event.CustomEventHandler;
import com.greencat.antimony.utils.SmoothRotation;
import com.greencat.antimony.utils.Utils;
import net.minecraft.block.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.util.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class nukerCore2 {
    public BlockPos pos;
    public BlockPos lastPos;
    public boolean active = false;
    public boolean enable = false;
    public byte hitDelay = 0;
    public boolean ignoreGround = false;
    public RotationType rotation = RotationType.SERVER_ROTATION;
    public MiningType miningType = MiningType.NORMAL;
    public boolean requestBlock = false;
    private float damageProgress;
    private String wrapperName;
    private int refreshTick = 0;
    List<BlockPos> ignoreList = new ArrayList<>();

    public nukerCore2(String wrapperName) {
            MinecraftForge.EVENT_BUS.register(this);
            CustomEventHandler.EVENT_BUS.register(this);
            this.wrapperName = wrapperName;
    }
    public void setActive(boolean isActive){
        damageProgress = 0;
        active = isActive;
    }
    public void putBlock(BlockPos pos){
        this.pos = pos;
        this.requestBlock = false;
    }
    public void init(){
        setActive(true);
        pos = null;
        requestBlock = false;
        lastPos = null;
    }
    public void post(){
        setActive(false);
        pos = null;
        requestBlock = false;
        lastPos = null;
    }
    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event){
        if(!this.enable && nukerWrapper.enable){
            nukerWrapper.enable();
        }
        if (refreshTick + 1 > 10) {
            refreshTick = 0;
            ignoreList.clear();
        } else {
            refreshTick = refreshTick + 1;
        }
    }
    @SubscribeEvent
    public void BlockChangeEvent(CustomEventHandler.BlockChangeEvent event) {
        if(BlockPosEquals(pos,event.pos) && enable && miningType == MiningType.NORMAL){
            requestBlock = true;
            post();
            nukerWrapper.disable();
        }
    }
    @SubscribeEvent
    public void onMotionChange(CustomEventHandler.MotionChangeEvent.Pre event) {
        if(enable) {
            BlockPos tempPos = null;
            requestBlock = active && (pos == null || Minecraft.getMinecraft().theWorld.getBlockState(pos).getBlock() == Blocks.air);
            if (Minecraft.getMinecraft().thePlayer != null && Minecraft.getMinecraft().theWorld != null && active && pos != null) {
                    if (damageProgress > 100.0F) {
                        damageProgress = 0.0F;
                    }

                    if (pos != null && Minecraft.getMinecraft().theWorld != null) {
                        IBlockState blockState = Minecraft.getMinecraft().theWorld.getBlockState(pos);
                        if (blockState.getBlock() == Blocks.bedrock || blockState.getBlock() == Blocks.air) {
                            damageProgress = 0.0F;
                        }
                    }
                    if (pos != null) {
                        if (this.hitDelay > 0) {
                            --this.hitDelay;
                            return;
                        }
                        //new Utils().devLog("dmg progress:" + damageProgress + " last pos:" + lastPos + " pos:" + pos);
                        if (damageProgress == 0.0F && (lastPos == null || pos != lastPos)) {
                            this.lastPos = pos;
                            Minecraft.getMinecraft().getNetHandler().getNetworkManager().sendPacket(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.START_DESTROY_BLOCK, pos, EnumFacing.DOWN));
                            if(miningType == MiningType.ONE_TICK){
                                if(ignoreList.size() + 1 > 10){
                                    ignoreList.clear();
                                }
                                tempPos = pos;
                                ignoreList.add(pos);
                                requestBlock = true;
                                post();
                                nukerWrapper.disable();
                            }
                        }

                        Minecraft.getMinecraft().thePlayer.swingItem();
                        ++damageProgress;
                    }
                
            }
            if (active && (pos != null || miningType == MiningType.ONE_TICK)) {
                try {
                    if (rotation == RotationType.SERVER_ROTATION) {
                        float[] angles = Utils.getRotation(miningType == MiningType.ONE_TICK ? Objects.requireNonNull(tempPos) : pos, Utils.getClosestEnum(miningType == MiningType.ONE_TICK ? Objects.requireNonNull(tempPos) : pos));
                        event.yaw = angles[0];
                        event.pitch = angles[1];
                    }
                    if (rotation == RotationType.ROTATION) {
                        float[] angles = Utils.getRotation(miningType == MiningType.ONE_TICK ? Objects.requireNonNull(tempPos) : pos, Utils.getClosestEnum(miningType == MiningType.ONE_TICK ? Objects.requireNonNull(tempPos) : pos));
                        Minecraft.getMinecraft().thePlayer.rotationYaw = angles[0];
                        Minecraft.getMinecraft().thePlayer.rotationPitch = angles[1];
                    }
                    if (rotation == RotationType.SMOOTH) {
                        SmoothRotation.smoothLook(Utils.getRotation(miningType == MiningType.ONE_TICK ? Objects.requireNonNull(tempPos) : pos), 5, () -> {
                        });
                    }
                } catch(NullPointerException ignored){

                }
            }
        }
    }
    public BlockPos closestMineableBlock(Block block) {
        int r = 5;
        if (Minecraft.getMinecraft().thePlayer == null) return null;
        BlockPos playerPos = Minecraft.getMinecraft().thePlayer.getPosition();
        playerPos = playerPos.add(0, 1, 0);
        Vec3 playerVec = Minecraft.getMinecraft().thePlayer.getPositionVector();
        Vec3i vec3i = new Vec3i(r, r, r);
        ArrayList<Vec3> chests = new ArrayList<>();
        if (playerPos != null) {
            for (BlockPos blockPos : BlockPos.getAllInBox(playerPos.add(vec3i), playerPos.subtract(vec3i))) {
                IBlockState blockState = Minecraft.getMinecraft().theWorld.getBlockState(blockPos);
                if (blockState.getBlock() == block) {

                    Vec3 eyesPos = new Vec3(Minecraft.getMinecraft().thePlayer.posX, Minecraft.getMinecraft().thePlayer.getEntityBoundingBox().minY + Minecraft.getMinecraft().thePlayer.eyeHeight, Minecraft.getMinecraft().thePlayer.posZ);
                    Vec3 blockVec = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);

                    MovingObjectPosition rayTrace = Minecraft.getMinecraft().theWorld.rayTraceBlocks(eyesPos, blockVec, false, true, false);

                    if (rayTrace == null || !rayTrace.getBlockPos().equals(pos)) {
                        continue;
                    }

                    if(blockPos.getY() >= Minecraft.getMinecraft().thePlayer.posY || !ignoreGround) {
                        if (miningType == MiningType.NORMAL) {
                            chests.add(new Vec3(blockPos.getX() + 0.5, blockPos.getY(), blockPos.getZ() + 0.5));
                        } else {
                            Vec3 vec3 = new Vec3(blockPos.getX() + 0.5, blockPos.getY(), blockPos.getZ() + 0.5);
                            if (!isIgnored(new BlockPos(vec3.xCoord, vec3.yCoord, vec3.zCoord))) {
                                chests.add(vec3);
                            }
                        }
                    }
                }
            }
        }
        double smallest = 9999;
        Vec3 closest = null;
        for (Vec3 chest : chests) {
            double dist = chest.distanceTo(playerVec);
            if (dist < smallest) {
                smallest = dist;
                closest = chest;
            }
        }
        if (closest != null && smallest < 5) {
            return new BlockPos(closest.xCoord, closest.yCoord, closest.zCoord);
        }
        return null;
    }
    public BlockPos closestCropBlock() {
        int r = 5;
        if (Minecraft.getMinecraft().thePlayer == null) return null;
        BlockPos playerPos = Minecraft.getMinecraft().thePlayer.getPosition();
        playerPos = playerPos.add(0, 1, 0);
        Vec3 playerVec = Minecraft.getMinecraft().thePlayer.getPositionVector();
        Vec3i vec3i = new Vec3i(r, r, r);
        ArrayList<Vec3> chests = new ArrayList<>();
        if (playerPos != null) {
            for (BlockPos blockPos : BlockPos.getAllInBox(playerPos.add(vec3i), playerPos.subtract(vec3i))) {
                IBlockState blockState = Minecraft.getMinecraft().theWorld.getBlockState(blockPos);
                if (isValidCrop(blockState)) {
                    if(blockPos.getY() >= Minecraft.getMinecraft().thePlayer.posY || !ignoreGround) {
                        if (miningType == MiningType.NORMAL) {
                            chests.add(new Vec3(blockPos.getX() + 0.5, blockPos.getY(), blockPos.getZ() + 0.5));
                        } else {
                            Vec3 vec3 = new Vec3(blockPos.getX() + 0.5, blockPos.getY(), blockPos.getZ() + 0.5);
                            if (!isIgnored(new BlockPos(vec3.xCoord, vec3.yCoord, vec3.zCoord))) {
                                chests.add(vec3);
                            }
                        }
                    }
                }
            }
        }
        double smallest = 9999;
        Vec3 closest = null;
        for (Vec3 chest : chests) {
            double dist = chest.distanceTo(playerVec);
            if (dist < smallest) {
                smallest = dist;
                closest = chest;
            }
        }
        if (closest != null && smallest < 5) {
            return new BlockPos(closest.xCoord, closest.yCoord, closest.zCoord);
        }
        return null;
    }
    private Boolean isValidCrop(IBlockState block) {
            if (block.getBlock() == Blocks.potatoes) {
                return block.getValue(BlockCrops.AGE) == 7;
            }
            if (block.getBlock() == Blocks.carrots) {
                return block.getValue(BlockCrops.AGE) == 7;
            }
            if ((block.getBlock() == Blocks.brown_mushroom || block.getBlock() == Blocks.red_mushroom)) {
                return true;
            }
            if (block.getBlock() == Blocks.nether_wart) {
                return block.getValue(BlockNetherWart.AGE) == 3;
            }
            if (block.getBlock() == Blocks.wheat){
                return block.getValue(BlockCrops.AGE) == 7;
            }
        return false;
    }
    public BlockPos closestMineableBlock(List<Block> block) {
        int r = 5;
        if (Minecraft.getMinecraft().thePlayer == null) return null;
        BlockPos playerPos = Minecraft.getMinecraft().thePlayer.getPosition();
        playerPos = playerPos.add(0, 1, 0);
        Vec3 playerVec = Minecraft.getMinecraft().thePlayer.getPositionVector();
        Vec3i vec3i = new Vec3i(r, r, r);
        ArrayList<Vec3> chests = new ArrayList<>();
        if (playerPos != null) {
            for (BlockPos blockPos : BlockPos.getAllInBox(playerPos.add(vec3i), playerPos.subtract(vec3i))) {
                IBlockState blockState = Minecraft.getMinecraft().theWorld.getBlockState(blockPos);
                for(Block b : block) {
                    if(blockPos.getY() >= Minecraft.getMinecraft().thePlayer.posY || !ignoreGround) {
                        if (blockState.getBlock() == b) {
                            if (miningType == MiningType.NORMAL) {
                                chests.add(new Vec3(blockPos.getX() + 0.5, blockPos.getY(), blockPos.getZ() + 0.5));
                            } else {
                                Vec3 vec3 = new Vec3(blockPos.getX() + 0.5, blockPos.getY(), blockPos.getZ() + 0.5);
                                if (!isIgnored(new BlockPos(vec3.xCoord, vec3.yCoord, vec3.zCoord))) {
                                    chests.add(vec3);
                                }
                            }
                        }
                    }
                }
            }
        }
        double smallest = 9999;
        Vec3 closest = null;
        for (Vec3 chest : chests) {
            double dist = chest.distanceTo(playerVec);
            if (dist < smallest) {
                smallest = dist;
                closest = chest;
            }
        }
        if (closest != null && smallest < 5) {
            return new BlockPos(closest.xCoord, closest.yCoord, closest.zCoord);
        }
        return null;
    }
    public BlockPos closestMineableBlock(List<Block> block,Boolean titanium) {
        int r = 5;
        if (Minecraft.getMinecraft().thePlayer == null) return null;
        BlockPos playerPos = Minecraft.getMinecraft().thePlayer.getPosition();
        playerPos = playerPos.add(0, 1, 0);
        Vec3 playerVec = Minecraft.getMinecraft().thePlayer.getPositionVector();
        Vec3i vec3i = new Vec3i(r, r, r);
        ArrayList<Vec3> chests = new ArrayList<>();
        if (playerPos != null) {
            for (BlockPos blockPos : BlockPos.getAllInBox(playerPos.add(vec3i), playerPos.subtract(vec3i))) {
                IBlockState blockState = Minecraft.getMinecraft().theWorld.getBlockState(blockPos);
                for(Block b : block) {
                    if (blockState.getBlock() == b) {
                        if(blockPos.getY() >= Minecraft.getMinecraft().thePlayer.posY || !ignoreGround) {
                            if (titanium && b == Blocks.stone) {
                                int meta = blockState.getValue(BlockStone.VARIANT).getMetadata();
                                if (meta == 4) {
                                    if (miningType == MiningType.NORMAL) {
                                        chests.add(new Vec3(blockPos.getX() + 0.5, blockPos.getY(), blockPos.getZ() + 0.5));
                                    } else {
                                        Vec3 vec3 = new Vec3(blockPos.getX() + 0.5, blockPos.getY(), blockPos.getZ() + 0.5);
                                        if (!isIgnored(new BlockPos(vec3.xCoord, vec3.yCoord, vec3.zCoord))) {
                                            chests.add(vec3);
                                        }
                                    }
                                }
                            } else {
                                if (miningType == MiningType.NORMAL) {
                                    chests.add(new Vec3(blockPos.getX() + 0.5, blockPos.getY(), blockPos.getZ() + 0.5));
                                } else {
                                    Vec3 vec3 = new Vec3(blockPos.getX() + 0.5, blockPos.getY(), blockPos.getZ() + 0.5);
                                    if (!isIgnored(new BlockPos(vec3.xCoord, vec3.yCoord, vec3.zCoord))) {
                                        chests.add(vec3);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        double smallest = 9999;
        Vec3 closest = null;
        for (Vec3 chest : chests) {
            double dist = chest.distanceTo(playerVec);
            if (dist < smallest) {
                smallest = dist;
                closest = chest;
            }
        }
        if (closest != null && smallest < 5) {
            return new BlockPos(closest.xCoord, closest.yCoord, closest.zCoord);
        }
        return null;
    }
    public BlockPos closestMineableBlockBlueWool(List<Block> block,Boolean titanium) {
        int r = 3;
        if (Minecraft.getMinecraft().thePlayer == null) return null;
        BlockPos playerPos = Minecraft.getMinecraft().thePlayer.getPosition();
        playerPos = playerPos.add(0, 1, 0);
        Vec3 playerVec = Minecraft.getMinecraft().thePlayer.getPositionVector();
        Vec3i vec3i = new Vec3i(r, r, r);
        ArrayList<Vec3> chests = new ArrayList<>();
        ArrayList<Vec3> woolList = new ArrayList<>();
        if (playerPos != null) {
            for (BlockPos blockPos : BlockPos.getAllInBox(playerPos.add(vec3i), playerPos.subtract(vec3i))) {
                IBlockState blockState = Minecraft.getMinecraft().theWorld.getBlockState(blockPos);
                for(Block b : block) {
                    if (blockState.getBlock() == b) {
                        if(blockPos.getY() >= Minecraft.getMinecraft().thePlayer.posY || !ignoreGround) {
                            if (titanium && b == Blocks.stone) {
                                int meta = blockState.getValue(BlockStone.VARIANT).getMetadata();
                                if (meta == 4) {
                                    if (miningType == MiningType.NORMAL) {
                                        chests.add(new Vec3(blockPos.getX() + 0.5, blockPos.getY(), blockPos.getZ() + 0.5));
                                    } else {
                                        Vec3 vec3 = new Vec3(blockPos.getX() + 0.5, blockPos.getY(), blockPos.getZ() + 0.5);
                                        if (!isIgnored(new BlockPos(vec3.xCoord, vec3.yCoord, vec3.zCoord))) {
                                            chests.add(vec3);
                                        }
                                    }
                                }
                            } else if(b == Blocks.wool && blockState.getValue(BlockColored.COLOR) == EnumDyeColor.LIGHT_BLUE){
                                if (miningType == MiningType.NORMAL) {
                                    woolList.add(new Vec3(blockPos.getX() + 0.5, blockPos.getY(), blockPos.getZ() + 0.5));
                                } else {
                                    Vec3 vec3 = new Vec3(blockPos.getX() + 0.5, blockPos.getY(), blockPos.getZ() + 0.5);
                                    if (!isIgnored(new BlockPos(vec3.xCoord, vec3.yCoord, vec3.zCoord))) {
                                        woolList.add(vec3);
                                    }
                                }
                            } else {
                                if (miningType == MiningType.NORMAL) {
                                    chests.add(new Vec3(blockPos.getX() + 0.5, blockPos.getY(), blockPos.getZ() + 0.5));
                                } else {
                                    Vec3 vec3 = new Vec3(blockPos.getX() + 0.5, blockPos.getY(), blockPos.getZ() + 0.5);
                                    if (!isIgnored(new BlockPos(vec3.xCoord, vec3.yCoord, vec3.zCoord))) {
                                        chests.add(vec3);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        double smallest = 9999;
        Vec3 closest = null;
        if(woolList.isEmpty()) {
            for (Vec3 chest : chests) {
                double dist = chest.distanceTo(playerVec);
                if (dist < smallest) {
                    smallest = dist;
                    closest = chest;
                }
            }
        } else {
            for (Vec3 chest : woolList) {
                double dist = chest.distanceTo(playerVec);
                if (dist < smallest) {
                    smallest = dist;
                    closest = chest;
                }
            }
        }
        if (closest != null && smallest < 5) {
            return new BlockPos(closest.xCoord, closest.yCoord, closest.zCoord);
        }
        return null;
    }
    public boolean BlockPosEquals(BlockPos pos1,BlockPos pos2) {
        if(pos1 != null && pos2 != null) {
            return pos1.getX() == pos1.getX() && pos1.getY() == pos2.getY() && pos1.getZ() == pos2.getZ();
        }
        return false;
    }
    public BlockPos BlockPosMin(BlockPos pos1,BlockPos pos2){
        if(pos1 != null && pos2 != null) {
            double pos1Distance = pos1.distanceSq(Minecraft.getMinecraft().thePlayer.getPosition());
            double pos2Distance = pos2.distanceSq(Minecraft.getMinecraft().thePlayer.getPosition());
            double diff = pos1Distance - pos2Distance;
            if(diff < 0){
                return pos1;
            } else if(diff > 0){
                return pos2;
            } else {
                return pos1;
            }
        }
        return null;
    }
    public boolean isIgnored(BlockPos pos){
        boolean isIgnored = false;
        for(BlockPos ignored : ignoreList){
            if(BlockPosEquals(pos,ignored)){
                isIgnored = true;
                break;
            }
        }
        return isIgnored;
    }
    public enum RotationType{
        SERVER_ROTATION,ROTATION,SMOOTH
    }
    public enum MiningType{
        NORMAL,ONE_TICK
    }
}
