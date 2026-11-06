package com.carmod;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Looks up optional custom shapes: assets/carmod/car_models/<car id>.json (from any resource pack). */
public final class CarCustomModels {
    private static final Logger LOGGER = LoggerFactory.getLogger("carmod");
    private static final Map<String, CustomModel> CACHE = new HashMap<>();

    private CarCustomModels() {
    }

    public static void clear() {
        CACHE.clear();
    }

    public static CustomModel get(CarSpec spec) {
        CustomModel model = CACHE.get(spec.id());
        if (model == null) {
            model = load(spec);
            CACHE.put(spec.id(), model);
        }
        return model;
    }

    private static CustomModel load(CarSpec spec) {
        ResourceManager manager = Minecraft.getInstance().getResourceManager();
        Optional<Resource> resource = manager.getResource(CarMod.id("car_models/" + spec.id() + ".json"));
        if (resource.isEmpty()) {
            return CustomModel.NONE;
        }
        try (Reader reader = resource.get().openAsReader()) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            CustomModel model = CustomModel.parse(root, spec.id(), spec.body().length);
            if (model == CustomModel.NONE) {
                LOGGER.warn("Car model for {} has no usable cubes", spec.id());
            } else {
                LOGGER.info("Loaded custom car model for {}", spec.id());
            }
            return model;
        } catch (Exception e) {
            LOGGER.error("Could not read car model for " + spec.id(), e);
            return CustomModel.NONE;
        }
    }
}
