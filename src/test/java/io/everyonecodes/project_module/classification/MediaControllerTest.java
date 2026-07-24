package io.everyonecodes.project_module.classification;

import io.everyonecodes.project_module.classification.category.Category;
import io.everyonecodes.project_module.classification.enums.CategoryCode;
import io.everyonecodes.project_module.classification.enums.MediaCode;
import io.everyonecodes.project_module.classification.media.Media;
import io.everyonecodes.project_module.classification.media.MediaController;
import io.everyonecodes.project_module.classification.media.MediaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
public class MediaControllerTest {

    @Autowired
    MediaController controller;

    @MockitoBean
    MediaService service;

    @Autowired
    RestTestClient client;

    private final Category paintingCategory = new Category(1L, "Painting", CategoryCode.PAINTING.getCode());

    @Test
    void getAll() {
        var expected = List.of(
                new Media(1L, "Oil", MediaCode.OIL.getCode(), paintingCategory),
                new Media(2L, "Acrylic", MediaCode.ACRYLIC.getCode(), paintingCategory),
                new Media(3L, "Watercolor", MediaCode.WATERCOLOR.getCode(), paintingCategory)
        );
        when(service.findAll()).thenReturn(expected);
        List<Media> response = client.get()
                                     .uri("/media")
                                     .exchange()
                                     .expectStatus().isOk()
                                     .expectBody(new ParameterizedTypeReference<List<Media>>() {
                                     })
                                     .returnResult()
                                     .getResponseBody();
        assertEquals(expected, response);
    }

    @Test
    void getByExistentId() {
        var expected = new Media(1L, "Oil", MediaCode.OIL.getCode(), paintingCategory);
        when(service.findById(1L)).thenReturn(Optional.of(expected));
        Media response = client.get()
                               .uri("/media/1")
                               .exchange()
                               .expectStatus().isOk()
                               .expectBody(new ParameterizedTypeReference<Media>() {
                               })
                               .returnResult()
                               .getResponseBody();
        assertEquals(expected, response);
    }

    @Test
    void getByUnexistentId() {
        client.get()
              .uri("/media/1")
              .exchange()
              .expectStatus().isNotFound();
    }

    @Test
    void getByExistentCode() {
        var expected = new Media(1L, "Oil", MediaCode.OIL.getCode(), paintingCategory);
        when(service.findByCode(MediaCode.OIL.getCode())).thenReturn(Optional.of(expected));
        Media response = client.get()
                               .uri("/media/code/" + MediaCode.OIL.getCode())
                               .exchange()
                               .expectStatus().isOk()
                               .expectBody(new ParameterizedTypeReference<Media>() {
                               })
                               .returnResult()
                               .getResponseBody();
        assertEquals(expected, response);
    }

    @Test
    void getByUnexistentCode() {
        client.get()
              .uri("/media/code/MED_UNKNOWN")
              .exchange()
              .expectStatus().isNotFound();
    }

    @Test
    void getByCategory() {
        var expected = List.of(
                new Media(1L, "Oil", MediaCode.OIL.getCode(), paintingCategory),
                new Media(2L, "Acrylic", MediaCode.ACRYLIC.getCode(), paintingCategory)
        );
        when(service.findByCategoryId(1L)).thenReturn(expected);
        List<Media> response = client.get()
                                     .uri("/media/category/1")
                                     .exchange()
                                     .expectStatus().isOk()
                                     .expectBody(new ParameterizedTypeReference<List<Media>>() {
                                     })
                                     .returnResult()
                                     .getResponseBody();
        assertEquals(expected, response);
    }

}