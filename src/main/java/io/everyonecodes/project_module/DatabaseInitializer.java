package io.everyonecodes.project_module;

import io.everyonecodes.project_module.classification.category.Category;
import io.everyonecodes.project_module.classification.category.CategoryRepository;
import io.everyonecodes.project_module.classification.enums.CategoryEnum;
import io.everyonecodes.project_module.classification.enums.MediaEnum;
import io.everyonecodes.project_module.classification.enums.SupportEnum;
import io.everyonecodes.project_module.classification.media.Media;
import io.everyonecodes.project_module.classification.media.MediaRepository;
import io.everyonecodes.project_module.classification.support.Support;
import io.everyonecodes.project_module.classification.support.SupportRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DatabaseInitializer {

    @Bean
    CommandLineRunner initDatabase(CategoryRepository categoryRepository,
                                   MediaRepository mediaRepository,
                                   SupportRepository supportRepository) {

        return args -> {

            if (categoryRepository.findAll().isEmpty()) {
                var painting = categoryRepository.save(new Category(null, CategoryEnum.PAINTING.getName(), CategoryEnum.PAINTING.getCode()));
                var drawing = categoryRepository.save(new Category(null, CategoryEnum.DRAWING.getName(), CategoryEnum.DRAWING.getCode()));

                mediaRepository.save(new Media(null, MediaEnum.OIL.getName(), MediaEnum.OIL.getCode(), painting));
                mediaRepository.save(new Media(null, MediaEnum.ACRYLIC.getName(), MediaEnum.ACRYLIC.getCode(), painting));
                mediaRepository.save(new Media(null, MediaEnum.WATERCOLOR.getName(), MediaEnum.WATERCOLOR.getCode(), painting));
                mediaRepository.save(new Media(null, MediaEnum.GOUACHE.getName(), MediaEnum.GOUACHE.getCode(), painting));
                mediaRepository.save(new Media(null, MediaEnum.MIXED_MEDIA.getName(), MediaEnum.MIXED_MEDIA.getCode(), painting));

                mediaRepository.save(new Media(null, MediaEnum.CHARCOAL.getName(), MediaEnum.CHARCOAL.getCode(), drawing));
                mediaRepository.save(new Media(null, MediaEnum.GRAPHITE.getName(), MediaEnum.GRAPHITE.getCode(), drawing));
                mediaRepository.save(new Media(null, MediaEnum.INK.getName(), MediaEnum.INK.getCode(), drawing));
                mediaRepository.save(new Media(null, MediaEnum.PASTEL.getName(), MediaEnum.PASTEL.getCode(), drawing));
                mediaRepository.save(new Media(null, MediaEnum.OIL_PASTEL.getName(), MediaEnum.OIL_PASTEL.getCode(), drawing));

                supportRepository.save(new Support(null, SupportEnum.CANVAS.getName(), SupportEnum.CANVAS.getCode(), painting));
                supportRepository.save(new Support(null, SupportEnum.WOOD_PANEL.getName(), SupportEnum.WOOD_PANEL.getCode(), painting));
                supportRepository.save(new Support(null, SupportEnum.LINEN.getName(), SupportEnum.LINEN.getCode(), painting));
                supportRepository.save(new Support(null, SupportEnum.PAPER_PAINTING.getName(), SupportEnum.PAPER_PAINTING.getCode(), painting));

                supportRepository.save(new Support(null, SupportEnum.PAPER_DRAWING.getName(), SupportEnum.PAPER_DRAWING.getCode(), drawing));
                supportRepository.save(new Support(null, SupportEnum.CARDBOARD.getName(), SupportEnum.CARDBOARD.getCode(), drawing));
                supportRepository.save(new Support(null, SupportEnum.VELLUM.getName(), SupportEnum.VELLUM.getCode(), drawing));

                System.out.println("Classification (categories, media, supports) has been successfully seeded.");
            }
        };
    }
}