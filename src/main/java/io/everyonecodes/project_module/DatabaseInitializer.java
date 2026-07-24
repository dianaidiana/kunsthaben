package io.everyonecodes.project_module;

import io.everyonecodes.project_module.classification.category.Category;
import io.everyonecodes.project_module.classification.category.CategoryRepository;
import io.everyonecodes.project_module.classification.enums.CategoryCode;
import io.everyonecodes.project_module.classification.enums.MediaCode;
import io.everyonecodes.project_module.classification.enums.SupportCode;
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
                var painting = categoryRepository.save(new Category(null, "Painting", CategoryCode.PAINTING.getCode()));
                var drawing = categoryRepository.save(new Category(null, "Drawing", CategoryCode.DRAWING.getCode()));

                mediaRepository.save(new Media(null, "Oil", MediaCode.OIL.getCode(), painting));
                mediaRepository.save(new Media(null, "Acrylic", MediaCode.ACRYLIC.getCode(), painting));
                mediaRepository.save(new Media(null, "Watercolor", MediaCode.WATERCOLOR.getCode(), painting));
                mediaRepository.save(new Media(null, "Gouache", MediaCode.GOUACHE.getCode(), painting));
                mediaRepository.save(new Media(null, "Mixed media", MediaCode.MIXED_MEDIA.getCode(), painting));

                mediaRepository.save(new Media(null, "Charcoal", MediaCode.CHARCOAL.getCode(), drawing));
                mediaRepository.save(new Media(null, "Graphite", MediaCode.GRAPHITE.getCode(), drawing));
                mediaRepository.save(new Media(null, "Ink", MediaCode.INK.getCode(), drawing));
                mediaRepository.save(new Media(null, "Pastel", MediaCode.PASTEL.getCode(), drawing));
                mediaRepository.save(new Media(null, "Oil pastel", MediaCode.OIL_PASTEL.getCode(), drawing));

                supportRepository.save(new Support(null, "Canvas", SupportCode.CANVAS.getCode(), painting));
                supportRepository.save(new Support(null, "Wood Panel", SupportCode.WOOD_PANEL.getCode(), painting));
                supportRepository.save(new Support(null, "Linen", SupportCode.LINEN.getCode(), painting));
                supportRepository.save(new Support(null, "Paper", SupportCode.PAPER_PAINTING.getCode(), painting));

                supportRepository.save(new Support(null, "Paper", SupportCode.PAPER_DRAWING.getCode(), drawing));
                supportRepository.save(new Support(null, "Cardboard", SupportCode.CARDBOARD.getCode(), drawing));
                supportRepository.save(new Support(null, "Vellum", SupportCode.VELLUM.getCode(), drawing));

                System.out.println("Classification (categories, media, supports) has been successfully seeded.");
            }
        };
    }
}