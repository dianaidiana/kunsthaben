package io.everyonecodes.project_module.exceptions;

public final class ErrorMessages {

    private ErrorMessages() {
    }

    public static final String CATEGORY_NOT_FOUND = "Category not found";
    public static final String MEDIA_NOT_FOUND = "Media not found";
    public static final String SUPPORT_NOT_FOUND = "Support not found";

    public static final String NAME_REQUIRED = "name should not be empty";
    public static final String CODE_REQUIRED = "code should not be empty";
    public static final String EMAIL_INVALID = "invalid email";
    public static final String PASSWORD_REQUIRED = "password should not be empty";
    public static final String PASSWORD_HASH_REQUIRED = "password hash should not be empty";
    public static final String CITY_REQUIRED = "city should not be empty";
    public static final String POSTCODE_REQUIRED = "postcode should not be empty";
    public static final String BANNER_URL_INVALID = "banner URL must be valid";
    public static final String AVATAR_URL_INVALID = "avatar URL must be valid";

    public static final String FRAME_DIMENSIONS_INVALID = "Frame dimensions must be positive";
    public static final String DIMENSIONS_INVALID = "Dimensions must be positive";
}