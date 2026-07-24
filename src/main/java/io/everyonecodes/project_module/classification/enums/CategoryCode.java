package io.everyonecodes.project_module.classification.enums;

import lombok.Getter;

@Getter
public enum CategoryCode {

    PAINTING("CAT_PAINTING"),
    DRAWING("CAT_DRAWING");

    private final String code;

    CategoryCode(String code) {
        this.code = code;
    }
}