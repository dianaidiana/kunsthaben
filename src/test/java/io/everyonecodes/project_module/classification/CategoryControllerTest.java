package io.everyonecodes.project_module.classification;

import io.everyonecodes.project_module.classification.category.Category;
import io.everyonecodes.project_module.classification.category.CategoryController;
import io.everyonecodes.project_module.classification.category.CategoryService;
import io.everyonecodes.project_module.classification.enums.CategoryCode;
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
public class CategoryControllerTest {

    @Autowired
    CategoryController controller;

    @MockitoBean
    CategoryService service;

    @Autowired
    RestTestClient client;

    @Test
    void getAll() {
        var expected = List.of(
                new Category(1L, "Painting", CategoryCode.PAINTING.getCode()),
                new Category(2L, "Drawing", CategoryCode.DRAWING.getCode())
        );
        when(service.findAll()).thenReturn(expected);
        List<Category> response = client.get()
                                        .uri("/category")
                                        .exchange()
                                        .expectStatus().isOk()
                                        .expectBody(new ParameterizedTypeReference<List<Category>>() {
                                        })
                                        .returnResult()
                                        .getResponseBody();
        assertEquals(expected, response);
    }

    @Test
    void getByExistentId() {
        var expected = new Category(1L, "Painting", CategoryCode.PAINTING.getCode());
        when(service.findById(1L)).thenReturn(Optional.of(expected));
        Category response = client.get()
                                  .uri("/category/1")
                                  .exchange()
                                  .expectStatus().isOk()
                                  .expectBody(new ParameterizedTypeReference<Category>() {
                                  })
                                  .returnResult()
                                  .getResponseBody();
        assertEquals(expected, response);
    }

    @Test
    void getByUnexistentId() {
        client.get()
              .uri("/category/1")
              .exchange()
              .expectStatus().isNotFound();
    }

    @Test
    void getByExistentCode() {
        var expected = new Category(1L, "Painting", CategoryCode.PAINTING.getCode());
        when(service.findByCode(CategoryCode.PAINTING.getCode())).thenReturn(Optional.of(expected));
        Category response = client.get()
                                  .uri("/category/code/" + CategoryCode.PAINTING.getCode())
                                  .exchange()
                                  .expectStatus().isOk()
                                  .expectBody(new ParameterizedTypeReference<Category>() {
                                  })
                                  .returnResult()
                                  .getResponseBody();
        assertEquals(expected, response);
    }

    @Test
    void getByUnexistentCode() {
        client.get()
              .uri("/category/code/CAT_UNKNOWN")
              .exchange()
              .expectStatus().isNotFound();
    }

}