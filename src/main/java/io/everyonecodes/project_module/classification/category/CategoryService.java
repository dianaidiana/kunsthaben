package io.everyonecodes.project_module.classification.category;

import io.everyonecodes.project_module.exceptions.ErrorMessages;
import io.everyonecodes.project_module.exceptions.NotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CategoryService {

    private final CategoryRepository repository;

    public CategoryService(CategoryRepository repository) {
        this.repository = repository;
    }

    public List<Category> findAll() {
        return repository.findAll();
    }

    public Optional<Category> findById(Long id) {
        return repository.findById(id);
    }

    public Category fetchCategory(Long id) {
        return repository.findById(id)
                         .orElseThrow(() -> new NotFoundException(ErrorMessages.CATEGORY_NOT_FOUND));
    }

    public Optional<Category> findByCode(String code) {
        return repository.findByCode(code);
    }
}
