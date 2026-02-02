package org.pl.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static org.pl.controller.Actions.itemsAction;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityFilterChain(
            ServerHttpSecurity http,
            ServerOAuth2AuthorizedClientRepository authorizedClientRepository
    ) {
        return http
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/", "/login", "/logout", "/oauth2/**", "/error", "/public/**").permitAll()
                        .anyExchange().authenticated()
                )
                // Отключаем ВСЮ дефолтную логику формы, т.к. сначала переходит на спринговую форму а с нее на кейклок, что есть лишний шаг
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .oauth2Login(oauth2 -> oauth2
                        .authenticationSuccessHandler(createSuccessHandler(authorizedClientRepository))
                )
                .oauth2Client(oauth2 -> oauth2
                        .authorizedClientRepository(authorizedClientRepository)
                )
                // ВАЖНО: Отключаем CSRF для OAuth2 редиректов
                .csrf(csrf -> csrf.disable())
                .build();
    }

    private ServerAuthenticationSuccessHandler createSuccessHandler(
            ServerOAuth2AuthorizedClientRepository authorizedClientRepository
    ) {
        return (webFilterExchange, authentication) -> {
            ServerWebExchange exchange = webFilterExchange.getExchange();
            
            if (authentication instanceof OAuth2AuthenticationToken) {
                OAuth2AuthenticationToken oauth2Token = (OAuth2AuthenticationToken) authentication;
                String registrationId = oauth2Token.getAuthorizedClientRegistrationId();
                
                System.out.println("SecurityConfig: Обработка успешной аутентификации для registrationId: " + registrationId);
                
                // Пробуем найти токен в сессии и сохранить его в репозитории
                return exchange.getSession()
                        .flatMap(session -> {
                            System.out.println("SecurityConfig: Проверка сессии, количество атрибутов: " + session.getAttributes().size());
                            
                            // Выводим все ключи в сессии для отладки
                            session.getAttributes().keySet().forEach(key -> 
                                System.out.println("SecurityConfig: Ключ в сессии: " + key)
                            );
                            
                            // Ищем токен в сессии по различным ключам
                            String[] possibleKeys = {
                                "SPRING_SECURITY_OAUTH2_AUTHORIZED_CLIENT_" + registrationId,
                                "oauth2AuthorizedClient_" + registrationId,
                                registrationId + "_authorized_client"
                            };
                            
                            OAuth2AuthorizedClient authorizedClient = null;
                            for (String key : possibleKeys) {
                                @SuppressWarnings("unchecked")
                                OAuth2AuthorizedClient client = (OAuth2AuthorizedClient) session.getAttribute(key);
                                if (client != null && client.getAccessToken() != null) {
                                    authorizedClient = client;
                                    System.out.println("SecurityConfig: Токен найден в сессии по ключу: " + key);
                                    break;
                                }
                            }
                            
                            // Если токен найден в сессии, сохраняем его в репозитории
                            if (authorizedClient != null) {
                                System.out.println("SecurityConfig: Сохранение токена в репозитории");
                                return authorizedClientRepository
                                        .saveAuthorizedClient(authorizedClient, oauth2Token, exchange)
                                        .doOnSuccess(v -> System.out.println("SecurityConfig: Токен успешно сохранен в репозитории"))
                                        .doOnError(e -> System.err.println("SecurityConfig: Ошибка при сохранении токена: " + e.getMessage()))
                                        .then(Mono.empty());
                            }
                            
                            System.err.println("SecurityConfig: Токен не найден в сессии");
                            return Mono.empty();
                        })
                        .then(new RedirectServerAuthenticationSuccessHandler(itemsAction)
                                .onAuthenticationSuccess(webFilterExchange, authentication));
            }
            
            return new RedirectServerAuthenticationSuccessHandler(itemsAction)
                    .onAuthenticationSuccess(webFilterExchange, authentication);
        };
    }
}