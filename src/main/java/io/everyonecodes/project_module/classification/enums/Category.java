package io.everyonecodes.project_module.enums;

import lombok.Getter;

@Getter
public enum Category {

    PAINTING("CAT_PAINTING"),
    DRAWING("CAT_DRAWING");

    private final String code;

    Category(String code) {
        this.code = code;
    }
}