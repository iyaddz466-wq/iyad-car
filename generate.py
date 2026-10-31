#!/usr/bin/env python3
"""Generates CarCatalog.java, item models and item icons from the list below.
Run from the project root:  python3 tools/generate.py
Add a car = add "Model/BODY/color[/topSpeedBlocksPerTick]" to a brand line, then re-run.
"""
import json, os, re, struct, unicodedata, zlib

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

COLORS = {
    "white": 0xF2F2F2, "silver": 0xB9BEC4, "grey": 0x6E7378, "black": 0x1C1C1E,
    "red": 0xC4161C, "darkred": 0x7A1018, "orange": 0xE8601C, "yellow": 0xF2C80F,
    "green": 0x2E7D32, "darkgreen": 0x1E3A2B, "lime": 0x8BC34A, "teal": 0x1F8A8A,
    "blue": 0x1F4FB5, "lightblue": 0x4AA3E0, "navy": 0x14284B, "purple": 0x6A2C91,
    "pink": 0xE46FA5, "brown": 0x6B4423, "beige": 0xD8C9A3, "gold": 0xC9A227,
    "bronze": 0x8C6A3B,
}

# Brand -> comma separated "Model/BODY/color[/top]"
BRANDS = [
    ("Toyota", "Camry/SEDAN/silver, Corolla/SEDAN/white, Yaris/HATCH/lightblue, Prius/HATCH/white, GR86/SPORT/blue, Supra/SPORT/orange/1.9, Land Cruiser/OFFROAD/darkgreen, Land Cruiser Prado/SUV/white, Hilux/PICKUP/red, RAV4/SUV/grey, Fortuner/SUV/black, Avalon/LUXURY/darkred"),
    ("Honda", "Civic/SEDAN/white, Civic Type R/SPORT/white/1.85, Accord/SEDAN/grey, CR-V/SUV/blue, Jazz/HATCH/yellow, S2000/SPORT/yellow, NSX/SUPER/red, Pilot/SUV/black"),
    ("Nissan", "Altima/SEDAN/silver, Sunny/SEDAN/white, 370Z/SPORT/orange, GT-R/SUPER/white/2.1, Skyline R34/SPORT/blue/1.9, Patrol/SUV/white, Patrol Nismo/SUV/black, Navara/PICKUP/grey, Leaf/HATCH/lightblue, Maxima/SEDAN/darkred"),
    ("Mazda", "Mazda3/HATCH/red, Mazda6/SEDAN/grey, MX-5/SPORT/red, RX-7/SPORT/yellow, CX-5/SUV/darkred, CX-9/SUV/navy"),
    ("Subaru", "Impreza WRX/SPORT/blue, BRZ/SPORT/orange, Forester/SUV/green, Outback/SUV/grey"),
    ("Mitsubishi", "Lancer Evolution/SPORT/white, Pajero/OFFROAD/silver, Outlander/SUV/black, L200/PICKUP/white, Eclipse/SPORT/purple"),
    ("Suzuki", "Swift/HATCH/yellow, Jimny/OFFROAD/green, Vitara/SUV/red, Alto/HATCH/white"),
    ("Isuzu", "D-Max/PICKUP/grey, MU-X/SUV/white"),
    ("Lexus", "IS/SEDAN/silver, ES/LUXURY/white, LS/LUXURY/black, LC 500/SPORT/orange, LFA/SUPER/white/2.2, RX/SUV/grey, LX 570/SUV/white, GX/OFFROAD/black"),
    ("Infiniti", "Q50/SEDAN/darkred, Q60/SPORT/blue, QX80/SUV/black"),
    ("Acura", "TLX/SEDAN/silver, Integra/HATCH/red, NSX/SUPER/blue"),
    ("Hyundai", "Elantra/SEDAN/white, Sonata/SEDAN/grey, i20/HATCH/red, Veloster N/SPORT/lightblue, Ioniq 5/HATCH/silver, Tucson/SUV/white, Santa Fe/SUV/black, Palisade/SUV/darkgreen, Accent/SEDAN/silver"),
    ("Kia", "Rio/HATCH/white, Cerato/SEDAN/grey, K5/SEDAN/blue, Stinger/SPORT/red, Sportage/SUV/orange, Sorento/SUV/black, Telluride/SUV/darkgreen, EV6/HATCH/silver, Carnival/VAN/white"),
    ("Genesis", "G70/SEDAN/blue, G80/LUXURY/black, G90/LUXURY/white, GV70/SUV/grey, GV80/SUV/darkgreen"),
    ("BYD", "Seal/SEDAN/blue, Han/LUXURY/darkred, Tang/SUV/white, Atto 3/SUV/teal, Yangwang U9/SUPER/black/2.3"),
    ("Geely", "Emgrand/SEDAN/silver, Coolray/SUV/orange, Monjaro/SUV/grey"),
    ("Zeekr", "001/SPORT/darkgreen, 009/VAN/black"),
    ("Chery", "Arrizo 6/SEDAN/white, Tiggo 7/SUV/red, Tiggo 8/SUV/navy"),
    ("Haval", "H6/SUV/grey, Jolion/SUV/white"),
    ("Tank", "300/OFFROAD/orange, 500/OFFROAD/black"),
    ("Changan", "CS75/SUV/blue, Eado/SEDAN/silver, UNI-T/SUV/purple"),
    ("Hongqi", "H5/LUXURY/black, H9/LUXURY/darkred, E-HS9/SUV/white"),
    ("MG", "MG5/SEDAN/red, ZS/SUV/white, HS/SUV/grey, Cyberster/SPORT/orange"),
    ("NIO", "ET5/SEDAN/silver, ET7/LUXURY/blue, ES8/SUV/white, EP9/SUPER/red/2.2"),
    ("Xpeng", "P7/SEDAN/white, G9/SUV/grey"),
    ("Li Auto", "L9/SUV/black, L7/SUV/silver"),
    ("Xiaomi", "SU7/SPORT/lightblue/1.8, YU7/SUV/green"),
    ("Mahindra", "Thar/OFFROAD/red, Scorpio/SUV/black, XUV700/SUV/silver"),
    ("Tata", "Nexon/SUV/blue, Harrier/SUV/grey, Safari/SUV/white"),
    ("Proton", "Saga/SEDAN/silver, X70/SUV/red"),
    ("Mercedes-Benz", "A-Class/HATCH/silver, C-Class/SEDAN/black, E-Class/LUXURY/white, S-Class/LUXURY/black, CLS/SEDAN/darkred, GLE/SUV/grey, GLS/SUV/black, G-Class/OFFROAD/black, G63 AMG/OFFROAD/darkgreen/1.4, AMG GT/SPORT/silver/1.95, SLS AMG/SUPER/silver/2.1, AMG One/HYPER/white, 300 SL/CLASSIC/silver"),
    ("Maybach", "S 680/LUXURY/black, GLS 600/SUV/grey"),
    ("BMW", "1 Series/HATCH/blue, 3 Series/SEDAN/white, 5 Series/LUXURY/grey, 7 Series/LUXURY/black, M2/SPORT/orange, M3/SPORT/green/1.85, M4/SPORT/yellow/1.85, M5/SPORT/blue/1.9, Z4/SPORT/red, i8/SUPER/white/2.0, X3/SUV/silver, X5/SUV/black, X6/SUV/white, X7/SUV/navy, M1/SUPER/white"),
    ("Audi", "A3/HATCH/red, A4/SEDAN/grey, A6/LUXURY/black, A8/LUXURY/silver, RS3/HATCH/lime, RS6 Avant/SPORT/grey/1.9, RS7/SPORT/black/1.9, TT/SPORT/silver, R8/SUPER/yellow, e-tron GT/SPORT/darkgreen, Q5/SUV/white, Q7/SUV/black, Q8/SUV/navy"),
    ("Volkswagen", "Polo/HATCH/white, Golf GTI/HATCH/red, Golf R/HATCH/blue, Jetta/SEDAN/silver, Passat/SEDAN/grey, Scirocco/SPORT/black, Beetle/CLASSIC/yellow, Transporter T1/VAN/teal, Tiguan/SUV/white, Touareg/SUV/black, ID.4/SUV/lightblue, Arteon/LUXURY/darkred"),
    ("Porsche", "718 Cayman/SPORT/yellow, 911 Carrera/SPORT/silver, 911 GT3 RS/SUPER/orange, 911 Turbo S/SUPER/white, Taycan/SPORT/blue, Panamera/LUXURY/black, Macan/SUV/grey, Cayenne/SUV/darkgreen, Carrera GT/SUPER/silver/2.2, 918 Spyder/HYPER/white"),
    ("Opel", "Corsa/HATCH/red, Astra/HATCH/blue, Insignia/SEDAN/grey"),
    ("Skoda", "Fabia/HATCH/white, Octavia/SEDAN/green, Superb/LUXURY/black, Kodiaq/SUV/silver"),
    ("SEAT", "Ibiza/HATCH/red, Leon/HATCH/blue"),
    ("Cupra", "Formentor/SUV/bronze, Born/HATCH/teal"),
    ("Mini", "Cooper/HATCH/red, Cooper S/HATCH/green, Countryman/SUV/orange"),
    ("Smart", "Fortwo/HATCH/yellow"),
    ("Ford", "Fiesta/HATCH/blue, Focus RS/SPORT/lightblue, Mondeo/SEDAN/grey, Mustang/MUSCLE/yellow, Mustang Shelby GT500/MUSCLE/black/1.9, Mustang Boss 302/MUSCLE/orange, GT/SUPER/blue/2.3, Explorer/SUV/white, Bronco/OFFROAD/orange, Ranger/PICKUP/red, F-150/PICKUP/blue, F-150 Raptor/PICKUP/grey/1.45, Crown Victoria/SEDAN/black, Model T/CLASSIC/black"),
    ("Chevrolet", "Spark/HATCH/yellow, Malibu/SEDAN/silver, Impala/LUXURY/black, Camaro SS/MUSCLE/yellow, Camaro ZL1/MUSCLE/red/1.85, Corvette C8/SUPER/red/2.2, Corvette C3/SPORT/blue, Silverado/PICKUP/white, Tahoe/SUV/black, Suburban/SUV/silver, Bel Air 1957/CLASSIC/teal"),
    ("Dodge", "Charger/MUSCLE/black, Charger Hellcat/MUSCLE/orange/1.9, Challenger/MUSCLE/purple, Viper/SUPER/red/2.1, Durango/SUV/grey, Ram 1500/PICKUP/darkred"),
    ("Jeep", "Wrangler/OFFROAD/green, Wrangler Rubicon/OFFROAD/yellow, Gladiator/PICKUP/orange, Cherokee/SUV/silver, Grand Cherokee/SUV/black, Grand Cherokee Trackhawk/SUV/darkred/1.6"),
    ("Chrysler", "300C/LUXURY/black, Pacifica/VAN/silver"),
    ("Cadillac", "CTS-V/SPORT/black/1.8, CT5-V/SPORT/silver, Escalade/SUV/black, Eldorado/CLASSIC/pink"),
    ("GMC", "Sierra/PICKUP/red, Yukon/SUV/white, Hummer EV/OFFROAD/grey"),
    ("Hummer", "H1/OFFROAD/darkgreen, H2/OFFROAD/yellow"),
    ("Lincoln", "Continental/LUXURY/black, Navigator/SUV/grey"),
    ("Tesla", "Model 3/SEDAN/white, Model S/SEDAN/red, Model S Plaid/SPORT/grey/1.9, Model X/SUV/silver, Model Y/SUV/blue, Cybertruck/PICKUP/silver/1.4, Roadster/SUPER/red/2.3"),
    ("Rivian", "R1T/PICKUP/green, R1S/SUV/blue"),
    ("Lucid", "Air/LUXURY/white, Air Sapphire/SPORT/black/1.9"),
    ("Polestar", "1/SPORT/white, 2/HATCH/grey, 3/SUV/darkgreen"),
    ("Ferrari", "Roma/SPORT/red/1.95, 458 Italia/SUPER/red, 488 GTB/SUPER/red, F8 Tributo/SUPER/yellow, 812 Superfast/SUPER/red, Purosangue/SUV/darkred/1.9, F40/SUPER/red, Enzo/SUPER/red, Testarossa/SUPER/white, LaFerrari/HYPER/red, SF90 Stradale/HYPER/red, Daytona SP3/HYPER/red, 250 GTO/SPORT/red"),
    ("Lamborghini", "Gallardo/SUPER/lime, Huracan/SUPER/orange, Aventador/SUPER/yellow, Murcielago/SUPER/orange, Diablo/SUPER/yellow, Countach/SUPER/white, Urus/SUV/yellow/1.9, Veneno/HYPER/white, Sian/HYPER/green, Revuelto/HYPER/orange"),
    ("Maserati", "Ghibli/LUXURY/blue, Quattroporte/LUXURY/black, Levante/SUV/grey, GranTurismo/SPORT/silver, MC20/SUPER/lightblue"),
    ("Alfa Romeo", "Giulia/SEDAN/red, Giulia Quadrifoglio/SPORT/darkgreen/1.85, Stelvio/SUV/white, 4C/SPORT/red, 8C/SUPER/red"),
    ("Fiat", "500/HATCH/white, Panda/HATCH/green, Tipo/SEDAN/grey, 124 Spider/SPORT/red"),
    ("Lancia", "Delta Integrale/HATCH/white, Stratos/SPORT/orange"),
    ("Pagani", "Zonda/HYPER/silver, Huayra/HYPER/black, Utopia/HYPER/grey"),
    ("Bugatti", "Veyron/HYPER/blue/3.0, Chiron/HYPER/blue/3.1, Divo/HYPER/lightblue/3.0, Bolide/HYPER/orange/3.1, EB110/SUPER/yellow"),
    ("Koenigsegg", "Agera RS/HYPER/white, Regera/HYPER/black, Jesko/HYPER/orange, Gemera/HYPER/grey"),
    ("McLaren", "F1/SUPER/silver/2.3, 570S/SUPER/orange, 720S/SUPER/orange/2.3, Artura/SUPER/darkgreen, P1/HYPER/orange, Senna/HYPER/black, Speedtail/HYPER/grey"),
    ("Aston Martin", "DB5/SPORT/silver, Vantage/SPORT/green, DB11/SPORT/blue, Vanquish/SUPER/black, One-77/SUPER/silver, DBX/SUV/darkgreen, Valkyrie/HYPER/white"),
    ("Bentley", "Continental GT/LUXURY/blue/1.7, Flying Spur/LUXURY/black, Mulsanne/LUXURY/darkgreen, Bentayga/SUV/white"),
    ("Rolls-Royce", "Ghost/LUXURY/white, Phantom/LUXURY/black, Wraith/LUXURY/silver, Spectre/LUXURY/grey, Cullinan/SUV/darkred"),
    ("Jaguar", "XE/SEDAN/blue, XJ/LUXURY/black, F-Type/SPORT/green, E-Type/CLASSIC/darkgreen, F-Pace/SUV/white"),
    ("Land Rover", "Defender/OFFROAD/darkgreen, Discovery/SUV/grey, Range Rover/SUV/black, Range Rover Sport/SUV/white, Evoque/SUV/orange"),
    ("Lotus", "Elise/SPORT/yellow, Exige/SPORT/orange, Esprit/SPORT/white, Emira/SPORT/green, Evija/HYPER/lime"),
    ("Peugeot", "205 GTI/HATCH/red, 206/HATCH/blue, 208/HATCH/yellow, 508/SEDAN/grey, 3008/SUV/black"),
    ("Renault", "5/CLASSIC/orange, Twingo/HATCH/lightblue, Clio/HATCH/yellow, Megane RS/SPORT/orange, Captur/SUV/red, Duster/SUV/grey"),
    ("Citroen", "2CV/CLASSIC/beige, C3/HATCH/orange, C4/HATCH/blue, C5 Aircross/SUV/white"),
    ("DS", "DS 7/SUV/black"),
    ("Alpine", "A110/SPORT/blue"),
    ("Volvo", "P1800/CLASSIC/red, 240/CLASSIC/beige, S60/SEDAN/silver, V60/SEDAN/blue, XC40/SUV/orange, XC60/SUV/white, XC90/SUV/black"),
    ("Dacia", "Sandero/HATCH/blue, Logan/SEDAN/silver, Duster/SUV/orange"),
    ("Lada", "2107/CLASSIC/red, Niva/OFFROAD/green, Granta/SEDAN/white, Vesta/SEDAN/grey"),
    ("UAZ", "Hunter/OFFROAD/green, Patriot/SUV/silver"),
    ("GAZ", "Volga/CLASSIC/black, Gazelle/VAN/white"),
    ("Iran Khodro", "Samand/SEDAN/white"),
    ("Saipa", "Pride/HATCH/white"),
    ("Rimac", "Nevera/HYPER/white/3.0"),
    ("SSC", "Tuatara/HYPER/orange/3.1"),
    ("Hennessey", "Venom F5/HYPER/black/3.0"),
    ("Zenvo", "TSR-S/HYPER/red"),
    ("Spyker", "C8/SUPER/blue"),
    ("W Motors", "Lykan HyperSport/HYPER/red/2.6"),
    ("Devel", "Sixteen/HYPER/black/2.8"),
]

BODY_ICON = {  # (body height px, cabin height px)
    "SEDAN": (3, 3), "HATCH": (3, 3), "SUV": (4, 3), "OFFROAD": (4, 3), "PICKUP": (4, 3),
    "SPORT": (2, 2), "MUSCLE": (3, 2), "SUPER": (2, 2), "HYPER": (2, 2),
    "LUXURY": (3, 3), "VAN": (4, 4), "CLASSIC": (3, 3),
}


def slug(text):
    t = unicodedata.normalize("NFKD", text).encode("ascii", "ignore").decode()
    return re.sub(r"[^a-z0-9]+", "_", t.lower()).strip("_")


def write_png(path, rows):
    h, w = len(rows), len(rows[0])
    raw = b"".join(b"\x00" + bytes(c for px in row for c in px) for row in rows)

    def chunk(tag, data):
        c = struct.pack(">I", len(data)) + tag + data
        return c + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)

    png = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0)) \
        + chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b"")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "wb") as f:
        f.write(png)


def icon(body, color):
    r, g, b = (color >> 16) & 255, (color >> 8) & 255, color & 255
    paint = (r, g, b, 255)
    dark = (max(r - 50, 0), max(g - 50, 0), max(b - 50, 0), 255)
    glass = (150, 190, 225, 255)
    tire = (25, 25, 25, 255)
    hub = (190, 190, 190, 255)
    none = (0, 0, 0, 0)
    px = [[none] * 16 for _ in range(16)]
    bh, ch = BODY_ICON[body]
    body_bottom = 11
    body_top = body_bottom - bh + 1
    for y in range(body_top, body_bottom + 1):
        for x in range(1, 15):
            px[y][x] = paint if y < body_bottom else dark
    c0, c1 = (3, 12) if body not in ("PICKUP",) else (3, 8)
    if body in ("VAN",):
        c0, c1 = 2, 13
    for y in range(body_top - ch, body_top):
        for x in range(c0, c1 + 1):
            px[y][x] = paint if y == body_top - ch else glass
    for cx in (4, 11):
        for y in range(10, 14):
            for x in range(cx - 1, cx + 2):
                px[y][x] = tire
        px[12][cx] = hub
    return px


def main():
    cars = []
    seen = set()
    for brand, entries in BRANDS:
        for entry in [e.strip() for e in entries.split(",") if e.strip()]:
            parts = entry.split("/")
            model, body, color = parts[0].strip(), parts[1].strip(), parts[2].strip()
            top = float(parts[3]) if len(parts) > 3 else -1.0
            assert body in BODY_ICON, (brand, entry)
            assert color in COLORS, (brand, entry)
            cid = slug(brand) + "_" + slug(model)
            assert cid not in seen, cid
            seen.add(cid)
            cars.append((cid, brand, model, body, COLORS[color], top))

    java = ["package com.carmod;", "", "import java.util.List;", "",
            "/** GENERATED by tools/generate.py - do not edit by hand. */",
            "public final class CarCatalog {",
            "    private CarCatalog() {}", "",
            "    public static final List<CarSpec> ALL = List.of("]
    lines = []
    for cid, brand, model, body, color, top in cars:
        lines.append('            new CarSpec("%s", "%s", "%s", BodyType.%s, 0x%06X, %s)'
                     % (cid, brand, model, body, color, repr(top)))
    java.append(",\n".join(lines))
    java.append("    );")
    java.append("}")
    java.append("")
    out = os.path.join(ROOT, "src/main/java/com/carmod/CarCatalog.java")
    with open(out, "w", encoding="utf-8") as f:
        f.write("\n".join(java))

    assets = os.path.join(ROOT, "src/main/resources/assets/carmod")
    for cid, brand, model, body, color, top in cars:
        os.makedirs(os.path.join(assets, "models/item"), exist_ok=True)
        with open(os.path.join(assets, "models/item/%s.json" % cid), "w") as f:
            json.dump({"parent": "minecraft:item/generated",
                       "textures": {"layer0": "carmod:item/%s" % cid}}, f)
        write_png(os.path.join(assets, "textures/item/%s.png" % cid), icon(body, color))

    write_png(os.path.join(assets, "textures/entity/white.png"),
              [[(255, 255, 255, 255)] * 16 for _ in range(16)])
    brands = len({c[1] for c in cars})
    print("generated %d cars from %d brands" % (len(cars), brands))


if __name__ == "__main__":
    main()
