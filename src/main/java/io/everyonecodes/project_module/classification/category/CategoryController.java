package io.everyonecodes.project_module.classification.category;

import io.everyonecodes.project_module.exceptions.ErrorMessages;
import io.everyonecodes.project_module.exceptions.NotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping("/category")
    List<Category> getAll() {
        return categoryService.findAll();
    }

    @GetMapping("/category/{id}")
    Category getById(@PathVariable Long id) {
        return categoryService.findById(id)
                              .orElseThrow(() -> new NotFoundException(ErrorMessages.CATEGORY_NOT_FOUND));
    }

    @GetMapping("/category/code/{code}")
    Category getByCode(@PathVariable String code) {
        return categoryService.findByCode(code)
                              .orElseThrow(() -> new NotFoundException(ErrorMessages.CATEGORY_NOT_FOUND));
    }
}
