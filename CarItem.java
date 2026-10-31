package com.carmod;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/** Right-click on the ground to place a car. */
public class CarItem extends Item {
    private final int index;

    public CarItem(Properties properties, int index) {
        super(properties);
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public CarSpec getSpec() {
        return CarCatalog.ALL.get(index);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.literal(getSpec().displayName());
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        CarEntity car = CarMod.CAR_ENTITY.create(level);
        if (car == null) {
            return InteractionResult.FAIL;
        }
        Player player = context.getPlayer();
        float yaw = player != null ? player.getYRot() : 0.0f;
        car.setModelIndex(index);
        car.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yaw, 0.0f);
        level.addFreshEntity(car);
        if (player == null || !player.getAbilities().instabuild) {
            context.getItemInHand().shrink(1);
        }
        return InteractionResult.CONSUME;
    }
}
