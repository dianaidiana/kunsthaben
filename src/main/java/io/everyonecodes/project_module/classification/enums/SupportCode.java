package io.everyonecodes.project_module.classification.enums;

import lombok.Getter;

@Getter
public enum SupportCode {

    CANVAS("SUP_PAINT_CANVAS"),
    WOOD_PANEL("SUP_PAINT_WOOD"),
    LINEN("SUP_PAINT_LINEN"),
    PAPER_PAINTING("SUP_PAINT_PAPER"),
    PAPER_DRAWING("SUP_DRAW_PAPER"),
    CARDBOARD("SUP_DRAW_CARDBOARD"),
    VELLUM("SUP_DRAW_VELLUM");

    private final String code;

    SupportCode(String code) {
        this.code = code;
    }
}