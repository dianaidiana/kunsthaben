package io.everyonecodes.project_module.classification.enums;

import lombok.Getter;

@Getter
public enum CategoryEnum {

    PAINTING("Painting", "CAT_PAINTING"),
    DRAWING("Drawing", "CAT_DRAWING");

    private final String name;
    private final String code;

    CategoryEnum(String name, String code) {
        this.name = name;
        this.code = code;
    }
}