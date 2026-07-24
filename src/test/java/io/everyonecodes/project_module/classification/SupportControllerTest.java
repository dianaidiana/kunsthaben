package io.everyonecodes.project_module.classification;

import io.everyonecodes.project_module.classification.category.Category;
import io.everyonecodes.project_module.classification.enums.CategoryCode;
import io.everyonecodes.project_module.classification.enums.SupportCode;
import io.everyonecodes.project_module.classification.support.Support;
import io.everyonecodes.project_module.classification.support.SupportController;
import io.everyonecodes.project_module.classification.support.SupportService;
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
public class SupportControllerTest {

    @Autowired
    SupportController controller;

    @MockitoBean
    SupportService service;

    @Autowired
    RestTestClient client;

    private final Category paintingCategory = new Category(1L, "Painting", CategoryCode.PAINTING.getCode());

    @Test
    void getAll() {
        var expected = List.of(
                new Support(1L, "Canvas", SupportCode.CANVAS.getCode(), paintingCategory),
                new Support(2L, "Wood Panel", SupportCode.WOOD_PANEL.getCode(), paintingCategory),
                new Support(3L, "Linen", SupportCode.LINEN.getCode(), paintingCategory)
        );
        when(service.findAll()).thenReturn(expected);
        List<Support> response = client.get()
                                       .uri("/support")
                                       .exchange()
                                       .expectStatus().isOk()
                                       .expectBody(new ParameterizedTypeReference<List<Support>>() {
                                       })
                                       .returnResult()
                                       .getResponseBody();
        assertEquals(expected, response);
    }

    @Test
    void getByExistentId() {
        var expected = new Support(1L, "Canvas", SupportCode.CANVAS.getCode(), paintingCategory);
        when(service.findById(1L)).thenReturn(Optional.of(expected));
        Support response = client.get()
                                 .uri("/support/1")
                                 .exchange()
                                 .expectStatus().isOk()
                                 .expectBody(new ParameterizedTypeReference<Support>() {
                                 })
                                 .returnResult()
                                 .getResponseBody();
        assertEquals(expected, response);
    }

    @Test
    void getByUnexistentId() {
        client.get()
              .uri("/support/1")
              .exchange()
              .expectStatus().isNotFound();
    }

    @Test
    void getByExistentCode() {
        var expected = new Support(1L, "Canvas", SupportCode.CANVAS.getCode(), paintingCategory);
        when(service.findByCode(SupportCode.CANVAS.getCode())).thenReturn(Optional.of(expected));
        Support response = client.get()
                                 .uri("/support/code/" + SupportCode.CANVAS.getCode())
                                 .exchange()
                                 .expectStatus().isOk()
                                 .expectBody(new ParameterizedTypeReference<Support>() {
                                 })
                                 .returnResult()
                                 .getResponseBody();
        assertEquals(expected, response);
    }

    @Test
    void getByUnexistentCode() {
        client.get()
              .uri("/support/code/SUP_UNKNOWN")
              .exchange()
              .expectStatus().isNotFound();
    }

    @Test
    void getByCategory() {
        var expected = List.of(
                new Support(1L, "Canvas", SupportCode.CANVAS.getCode(), paintingCategory),
                new Support(2L, "Wood Panel", SupportCode.WOOD_PANEL.getCode(), paintingCategory)
        );
        when(service.findByCategoryId(1L)).thenReturn(expected);
        List<Support> response = client.get()
                                       .uri("/support/category/1")
                                       .exchange()
                                       .expectStatus().isOk()
                                       .expectBody(new ParameterizedTypeReference<List<Support>>() {
                                       })
                                       .returnResult()
                                       .getResponseBody();
        assertEquals(expected, response);
    }

}