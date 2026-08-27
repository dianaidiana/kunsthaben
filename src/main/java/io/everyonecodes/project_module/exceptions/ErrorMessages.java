package io.everyonecodes.project_module.exceptions;

public final class ErrorMessages {

    private ErrorMessages() {
    }

    public static final String CATEGORY_NOT_FOUND = "Category not found";
    public static final String MEDIA_NOT_FOUND = "Media not found";
    public static final String SUPPORT_NOT_FOUND = "Support not found";
    public static final String USER_NOT_FOUND = "User not found";
    public static final String ARTWORK_NOT_FOUND = "Artwork not found";

    public static final String NOT_ARTWORK_OWNER = "You do not have permission to modify this artwork";
    public static final String NOT_PROFILE_OWNER = "You do not have permission to modify this user's profile";

    public static final String TOO_MANY_IMAGES = "The number of images has reached a limit";
    public static final String IMAGES_DO_NOT_MATCH = "Can't reorder images as set of images don't match";
    public static final String IMAGE_NOT_FOUND = "Image not found";

    public static final String EMAIL_ALREADY_TAKEN = "email is already taken";

    public static final String NAME_REQUIRED = "name should not be empty";
    public static final String CODE_REQUIRED = "code should not be empty";
    public static final String EMAIL_REQUIRED = "email should not be empty";
    public static final String EMAIL_INVALID = "invalid email";
    public static final String PASSWORD_REQUIRED = "password should not be empty";
    public static final String PASSWORD_HASH_REQUIRED = "password hash should not be empty";
    public static final String CITY_REQUIRED = "city should not be empty";
    public static final String POSTCODE_REQUIRED = "postcode should not be empty";
    public static final String BANNER_URL_INVALID = "banner URL must be valid";
    public static final String AVATAR_URL_INVALID = "avatar URL must be valid";
    public static final String CATEGORY_REQUIRED = "category is required";

    public static final String TITLE_REQUIRED = "title should not be empty";
    public static final String PRICE_INVALID = "price must be positive";
    public static final String YEAR_INVALID = "year must be positive";
    public static final String DESCRIPTION_REQUIRED = "description should not be empty";
    public static final String URL_REQUIRED = "url should not be empty";
    public static final String SORT_ORDER_INVALID = "sort order must be zero or positive";
    public static final String INVALID_IMAGE_FILE = "The provided image file is invalid";

    public static final String FRAME_DIMENSIONS_INVALID = "Frame dimensions must be positive";
    public static final String DIMENSIONS_INVALID = "Dimensions must be positive";

    public static final String SELF_FOLLOW = "Self-following is not permitted";
    public static final String DUPLICATE_FOLLOW = "Follow already exists";

    public static final String INVALID_CREDENTIALS = "Invalid credentials";

    public static final String CONTENT_REQUIRED = "Message content can't be empty";
    public static final String MAX_CHARACTERS = "The message exceeds the maximum of characters allowed";

    public static final String CHAT_NOT_FOUND = "Chat not found";
    public static final String NOT_CHAT_PARTICIPANT = "You do not have permission to access this chat";
    public static final String CANNOT_CHAT_WITH_SELF = "You can't chat with yourself";
    public static final String NO_MESSAGES = "This chat has no messages";
}