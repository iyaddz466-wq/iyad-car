package com.carmod;

/** Shape + performance class of a car. All sizes are in blocks. */
public enum BodyType {
    //          len   wid   clr   bodyH cabL  cabH  cabZ    wheelR top   seats spoiler
    SEDAN   (4.2f, 1.75f, 0.25f, 0.50f, 2.0f, 0.48f, -0.15f, 0.40f, 1.35, 4, false),
    HATCH   (3.5f, 1.65f, 0.25f, 0.55f, 2.0f, 0.50f, -0.25f, 0.38f, 1.15, 4, false),
    SUV     (4.3f, 1.85f, 0.45f, 0.70f, 2.7f, 0.55f, -0.30f, 0.50f, 1.25, 4, false),
    OFFROAD (3.9f, 1.75f, 0.50f, 0.75f, 2.5f, 0.60f, -0.25f, 0.55f, 1.10, 4, false),
    PICKUP  (4.7f, 1.90f, 0.50f, 0.65f, 1.6f, 0.55f,  0.60f, 0.52f, 1.30, 3, false),
    SPORT   (4.2f, 1.80f, 0.17f, 0.42f, 1.6f, 0.38f, -0.20f, 0.38f, 1.75, 2, true),
    MUSCLE  (4.5f, 1.90f, 0.20f, 0.50f, 1.7f, 0.42f, -0.45f, 0.42f, 1.65, 4, true),
    SUPER   (4.3f, 1.95f, 0.13f, 0.38f, 1.4f, 0.34f, -0.10f, 0.40f, 2.20, 2, true),
    HYPER   (4.3f, 1.95f, 0.13f, 0.38f, 1.4f, 0.34f, -0.10f, 0.40f, 2.80, 2, true),
    LUXURY  (4.8f, 1.90f, 0.25f, 0.52f, 2.3f, 0.50f, -0.30f, 0.42f, 1.50, 4, false),
    VAN     (4.4f, 1.90f, 0.30f, 0.75f, 3.3f, 0.65f, -0.20f, 0.45f, 1.00, 4, false),
    CLASSIC (3.4f, 1.55f, 0.30f, 0.55f, 1.9f, 0.55f, -0.20f, 0.36f, 0.85, 4, false);

    public final float length, width, clearance, bodyHeight, cabinLength, cabinHeight, cabinZ, wheelRadius;
    public final double topSpeed;
    public final int seats;
    public final boolean spoiler;

    BodyType(float length, float width, float clearance, float bodyHeight, float cabinLength,
             float cabinHeight, float cabinZ, float wheelRadius, double topSpeed, int seats, boolean spoiler) {
        this.length = length;
        this.width = width;
        this.clearance = clearance;
        this.bodyHeight = bodyHeight;
        this.cabinLength = cabinLength;
        this.cabinHeight = cabinHeight;
        this.cabinZ = cabinZ;
        this.wheelRadius = wheelRadius;
        this.topSpeed = topSpeed;
        this.seats = seats;
        this.spoiler = spoiler;
    }
}
