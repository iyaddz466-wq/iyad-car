package com.carmod;

import java.util.Map;

/**
 * Hand-tuned silhouette tweaks for a handful of well-known cars, layered on top of the
 * generic per-bodytype shape in CarRenderer. Any car id not listed here just keeps the
 * plain generic shape.
 */
public final class CarAccents {
    private CarAccents() {
    }

    /**
     * @param wedge       nose steps down toward the front (low-slung supercar look)
     * @param slopedTail  body steps down toward the back (fastback / ducktail look)
     * @param boxy        perfectly flat, upright front and rear (off-roader look)
     * @param bed         rear third sits lower than the cabin, like a pickup bed
     * @param hoodBulge   raised strip down the centre of the hood (muscle-car look)
     * @param domedRoof   roof narrows in two steps instead of one flat cap (rounded retro look)
     * @param roundLights square-ish, centred lights instead of the wide default strip
     * @param grilleSlats a few dark vertical slats on the nose, between bumper and hood
     * @param frontBadge  a small round badge centred on the nose (old van / classic look)
     */
    public record Accent(boolean wedge, boolean slopedTail, boolean boxy, boolean bed, boolean hoodBulge,
                          boolean domedRoof, boolean roundLights, boolean grilleSlats, boolean frontBadge) {
    }

    private static Accent of(String flags) {
        return new Accent(
                flags.contains("wedge"), flags.contains("tail"), flags.contains("boxy"),
                flags.contains("bed"), flags.contains("bulge"), flags.contains("dome"),
                flags.contains("round"), flags.contains("slats"), flags.contains("badge"));
    }

    private static final Map<String, Accent> BY_ID = Map.ofEntries(
            Map.entry("toyota_supra", of("wedge slats")),
            Map.entry("nissan_gt_r", of("round")),
            Map.entry("porsche_911_carrera", of("tail round")),
            Map.entry("porsche_911_gt3_rs", of("tail round")),
            Map.entry("chevrolet_corvette_c8", of("wedge tail")),
            Map.entry("chevrolet_bel_air_1957", of("dome round")),
            Map.entry("ford_mustang", of("bulge round")),
            Map.entry("ford_f_150_raptor", of("bed boxy")),
            Map.entry("ford_model_t", of("dome round boxy")),
            Map.entry("dodge_charger_hellcat", of("bulge round")),
            Map.entry("bmw_m4", of("wedge slats")),
            Map.entry("mercedes_benz_g63_amg", of("boxy round slats")),
            Map.entry("jeep_wrangler_rubicon", of("boxy round slats")),
            Map.entry("land_rover_defender", of("boxy round slats")),
            Map.entry("volkswagen_beetle", of("dome round badge")),
            Map.entry("volkswagen_transporter_t1", of("boxy round badge")),
            Map.entry("mini_cooper_s", of("dome round")),
            Map.entry("ferrari_f40", of("wedge tail slats")),
            Map.entry("lamborghini_aventador", of("wedge tail slats")),
            Map.entry("bugatti_chiron", of("wedge round")),
            Map.entry("mclaren_p1", of("wedge tail")),
            Map.entry("tesla_cybertruck", of("bed boxy")),
            Map.entry("hummer_h1", of("boxy round slats"))
    );

    public static Accent get(String carId) {
        return BY_ID.get(carId);
    }
}
