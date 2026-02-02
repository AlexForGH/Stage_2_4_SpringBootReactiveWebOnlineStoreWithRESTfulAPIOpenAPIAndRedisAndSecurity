# Stage_2_4_SpringBootReactiveWebOnlineStoreWithRESTfulAPIOpenAPIAndRedisAndSecurity

![Демо работы](./demo.gif)

## *EN*
#### Link to the previous project based on the Spring Boot and reactive technology stack: https://github.com/AlexForGH/Stage_2_3_SpringBootReactiveWebOnlineStoreWithRESTfulAPIOpenAPIAndRedis.git
#### The project demonstrates development capabilities using Spring Boot on a reactive technology stack with the development of a RESTful service, Open API specification, caching via Redis and security through Spring Security and Keycloak: Spring WebFlux, Spring Data R2DBC, RESTful API, Open API, Spring Data Redis, Spring Security, Keycloak.
#### Technology stack: Spring Framework, Spring Boot, H2DB, HTML, Thymeleaf, Spring WebFlux, Spring Data R2DBC, Docker (Multi Stage Build), Docker Compose, RESTful API, Open API, Spring Data Redis, Spring Security, Keycloak.

### Application features:
    - user self-registration
    - filling the shopping cart with products
    - editing the shopping cart
    - removing products from the cart
    - searching for products by name
    - sorting products
    - creating product orders, with a check of the user's available balance, which is requested from the payment service via the RESTful API

### Application deployment:
    - Before you begin, you'll need:
            - Java (JRE) (version 23 was used during project development)
            - Docker (Multi Stage Build)
    1. Using an IDE (IntelliJIdea was used during project development):
            - clone the repository
            - open the project in the IDE
            - right‑click on the docker-compose.yml and select “Run docker-compose.yml”
            - go to the browser at http://localhost:8080/
            - the application's start page will open
    2. Without an IDE
            - clone the repository
            - run the following Docker commands:
                    - docker compose up --build
            - go to the browser at http://localhost:8082/
                - the application's start page will open.


## *RU*
#### Ссылка на предыдущий проект на основе Spring Boot и реактивного стека технологий: https://github.com/AlexForGH/Stage_2_3_SpringBootReactiveWebOnlineStoreWithRESTfulAPIOpenAPIAndRedis.git
#### Проект для демонстрации возможностей разработки с использованием Spring Boot на реактивном стеке технологий с разработкой RESTful сервиса, спецификации Open API, организации кэширования через Redis и обеспечения безопасности через Spring Security и Keycloak: Spring WebFlux, Spring Data R2DBC, RESTful API, Open API, Spring Data Redis, Spring Security, Keycloak.
#### Технологический стек: Spring Framework, Spring Boot, H2DB, HTML, Thymeleaf, Spring WebFlux, Spring Data R2DBC, Docker (Multi Stage Build), Docker Compose, RESTful API, Open API, Spring Data Redis, Spring Security, Keycloak.

### Возможности приложения:
    - саморегистрация пользователя
    - наполнение корзины товаров
    - редактирование корзины
    - удаление товаров из корзины
    - поиск товаров по названию
    - сортировка товаров
    - создание заказов на товары, с проверкой доступного баланса пользователя, который запрашивается из сервиса оплаты по RESTful API

### Развертывание приложения:
    - Перед началом работы необходимы:
            - Java (JRE) (при разработке проекта использовалась версия 23)
            - Docker (Multi Stage Build)
    1. Через IDE (при разработке проекта использовалась IntelliJIdea):
            - клонировать репозиторий
            - открыть проект в IDE
            - нажать ПКМ на docker-compose.yml и выбрать "Run docker-compose.yml"
            - зайти в браузер по адресу http://localhost:8082/
            - откроется стартовая страница приложения
    2. Без использования IDE
            - клонировать репозиторий
            - выполнить команды докера:
                    - docker compose up --build
            - зайти в браузер по адресу http://localhost:8082/
            - откроется стартовая страница приложения.
