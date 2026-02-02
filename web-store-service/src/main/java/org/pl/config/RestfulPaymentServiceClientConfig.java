package org.pl.config;

import org.pl.webstore.client.payment.api.DefaultApi;
import org.pl.webstore.client.payment.invoker.ApiClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Configuration
public class RestfulPaymentServiceClientConfig {

    @Value("${restful.payment.service.url}")
    private String paymentServiceUrl;

    @Bean
    public WebClient paymentServiceWebClient(
            ReactiveClientRegistrationRepository clientRegistrationRepository,
            ServerOAuth2AuthorizedClientRepository authorizedClientRepository
    ) {
        ServerOAuth2AuthorizedClientExchangeFilterFunction oauth2Filter =
                new ServerOAuth2AuthorizedClientExchangeFilterFunction(
                        clientRegistrationRepository,
                        authorizedClientRepository
                );
        oauth2Filter.setDefaultClientRegistrationId("keycloak");

        return WebClient.builder()
                .baseUrl(paymentServiceUrl)
                .filter(oauth2Filter)
                .filter((request, next) -> {
                    // Fallback фильтр: если стандартный фильтр не добавил токен, пробуем извлечь из сессии
                    return Mono.deferContextual(contextView -> {
                        ServerWebExchange exchange = contextView.getOrDefault(ServerWebExchange.class, null);
                        
                        if (exchange == null) {
                            return next.exchange(request);
                        }
                        
                        // Проверяем, есть ли уже заголовок Authorization
                        if (request.headers().getFirst("Authorization") != null) {
                            System.out.println("PaymentServiceWebClient: Токен уже добавлен стандартным фильтром");
                            return next.exchange(request);
                        }
                        
                        System.out.println("PaymentServiceWebClient: Токен не найден стандартным фильтром, пробуем сессию");
                        
                        return ReactiveSecurityContextHolder.getContext()
                                .cast(org.springframework.security.core.context.SecurityContext.class)
                                .map(org.springframework.security.core.context.SecurityContext::getAuthentication)
                                .cast(Authentication.class)
                                .flatMap(authentication -> {
                                    if (authentication instanceof OAuth2AuthenticationToken) {
                                        OAuth2AuthenticationToken oauth2Token = (OAuth2AuthenticationToken) authentication;
                                        String registrationId = oauth2Token.getAuthorizedClientRegistrationId();
                                        
                                        return exchange.getSession()
                                                .flatMap(session -> {
                                                    System.out.println("PaymentServiceWebClient: Проверка сессии, количество атрибутов: " + session.getAttributes().size());
                                                    
                                                    // Выводим все ключи в сессии для отладки
                                                    session.getAttributes().keySet().forEach(key -> 
                                                        System.out.println("PaymentServiceWebClient: Ключ в сессии: " + key + ", тип: " + 
                                                            (session.getAttribute(key) != null ? session.getAttribute(key).getClass().getName() : "null"))
                                                    );
                                                    
                                                    // Проверяем различные возможные ключи в сессии
                                                    String[] possibleKeys = {
                                                        "SPRING_SECURITY_OAUTH2_AUTHORIZED_CLIENT_" + registrationId,
                                                        "oauth2AuthorizedClient_" + registrationId,
                                                        registrationId + "_authorized_client"
                                                    };
                                                    
                                                    for (String key : possibleKeys) {
                                                        @SuppressWarnings("unchecked")
                                                        OAuth2AuthorizedClient authorizedClient = 
                                                                (OAuth2AuthorizedClient) session.getAttribute(key);
                                                        if (authorizedClient != null && authorizedClient.getAccessToken() != null) {
                                                            String token = authorizedClient.getAccessToken().getTokenValue();
                                                            System.out.println("PaymentServiceWebClient: Токен найден в сессии по ключу: " + key);
                                                            ClientRequest authorizedRequest = ClientRequest.from(request)
                                                                    .header("Authorization", "Bearer " + token)
                                                                    .build();
                                                            return next.exchange(authorizedRequest);
                                                        }
                                                    }
                                                    
                                                    System.err.println("PaymentServiceWebClient: Токен не найден ни в репозитории, ни в сессии");
                                                    return next.exchange(request);
                                                });
                                    }
                                    return next.exchange(request);
                                })
                                .switchIfEmpty(next.exchange(request));
                    });
                })
                .build();
    }

    @Bean
    public ApiClient paymentApiClient(WebClient paymentServiceWebClient) {
        ApiClient apiClient = new ApiClient(paymentServiceWebClient);
        apiClient.setBasePath(paymentServiceUrl);
        return apiClient;
    }

    @Bean
    public DefaultApi balanceApi(ApiClient paymentApiClient) {
        return new DefaultApi(paymentApiClient);
    }
}