package io.everyonecodes.project_module.classification.media;

import io.everyonecodes.project_module.exceptions.NotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class MediaController {

    private final MediaService mediaService;
    private final String NOT_FOUND_MESSAGE = "Media not found";

    public MediaController(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    @GetMapping("/media")
    List<Media> getAll() {
        return mediaService.findAll();
    }

    @GetMapping("/media/{id}")
    Media getById(@PathVariable Long id) {
        return mediaService.findById(id)
                           .orElseThrow(() -> new NotFoundException(NOT_FOUND_MESSAGE));
    }

    @GetMapping("/media/code/{code}")
    Media getByCode(@PathVariable String code) {
        return mediaService.findByCode(code)
                           .orElseThrow(() -> new NotFoundException(NOT_FOUND_MESSAGE));
    }

    @GetMapping("/media/category/{categoryId}")
    List<Media> getByCategory(@PathVariable Long categoryId) {
        return mediaService.findByCategoryId(categoryId);
    }
}
