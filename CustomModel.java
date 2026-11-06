package com.carmod;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * A car shape loaded from a Blockbench "Java Block/Item" .json file (cubes with per-face UVs).
 * The model is auto-fitted: scaled so its length matches the car, centred, resting on the ground,
 * and rotated so the car's front (north in Blockbench) points forward.
 */
public final class CustomModel {
    public static final CustomModel NONE = new CustomModel(new float[0][], null);

    private static final String[] DIRS = {"north", "south", "east", "west", "up", "down"};
    private static final float[][] NORMALS = {
            {0, 0, -1}, {0, 0, 1}, {1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}
    };

    public final ResourceLocation texture;
    /** Each face: 12 position floats (TL,TR,BR,BL), 8 uv floats, 3 normal floats. */
    private final float[][] faces;

    private CustomModel(float[][] faces, ResourceLocation texture) {
        this.faces = faces;
        this.texture = texture;
    }

    public void render(PoseStack.Pose pose, VertexConsumer vc, int light) {
        for (float[] f : faces) {
            for (int i = 0; i < 4; i++) {
                vc.addVertex(pose, f[i * 3], f[i * 3 + 1], f[i * 3 + 2])
                        .setColor(255, 255, 255, 255)
                        .setUv(f[12 + i * 2], f[13 + i * 2])
                        .setOverlay(OverlayTexture.NO_OVERLAY)
                        .setLight(light)
                        .setNormal(pose, f[20], f[21], f[22]);
            }
        }
    }

    public static CustomModel parse(JsonObject root, String id, float targetLength) {
        JsonArray elements = root.has("elements") ? root.getAsJsonArray("elements") : null;
        if (elements == null) {
            return NONE;
        }
        List<float[]> out = new ArrayList<>();
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;

        for (JsonElement el : elements) {
            JsonObject e = el.getAsJsonObject();
            if (!e.has("faces") || !e.has("from") || !e.has("to")) {
                continue;
            }
            JsonObject faces = e.getAsJsonObject("faces");
            float[] from = floats(e.getAsJsonArray("from"));
            float[] to = floats(e.getAsJsonArray("to"));
            JsonObject rot = e.has("rotation") ? e.getAsJsonObject("rotation") : null;

            for (int d = 0; d < 6; d++) {
                String dir = DIRS[d];
                if (!faces.has(dir)) {
                    continue;
                }
                JsonObject f = faces.getAsJsonObject(dir);
                float[] data = new float[23];
                float[] c = corners(d, from, to);
                float[] n = NORMALS[d].clone();

                if (rot != null && rot.has("axis") && rot.has("angle")) {
                    float[] origin = rot.has("origin") ? floats(rot.getAsJsonArray("origin")) : new float[]{8, 8, 8};
                    String axis = rot.get("axis").getAsString();
                    float ang = (float) Math.toRadians(rot.get("angle").getAsFloat());
                    for (int i = 0; i < 4; i++) {
                        rotate(c, i * 3, axis, ang, origin);
                    }
                    rotate(n, 0, axis, ang, new float[]{0, 0, 0});
                }
                System.arraycopy(c, 0, data, 0, 12);

                float[] uv = f.has("uv") ? floats(f.getAsJsonArray("uv")) : new float[]{0, 0, 16, 16};
                float[][] corner = {{uv[0], uv[1]}, {uv[2], uv[1]}, {uv[2], uv[3]}, {uv[0], uv[3]}};
                int steps = f.has("rotation") ? ((f.get("rotation").getAsInt() / 90) & 3) : 0;
                for (int i = 0; i < 4; i++) {
                    float[] t = corner[(i + steps) & 3];
                    data[12 + i * 2] = t[0] / 16.0f;
                    data[13 + i * 2] = t[1] / 16.0f;
                }
                data[20] = n[0];
                data[21] = n[1];
                data[22] = n[2];
                out.add(data);

                for (int i = 0; i < 4; i++) {
                    minX = Math.min(minX, c[i * 3]);
                    maxX = Math.max(maxX, c[i * 3]);
                    minY = Math.min(minY, c[i * 3 + 1]);
                    minZ = Math.min(minZ, c[i * 3 + 2]);
                    maxZ = Math.max(maxZ, c[i * 3 + 2]);
                }
            }
        }

        float extentZ = maxZ - minZ;
        if (out.isEmpty() || extentZ < 0.001f) {
            return NONE;
        }
        float s = targetLength / extentZ;
        float cx = (minX + maxX) / 2.0f;
        float cz = (minZ + maxZ) / 2.0f;
        for (float[] data : out) {
            for (int i = 0; i < 4; i++) {
                float x = data[i * 3];
                float y = data[i * 3 + 1];
                float z = data[i * 3 + 2];
                // Blockbench's front is north (-Z); the car's front is +Z, so turn the model around.
                data[i * 3] = -(x - cx) * s;
                data[i * 3 + 1] = (y - minY) * s;
                data[i * 3 + 2] = -(z - cz) * s;
            }
            data[20] = -data[20];
            data[22] = -data[22];
        }
        return new CustomModel(out.toArray(new float[0][]), CarMod.id("textures/car/" + id + ".png"));
    }

    private static float[] floats(JsonArray array) {
        float[] r = new float[array.size()];
        for (int i = 0; i < r.length; i++) {
            r[i] = array.get(i).getAsFloat();
        }
        return r;
    }

    /** Four corners (top-left, top-right, bottom-right, bottom-left as seen from outside), like vanilla block models. */
    private static float[] corners(int dir, float[] from, float[] to) {
        float x0 = from[0], y0 = from[1], z0 = from[2];
        float x1 = to[0], y1 = to[1], z1 = to[2];
        switch (dir) {
            case 0: // north
                return new float[]{x1, y1, z0, x0, y1, z0, x0, y0, z0, x1, y0, z0};
            case 1: // south
                return new float[]{x0, y1, z1, x1, y1, z1, x1, y0, z1, x0, y0, z1};
            case 2: // east
                return new float[]{x1, y1, z1, x1, y1, z0, x1, y0, z0, x1, y0, z1};
            case 3: // west
                return new float[]{x0, y1, z0, x0, y1, z1, x0, y0, z1, x0, y0, z0};
            case 4: // up
                return new float[]{x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1};
            default: // down
                return new float[]{x0, y0, z1, x1, y0, z1, x1, y0, z0, x0, y0, z0};
        }
    }

    private static void rotate(float[] a, int o, String axis, float ang, float[] origin) {
        float x = a[o] - origin[0];
        float y = a[o + 1] - origin[1];
        float z = a[o + 2] - origin[2];
        float cos = (float) Math.cos(ang);
        float sin = (float) Math.sin(ang);
        float nx = x, ny = y, nz = z;
        switch (axis) {
            case "x":
                ny = y * cos - z * sin;
                nz = y * sin + z * cos;
                break;
            case "y":
                nx = x * cos + z * sin;
                nz = -x * sin + z * cos;
                break;
            default:
                nx = x * cos - y * sin;
                ny = x * sin + y * cos;
                break;
        }
        a[o] = nx + origin[0];
        a[o + 1] = ny + origin[1];
        a[o + 2] = nz + origin[2];
    }
}
