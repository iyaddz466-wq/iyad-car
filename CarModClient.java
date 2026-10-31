package com.carmod;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;

public class CarModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(CarMod.CAR_ENTITY, CarRenderer::new);

        // simple speedometer above the hotbar while sitting in a car
        HudRenderCallback.EVENT.register((graphics, tickCounter) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.options.hideGui) {
                return;
            }
            if (!(mc.player.getVehicle() instanceof CarEntity car)) {
                return;
            }
            int x = graphics.guiWidth() / 2;
            int y = graphics.guiHeight() - 62;
            graphics.drawCenteredString(mc.font, car.getSpec().displayName(), x, y - 12, 0xFFFFFF);
            graphics.drawCenteredString(mc.font, ((int) Math.round(car.kmh)) + " km/h", x, y, 0xFFD24A);
        });
    }
}
