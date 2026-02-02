package org.pl.controller;

import org.junit.jupiter.api.BeforeEach;
import org.pl.service.CartService;
import org.pl.service.ItemService;
import org.pl.service.OrderItemService;
import org.pl.service.SessionItemsCountsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockOAuth2Login;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.springSecurity;

@SpringBootTest
@AutoConfigureWebTestClient
public abstract class ControllerIntegrationTest {

    /**
     * Тестовый UUID пользователя (соответствует "sub" claim в OAuth2 токене)
     */
    protected static final UUID TEST_USER_ID = UUID.fromString("b4f2e6d1-5555-6666-7777-888888888888");
    protected static final String TEST_USERNAME = "testuser";

    @Autowired
    private ApplicationContext applicationContext;

    protected WebTestClient webTestClient;

    @MockitoBean
    protected CartService cartService;

    @MockitoBean
    protected SessionItemsCountsService sessionItemsCountsService;

    @MockitoBean
    protected ItemService itemService;

    @MockitoBean
    protected OrderItemService orderItemService;

    /**
     * Настраивает WebTestClient с OAuth2 аутентификацией перед каждым тестом.
     * По умолчанию все запросы идут от имени аутентифицированного пользователя.
     */
    @BeforeEach
    void setUpWebTestClient() {
        this.webTestClient = WebTestClient
                .bindToApplicationContext(applicationContext)
                .apply(springSecurity())
                .configureClient()
                .build()
                .mutateWith(mockOAuth2Login()
                        .oauth2User(createTestOAuth2User()));
    }

    /**
     * Создает тестового OAuth2 пользователя с необходимыми атрибутами.
     * Атрибут "sub" используется контроллерами для идентификации пользователя.
     */
    protected OAuth2User createTestOAuth2User() {
        Map<String, Object> attributes = Map.of(
                "sub", TEST_USER_ID.toString(),
                "preferred_username", TEST_USERNAME,
                "email", TEST_USERNAME + "@test.com",
                "name", "Test User"
        );

        return new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                attributes,
                "sub"
        );
    }

    /**
     * Создает WebTestClient для неаутентифицированных запросов.
     * Используйте для тестирования публичных эндпоинтов или проверки 401.
     */
    protected WebTestClient unauthenticatedClient() {
        return WebTestClient
                .bindToApplicationContext(applicationContext)
                .apply(springSecurity())
                .configureClient()
                .build();
    }
}