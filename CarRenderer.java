package com.carmod;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Draws every car procedurally out of coloured boxes (no external model files needed).
 * Local space: +Z is the front of the car, +Y is up, the car is centred on X/Z.
 */
public class CarRenderer extends EntityRenderer<CarEntity> {
    private static final ResourceLocation WHITE = CarMod.id("textures/entity/white.png");

    private static final int GLASS = 0x1B2B3C;
    private static final int TIRE = 0x1A1A1A;
    private static final int RIM = 0xB8B8B8;
    private static final int HEADLIGHT = 0xFFF2B0;
    private static final int TAILLIGHT = 0xD01010;
    private static final int DARK = 0x222222;

    public CarRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 1.1f;
    }

    @Override
    public ResourceLocation getTextureLocation(CarEntity entity) {
        return WHITE;
    }

    @Override
    public void render(CarEntity car, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int light) {
        super.render(car, entityYaw, partialTick, poseStack, buffers, light);

        CarSpec spec = car.getSpec();
        BodyType b = spec.body();
        int paint = spec.color();

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-entityYaw));

        // a custom shape from a resource pack replaces the built-in boxy one
        CustomModel custom = CarCustomModels.get(spec);
        if (custom != CustomModel.NONE) {
            custom.render(poseStack.last(), buffers.getBuffer(RenderType.entityCutoutNoCull(custom.texture)), light);
            poseStack.popPose();
            return;
        }

        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(WHITE));

        float hw = b.width / 2.0f;
        float hl = b.length / 2.0f;
        float bodyBottom = b.clearance;
        float bodyTop = b.clearance + b.bodyHeight;

        CarAccents.Accent accent = CarAccents.get(spec.id());
        boolean wedge = accent != null && accent.wedge();
        boolean slopedTail = accent != null && accent.slopedTail();
        boolean boxy = accent != null && accent.boxy();
        boolean bed = accent != null && accent.bed();
        boolean hoodBulge = accent != null && accent.hoodBulge();
        boolean domedRoof = accent != null && accent.domedRoof();
        boolean roundLights = accent != null && accent.roundLights();
        boolean grilleSlats = accent != null && accent.grilleSlats();
        boolean frontBadge = accent != null && accent.frontBadge();

        // main body: a flat box by default, or a stepped nose/tail/bed profile for accented cars
        if (!wedge && !slopedTail && !bed) {
            box(poseStack.last(), vc, -hw, bodyBottom, -hl, hw, bodyTop, hl, paint, light);
        } else {
            float midFront = wedge ? hl - b.length * 0.32f : hl;
            float midBack = slopedTail ? -hl + b.length * 0.28f : (bed ? -hl + b.length * 0.42f : -hl);
            box(poseStack.last(), vc, -hw, bodyBottom, midBack, hw, bodyTop, midFront, paint, light);
            if (wedge) {
                float noseTop = bodyBottom + (bodyTop - bodyBottom) * 0.55f;
                float noseW = hw * 0.82f;
                box(poseStack.last(), vc, -noseW, bodyBottom, midFront - 0.02f, noseW, noseTop, hl, paint, light);
            } else if (!bed) {
                box(poseStack.last(), vc, -hw, bodyBottom, midFront - 0.02f, hw, bodyTop, hl, paint, light);
            }
            if (slopedTail) {
                float tailTop = bodyBottom + (bodyTop - bodyBottom) * 0.6f;
                box(poseStack.last(), vc, -hw, bodyBottom, -hl, hw, tailTop, midBack + 0.02f, paint, light);
            } else if (bed) {
                float bedTop = bodyBottom + (bodyTop - bodyBottom) * 0.55f;
                box(poseStack.last(), vc, -hw, bodyBottom, -hl, hw, bedTop, midBack, paint, light);
                box(poseStack.last(), vc, -hw, bodyBottom, midBack - 0.03f, hw, bedTop, midBack + 0.03f, DARK, light);
            } else if (!wedge) {
                box(poseStack.last(), vc, -hw, bodyBottom, -hl, hw, bodyTop, midBack + 0.02f, paint, light);
            }
        }

        if (hoodBulge) {
            float bz0 = Math.min(hl - 0.1f, 0.0f);
            box(poseStack.last(), vc, -hw * 0.12f, bodyTop, bz0, hw * 0.12f, bodyTop + 0.05f, hl - 0.05f, paint, light);
        }
        if (grilleSlats) {
            for (int i = -2; i <= 2; i++) {
                float sx = i * hw * 0.16f;
                box(poseStack.last(), vc, sx - hw * 0.03f, bodyBottom + 0.16f, hl - 0.02f,
                        sx + hw * 0.03f, bodyBottom + 0.32f, hl + 0.03f, DARK, light);
            }
        }
        if (frontBadge) {
            box(poseStack.last(), vc, -hw * 0.08f, bodyBottom + 0.26f, hl - 0.01f,
                    hw * 0.08f, bodyBottom + 0.42f, hl + 0.04f, RIM, light);
        }

        // bumpers
        box(poseStack.last(), vc, -hw, bodyBottom, hl, hw, bodyBottom + 0.14f, hl + 0.05f, DARK, light);
        box(poseStack.last(), vc, -hw, bodyBottom, -hl - 0.05f, hw, bodyBottom + 0.14f, -hl, DARK, light);
        if (!grilleSlats) {
            box(poseStack.last(), vc, -hw * 0.35f, bodyBottom + 0.16f, hl, hw * 0.35f, bodyBottom + 0.30f, hl + 0.03f, DARK, light);
        }

        // lights (always bright)
        int bright = LightTexture.FULL_BRIGHT;
        if (roundLights) {
            float cx = hw - 0.34f;
            float cy = bodyBottom + (bodyTop - bodyBottom) * 0.55f;
            roundLight(poseStack.last(), vc, cx, cy, hl, HEADLIGHT, bright);
            roundLight(poseStack.last(), vc, -cx, cy, hl, HEADLIGHT, bright);
            roundLight(poseStack.last(), vc, cx, cy, -hl, TAILLIGHT, bright);
            roundLight(poseStack.last(), vc, -cx, cy, -hl, TAILLIGHT, bright);
        } else {
            float lx = hw - 0.32f;
            float ly0 = bodyTop - 0.20f;
            float ly1 = bodyTop - 0.08f;
            box(poseStack.last(), vc, lx - 0.17f, ly0, hl - 0.01f, lx + 0.17f, ly1, hl + 0.04f, HEADLIGHT, bright);
            box(poseStack.last(), vc, -lx - 0.17f, ly0, hl - 0.01f, -lx + 0.17f, ly1, hl + 0.04f, HEADLIGHT, bright);
            box(poseStack.last(), vc, lx - 0.17f, ly0, -hl - 0.04f, lx + 0.17f, ly1, -hl + 0.01f, TAILLIGHT, bright);
            box(poseStack.last(), vc, -lx - 0.17f, ly0, -hl - 0.04f, -lx + 0.17f, ly1, -hl + 0.01f, TAILLIGHT, bright);
        }

        // cabin: glass block + roof + pillars
        float cz = b.cabinZ;
        float c0 = cz - b.cabinLength / 2.0f;
        float c1 = cz + b.cabinLength / 2.0f;
        float cabTop = bodyTop + b.cabinHeight;
        float cw = hw - 0.12f;
        box(poseStack.last(), vc, -cw, bodyTop, c0, cw, cabTop, c1, GLASS, light);
        if (domedRoof) {
            float step = (cabTop - bodyTop) * 0.4f;
            box(poseStack.last(), vc, -cw + 0.05f, cabTop - step, c0 + 0.05f, cw - 0.05f, cabTop, c1 - 0.05f, paint, light);
            box(poseStack.last(), vc, -cw - 0.02f, bodyTop + step * 0.3f, c0 - 0.02f, cw + 0.02f, bodyTop + step * 0.5f, c1 + 0.02f, paint, light);
        } else {
            box(poseStack.last(), vc, -cw - 0.02f, cabTop - 0.07f, c0 - 0.04f, cw + 0.02f, cabTop, c1 + 0.04f, paint, light);
        }
        float p = 0.08f;
        for (int sx = -1; sx <= 1; sx += 2) {
            float x0 = sx > 0 ? cw - p : -cw;
            float x1 = sx > 0 ? cw : -cw + p;
            box(poseStack.last(), vc, x0, bodyTop, c0, x1, cabTop, c0 + p, paint, light);
            box(poseStack.last(), vc, x0, bodyTop, c1 - p, x1, cabTop, c1, paint, light);
        }
        if (boxy) {
            // flat, square-ish fender flares along both sides, like a body-on-frame off-roader
            box(poseStack.last(), vc, -hw - 0.03f, bodyBottom, -hl * 0.3f, -hw, bodyBottom + 0.22f, hl * 0.3f, DARK, light);
            box(poseStack.last(), vc, hw, bodyBottom, -hl * 0.3f, hw + 0.03f, bodyBottom + 0.22f, hl * 0.3f, DARK, light);
        }

        // rear wing
        if (b.spoiler) {
            float wz = -hl + 0.2f;
            box(poseStack.last(), vc, -hw * 0.6f - 0.04f, bodyTop, wz, -hw * 0.6f + 0.04f, bodyTop + 0.22f, wz + 0.08f, DARK, light);
            box(poseStack.last(), vc, hw * 0.6f - 0.04f, bodyTop, wz, hw * 0.6f + 0.04f, bodyTop + 0.22f, wz + 0.08f, DARK, light);
            box(poseStack.last(), vc, -hw * 0.85f, bodyTop + 0.22f, wz - 0.12f, hw * 0.85f, bodyTop + 0.27f, wz + 0.36f, DARK, light);
        }

        // wheels
        float angle = Mth.lerp(partialTick, car.wheelAngleO, car.wheelAngle);
        float r = b.wheelRadius;
        float wx = hw - 0.10f;
        float wzFront = hl - 0.2f - r;
        float wzRear = -hl + 0.2f + r;
        wheel(poseStack, vc, wx, r, wzFront, r, angle, light);
        wheel(poseStack, vc, -wx, r, wzFront, r, angle, light);
        wheel(poseStack, vc, wx, r, wzRear, r, angle, light);
        wheel(poseStack, vc, -wx, r, wzRear, r, angle, light);

        poseStack.popPose();
    }

    /** A small centred square light with a thin bright ring, reading as "round" from a distance. */
    private static void roundLight(PoseStack.Pose pose, VertexConsumer vc, float cx, float cy, float z,
                                   int rgb, int light) {
        float outer = 0.16f;
        float inner = 0.09f;
        box(pose, vc, cx - outer, cy - outer, z - 0.02f, cx + outer, cy + outer, z + 0.03f, RIM, light);
        box(pose, vc, cx - inner, cy - inner, z - 0.01f, cx + inner, cy + inner, z + 0.04f, rgb, light);
    }

    private static void wheel(PoseStack ps, VertexConsumer vc, float cx, float cy, float cz,
                              float radius, float angle, int light) {
        float a = radius * 0.72f;
        float t = 0.12f;
        for (int k = 0; k < 2; k++) {
            ps.pushPose();
            ps.translate(cx, cy, cz);
            ps.mulPose(Axis.XP.rotationDegrees(angle + k * 45.0f));
            box(ps.last(), vc, -t, -a, -a, t, a, a, TIRE, light);
            ps.popPose();
        }
        // hub with a visible stripe so the rotation can be seen
        ps.pushPose();
        ps.translate(cx, cy, cz);
        ps.mulPose(Axis.XP.rotationDegrees(angle));
        float h = radius * 0.36f;
        float side = cx > 0 ? 0.0f : -0.02f;
        box(ps.last(), vc, -t - 0.02f + side, -h, -h * 0.35f, t + 0.02f + side, h, h * 0.35f, RIM, light);
        ps.popPose();
    }

    private static void box(PoseStack.Pose pose, VertexConsumer vc,
                            float x0, float y0, float z0, float x1, float y1, float z1,
                            int rgb, int light) {
        quad(pose, vc, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0, 0, 1, 0, rgb, light);
        quad(pose, vc, x0, y0, z1, x0, y0, z0, x1, y0, z0, x1, y0, z1, 0, -1, 0, rgb, light);
        quad(pose, vc, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, 0, 0, 1, rgb, light);
        quad(pose, vc, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, 0, 0, -1, rgb, light);
        quad(pose, vc, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, 1, 0, 0, rgb, light);
        quad(pose, vc, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, -1, 0, 0, rgb, light);
    }

    private static void quad(PoseStack.Pose pose, VertexConsumer vc,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz,
                             float nx, float ny, float nz, int rgb, int light) {
        vertex(pose, vc, ax, ay, az, 0.0f, 0.0f, nx, ny, nz, rgb, light);
        vertex(pose, vc, bx, by, bz, 0.0f, 1.0f, nx, ny, nz, rgb, light);
        vertex(pose, vc, cx, cy, cz, 1.0f, 1.0f, nx, ny, nz, rgb, light);
        vertex(pose, vc, dx, dy, dz, 1.0f, 0.0f, nx, ny, nz, rgb, light);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer vc, float x, float y, float z,
                               float u, float v, float nx, float ny, float nz, int rgb, int light) {
        vc.addVertex(pose, x, y, z)
                .setColor((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, nx, ny, nz);
    }
}
