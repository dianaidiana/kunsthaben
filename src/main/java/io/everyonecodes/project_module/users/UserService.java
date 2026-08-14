package io.everyonecodes.project_module.users;

import io.everyonecodes.project_module.artworkimages.ArtworkImageService;
import io.everyonecodes.project_module.exceptions.BadRequestException;
import io.everyonecodes.project_module.exceptions.ConflictException;
import io.everyonecodes.project_module.exceptions.ErrorMessages;
import io.everyonecodes.project_module.exceptions.NotFoundException;
import io.everyonecodes.project_module.storage.S3StorageService;
import io.everyonecodes.project_module.users.dto.UserRegisterRequest;
import io.everyonecodes.project_module.users.dto.UserResponse;
import io.everyonecodes.project_module.users.dto.UserUpdateRequest;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.util.Optional;
import java.util.Set;

@Service
public class UserService {

    private final UserRepository repository;
    private final S3StorageService s3StorageService;
    private final ArtworkImageService artworkImageService;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    public UserService(UserRepository repository, S3StorageService s3StorageService, ArtworkImageService artworkImageService) {
        this.repository = repository;
        this.s3StorageService = s3StorageService;
        this.artworkImageService = artworkImageService;
    }

    public UserResponse register(UserRegisterRequest userRequest) {
        var email = userRequest.getEmail();
        var oExistingUser = repository.findByEmail(email);
        if (oExistingUser.isPresent()) {
            throw new ConflictException(ErrorMessages.EMAIL_ALREADY_TAKEN);
        }

        var password = userRequest.getPassword();
        var passwordHash = encoder.encode(password);
        var newUser = new User(null, userRequest.getName(), email, passwordHash, null, null, userRequest.getCity(), userRequest.getPostcode(), null, null);
        var savedUser = repository.save(newUser);
        return UserResponse.from(savedUser);
    }

    public Optional<UserResponse> getById(Long id) {
        return repository.findById(id)
                         .map(UserResponse::from);
    }

    @Transactional
    public UserResponse update(Long id, UserUpdateRequest request) {
        var user = fetchUser(id);
        user.setName(request.getName());
        user.setCity(request.getCity());
        user.setPostcode(request.getPostcode());
        user.setAbout(request.getAbout());

        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse updateAvatar(Long id, MultipartFile file) {
        var user = fetchUser(id);
        throwIfInvalidImageFile(file);
        var newUrl = s3StorageService.uploadFile(file);
        if (user.getAvatarUrl() != null) {
            s3StorageService.deleteFile(user.getAvatarUrl());
        }

        user.setAvatarUrl(newUrl);
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse updateBanner(Long id, MultipartFile file) {
        var user = fetchUser(id);
        throwIfInvalidImageFile(file);
        var newUrl = s3StorageService.uploadFile(file);
        if (user.getBannerUrl() != null) {
            s3StorageService.deleteFile(user.getBannerUrl());
        }

        user.setBannerUrl(newUrl);
        return UserResponse.from(user);
    }

    @Transactional
    public void deleteAvatar(Long id) {
        var user = fetchUser(id);
        if (user.getAvatarUrl() != null) {
            s3StorageService.deleteFile(user.getAvatarUrl());
        }
        user.setAvatarUrl(null);
    }

    @Transactional
    public void deleteBanner(Long id) {
        var user = fetchUser(id);
        if (user.getBannerUrl() != null) {
            s3StorageService.deleteFile(user.getBannerUrl());
        }
        user.setBannerUrl(null);
    }

    @Transactional
    public void delete(Long id) {
        var user = fetchUser(id);
        artworkImageService.deleteAllImagesForArtist(id);
        if (user.getAvatarUrl() != null) {
            s3StorageService.deleteFile(user.getAvatarUrl());
        }
        if (user.getBannerUrl() != null) {
            s3StorageService.deleteFile(user.getBannerUrl());
        }
        repository.delete(user);
    }

    private User fetchUser(Long id) {
        return repository.findById(id)
                         .orElseThrow(() -> new NotFoundException(ErrorMessages.USER_NOT_FOUND));
    }

    // TODO think about a place to make this accessible to ArtworkImageService too
    private void throwIfInvalidImageFile(MultipartFile file) {
        if (file.isEmpty() || !ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new BadRequestException(ErrorMessages.INVALID_IMAGE_FILE);
        }
        try {
            if (ImageIO.read(file.getInputStream()) == null) {
                throw new BadRequestException(ErrorMessages.INVALID_IMAGE_FILE);
            }
        } catch (IOException e) {
            throw new BadRequestException(ErrorMessages.INVALID_IMAGE_FILE);
        }
    }

}
