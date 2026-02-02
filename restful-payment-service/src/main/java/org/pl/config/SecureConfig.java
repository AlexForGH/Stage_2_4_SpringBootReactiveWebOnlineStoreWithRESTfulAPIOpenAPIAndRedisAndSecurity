package org.pl.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;

@Configuration
public class SecureConfig {

    /**
     * Внутренний URL Keycloak для получения JWK ключей (используется внутри Docker сети)
     */
    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String jwkSetUri;

    /**
     * Список допустимых issuer'ов.
     * Первый - внутренний URL (для Docker), второй - публичный URL (для браузера).
     * Токен будет принят, если его issuer совпадает с любым из списка.
     */
    @Value("${jwt.allowed-issuers:http://keycloak-service:8080/realms/web-store,http://localhost:8080/realms/web-store}")
    private List<String> allowedIssuers;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity security) throws Exception {
        return security
                .authorizeHttpRequests(requests -> requests
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.decoder(jwtDecoder()))
                )
                .build();
    }

    /**
     * Кастомный JwtDecoder который:
     * 1. Использует внутренний URL для получения JWK ключей (работает в Docker сети)
     * 2. Допускает несколько issuer'ов (и внутренний, и публичный URL)
     * 
     * Это решает проблему "dual issuer" когда браузер получает токен по localhost,
     * а сервис проверяет его внутри Docker сети.
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();

        // Создаем валидатор, который принимает любой из допустимых issuer'ов
        OAuth2TokenValidator<Jwt> multiIssuerValidator = token -> {
            String tokenIssuer = token.getIssuer().toString();
            if (allowedIssuers.contains(tokenIssuer)) {
                return OAuth2TokenValidatorResult.success();
            }
            return OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("invalid_issuer", 
                            "Token issuer '" + tokenIssuer + "' is not in allowed list: " + allowedIssuers, 
                            null)
            );
        };

        // Комбинируем валидатор issuer с валидатором timestamp (exp, nbf, iat)
        OAuth2TokenValidator<Jwt> validator = new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(),
                multiIssuerValidator
        );

        jwtDecoder.setJwtValidator(validator);
        return jwtDecoder;
    }
}
