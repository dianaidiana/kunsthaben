package io.everyonecodes.project_module.classification.enums;

import lombok.Getter;

@Getter
public enum MediaEnum {

    OIL("Oil", "MED_PAINT_OIL"),
    ACRYLIC("Acrylic", "MED_PAINT_ACRYLIC"),
    WATERCOLOR("Watercolor", "MED_PAINT_WATERCOLOR"),
    GOUACHE("Gouache", "MED_PAINT_GOUACHE"),
    MIXED_MEDIA("Mixed media", "MED_PAINT_MIXED_MEDIA"),
    CHARCOAL("Charcoal", "MED_DRAW_CHARCOAL"),
    GRAPHITE("Graphite", "MED_DRAW_GRAPHITE"),
    INK("Ink", "MED_DRAW_INK"),
    PASTEL("Pastel", "MED_DRAW_PASTEL"),
    OIL_PASTEL("Oil pastel", "MED_DRAW_OIL_PASTEL");

    private final String name;
    private final String code;

    MediaEnum(String name, String code) {
        this.name = name;
        this.code = code;
    }
}