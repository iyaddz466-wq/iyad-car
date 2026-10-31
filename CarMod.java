package com.carmod;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class CarMod implements ModInitializer {
    public static final String MOD_ID = "carmod";

    public static EntityType<CarEntity> CAR_ENTITY;
    public static final List<CarItem> CAR_ITEMS = new ArrayList<>();

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        CAR_ENTITY = Registry.register(
                BuiltInRegistries.ENTITY_TYPE,
                id("car"),
                EntityType.Builder.<CarEntity>of(CarEntity::new, MobCategory.MISC)
                        .sized(1.9f, 1.6f)
                        .clientTrackingRange(10)
                        .updateInterval(1)
                        .build("car"));

        List<CarSpec> all = CarCatalog.ALL;
        for (int i = 0; i < all.size(); i++) {
            CarItem item = new CarItem(new Item.Properties().stacksTo(1), i);
            Registry.register(BuiltInRegistries.ITEM, id(all.get(i).id()), item);
            CAR_ITEMS.add(item);
        }

        Registry.register(
                BuiltInRegistries.CREATIVE_MODE_TAB,
                id("cars"),
                FabricItemGroup.builder()
                        .title(Component.literal("Car Mod"))
                        .icon(() -> new ItemStack(CAR_ITEMS.get(0)))
                        .displayItems((params, output) -> {
                            for (CarItem item : CAR_ITEMS) {
                                output.accept(item);
                            }
                        })
                        .build());
    }
}
