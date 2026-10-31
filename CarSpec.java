package com.carmod;

/**
 * One drivable car model.
 *
 * @param topSpeedOverride blocks/tick, or a value <= 0 to use the body type default
 */
public record CarSpec(String id, String brand, String model, BodyType body, int color, double topSpeedOverride) {
    public double topSpeed() {
        return topSpeedOverride > 0 ? topSpeedOverride : body.topSpeed;
    }

    public String displayName() {
        return brand + " " + model;
    }
}
