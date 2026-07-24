package io.everyonecodes.project_module.classification.support;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SupportRepository extends JpaRepository<Support, Long> {

    List<Support> findByCategoryId(Long categoryId);

    Optional<Support> findByCode(String code);
}
