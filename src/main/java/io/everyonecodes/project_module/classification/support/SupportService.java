package io.everyonecodes.project_module.classification.support;

import io.everyonecodes.project_module.exceptions.ErrorMessages;
import io.everyonecodes.project_module.exceptions.NotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SupportService {

    private final SupportRepository repository;

    public SupportService(SupportRepository repository) {
        this.repository = repository;
    }

    public List<Support> findAll() {
        return repository.findAll();
    }

    public Optional<Support> findById(Long id) {
        return repository.findById(id);
    }

    public Support fetchSupport(Long id) {
        return repository.findById(id)
                         .orElseThrow(() -> new NotFoundException(ErrorMessages.SUPPORT_NOT_FOUND));
    }

    public Optional<Support> findByCode(String code) {
        return repository.findByCode(code);
    }

    public List<Support> findByCategoryId(Long id) {
        return repository.findByCategoryId(id);
    }
}
