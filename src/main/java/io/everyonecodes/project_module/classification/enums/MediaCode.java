package io.everyonecodes.project_module.classification.enums;

import lombok.Getter;

@Getter
public enum MediaCode {

    OIL("MED_PAINT_OIL"),
    ACRYLIC("MED_PAINT_ACRYLIC"),
    WATERCOLOR("MED_PAINT_WATERCOLOR"),
    GOUACHE("MED_PAINT_GOUACHE"),
    MIXED_MEDIA("MED_PAINT_MIXED_MEDIA"),
    CHARCOAL("MED_DRAW_CHARCOAL"),
    GRAPHITE("MED_DRAW_GRAPHITE"),
    INK("MED_DRAW_INK"),
    PASTEL("MED_DRAW_PASTEL"),
    OIL_PASTEL("MED_DRAW_OIL_PASTEL");

    private final String code;

    MediaCode(String code) {
        this.code = code;
    }
}