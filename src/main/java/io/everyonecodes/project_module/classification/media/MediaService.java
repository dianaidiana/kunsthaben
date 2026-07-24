package io.everyonecodes.project_module.classification.media;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MediaService {

    private final MediaRepository repository;

    public MediaService(MediaRepository repository) {
        this.repository = repository;
    }

    public List<Media> findAll() {
        return repository.findAll();
    }

    public Optional<Media> findById(Long id) {
        return repository.findById(id);
    }

    public Optional<Media> findByCode(String code) {
        return repository.findByCode(code);
    }

    public List<Media> findByCategoryId(Long id) {
        return repository.findByCategoryId(id);
    }
}
