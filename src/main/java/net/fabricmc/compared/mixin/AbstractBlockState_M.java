package net.fabricmc.compared.mixin;

import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.CampfireBlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.enums.BedPart;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(AbstractBlock.AbstractBlockState.class)
public abstract class AbstractBlockState_M {

    @Shadow public abstract Block getBlock();

    @Shadow protected abstract BlockState asBlockState();

    // Make comparator behaviour editable by extensions
    @Inject(method = "hasComparatorOutput", at = @At("RETURN"), cancellable = true)
    public void hasComparatorOutput(CallbackInfoReturnable<Boolean> cir) {
        if (this.getBlock() instanceof CampfireBlock) {
            cir.setReturnValue(true);
        }

        if (this.getBlock() instanceof AbstractSignBlock) {
            cir.setReturnValue(true);
        }

        if (this.getBlock() instanceof BedBlock) {
            cir.setReturnValue(true);
        }
    }
    @Inject(method = "getComparatorOutput", at = @At("RETURN"), cancellable = true)
    public void getComparatorOutput(World world, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        if (this.getBlock() instanceof CampfireBlock) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof CampfireBlockEntity cBE) {
                int lit = this.asBlockState().get(CampfireBlock.LIT) ? 1 : 0;
                int signal = this.asBlockState().get(CampfireBlock.SIGNAL_FIRE) && lit == 1 ? 1 : 0;
                DefaultedList<ItemStack> food = cBE.getItemsBeingCooked();
                int foodFound = 0;
                for (ItemStack itemStack : food) {
                    if (itemStack != ItemStack.EMPTY) {
                        foodFound++;
                    }
                }
                int output = (foodFound * 3) + lit + signal;
                cir.setReturnValue(output);
            }
        }

        if (this.getBlock() instanceof AbstractSignBlock) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof SignBlockEntity s) {
                int glow = s.isGlowingText() ? 1 : 0;
                ArrayList<String> texts = new ArrayList<String>();
                for (var i = 0; i <= 3; i++) {
                    String text = s.getTextOnRow(i, true).asString();
                    if (text.length() > 0) {
                        texts.add(text);
                    }
                }
                int output = texts.size() == 0 ? 0 : texts.size() == 1 ? 3 : texts.size() == 2 ? 7 : texts.size() == 3 ? 11 : 14;
                cir.setReturnValue(output + glow);
            }
        }

        if (this.getBlock() instanceof BedBlock) {
            if (this.asBlockState().get(BedBlock.OCCUPIED)) {
                int sleeper;
                List<VillagerEntity> list;
                if (this.asBlockState().get(BedBlock.PART) == BedPart.HEAD) {
                    list = world.getEntitiesByClass(VillagerEntity.class, new Box(pos), LivingEntity::isSleeping);
                } else {
                    BlockPos head = pos.offset(this.asBlockState().get(BedBlock.FACING));
                    list = world.getEntitiesByClass(VillagerEntity.class, new Box(head), LivingEntity::isSleeping);
                }
                if (!list.isEmpty()) {
                    sleeper = 1;
                } else {
                    sleeper = 2;
                }
                cir.setReturnValue(sleeper);
            }
        }
    }
    @Inject(method = "onStateReplaced", at = @At("RETURN"))
    public void onStateReplaced(World world, BlockPos pos, BlockState state, boolean moved, CallbackInfo ci) {
        if (state.getBlock() instanceof CampfireBlock) {
            world.updateComparators(pos, state.getBlock());
        }

        if (state.getBlock() instanceof AbstractSignBlock) {
            world.updateComparators(pos, state.getBlock());
        }

        if (state.getBlock() instanceof BedBlock) {
            world.updateComparators(pos, state.getBlock());
        }
    }
}
