package io.everyonecodes.project_module.classification.support;

import io.everyonecodes.project_module.exceptions.ErrorMessages;
import io.everyonecodes.project_module.exceptions.NotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class SupportController {

    private final SupportService supportService;

    public SupportController(SupportService supportService) {
        this.supportService = supportService;
    }

    @GetMapping("/support")
    List<Support> getAll() {
        return supportService.findAll();
    }

    @GetMapping("/support/{id}")
    Support getById(@PathVariable Long id) {
        return supportService.findById(id)
                             .orElseThrow(() -> new NotFoundException(ErrorMessages.SUPPORT_NOT_FOUND));
    }

    @GetMapping("/support/code/{code}")
    Support getByCode(@PathVariable String code) {
        return supportService.findByCode(code)
                             .orElseThrow(() -> new NotFoundException(ErrorMessages.SUPPORT_NOT_FOUND));
    }

    @GetMapping("/support/category/{categoryId}")
    List<Support> getByCategory(@PathVariable Long categoryId) {
        return supportService.findByCategoryId(categoryId);
    }
}
