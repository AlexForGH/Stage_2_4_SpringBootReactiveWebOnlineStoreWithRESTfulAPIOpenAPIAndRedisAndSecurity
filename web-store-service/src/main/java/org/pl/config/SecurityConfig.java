package org.pl.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationEntryPoint;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityFilterChain(ServerHttpSecurity http) {
        return http
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/", "/login", "/oauth2/**", "/error", "/public/**").permitAll()
                        .anyExchange().authenticated()
                )
                // Отключаем ВСЮ дефолтную логику формы, т.к. сначала переходит на спринговую форму а с нее на кейклок, что есть лишний шаг
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .oauth2Login(Customizer.withDefaults())
                // ВАЖНО: Отключаем CSRF для OAuth2 редиректов
                .csrf(csrf -> csrf.disable())
                // ИЛИ настройка исключений:
                // .csrf(csrf -> csrf.ignoringRequestMatchers(
                //     "/login/oauth2/code/**",
                //     "/logout"
                // ))
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(new RedirectServerAuthenticationEntryPoint("/oauth2/authorization/keycloak"))
                )
                .build();
    }

//    @Bean
//    public OAuth2AuthorizedClientManager authorizedClientManager(
//            ClientRegistrationRepository clientRegistrationRepository,
//            OAuth2AuthorizedClientService authorizedClientService
//    ) {
//        AuthorizedClientServiceOAuth2AuthorizedClientManager manager =
//                new AuthorizedClientServiceOAuth2AuthorizedClientManager(clientRegistrationRepository, authorizedClientService);
//
//        manager.setAuthorizedClientProvider(OAuth2AuthorizedClientProviderBuilder.builder()
//                .clientCredentials() // Включаем получение токена с помощью client_credentials
//                .refreshToken() // Также включаем использование refresh_token
//                .build());
//
//        return manager;
//    }
}