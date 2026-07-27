package io.everyonecodes.project_module.classification.enums;

import lombok.Getter;

@Getter
public enum SupportEnum {

    CANVAS("Canvas", "SUP_PAINT_CANVAS"),
    WOOD_PANEL("Wood Panel", "SUP_PAINT_WOOD"),
    LINEN("Linen", "SUP_PAINT_LINEN"),
    PAPER_PAINTING("Paper", "SUP_PAINT_PAPER"),
    PAPER_DRAWING("Paper", "SUP_DRAW_PAPER"),
    CARDBOARD("Cardboard", "SUP_DRAW_CARDBOARD"),
    VELLUM("Vellum", "SUP_DRAW_VELLUM");

    private final String name;
    private final String code;

    SupportEnum(String name, String code) {
        this.name = name;
        this.code = code;
    }
}