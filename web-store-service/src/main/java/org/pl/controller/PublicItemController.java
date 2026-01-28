package org.pl.controller;

import org.pl.dao.Item;
import org.pl.dto.PagingInfoDto;
import org.pl.service.ItemService;
import org.pl.service.RedisCacheItemService;
import org.pl.service.SessionItemsCountsService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.result.view.Rendering;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.pl.controller.Actions.*;


@Controller
@RequestMapping()
public class PublicItemController {

    private final RedisCacheItemService redisCacheItemService;
    private final ItemService itemService;
    private final SessionItemsCountsService sessionItemsCountsService;

    public PublicItemController(
            RedisCacheItemService redisCacheItemService,
            ItemService itemService,
            SessionItemsCountsService sessionItemsCountsService
    ) {
        this.redisCacheItemService = redisCacheItemService;
        this.itemService = itemService;
        this.sessionItemsCountsService = sessionItemsCountsService;
    }

    @GetMapping()
    public Mono<String> redirectToItems() {
        return Mono.just("redirect:" + publicItemsAction);
    }

    @GetMapping(publicItemsAction)
    public Mono<Rendering> getItemsSorted(
            @RequestParam(defaultValue = "1") int pageNumber,
            @RequestParam(defaultValue = "5") int pageSize,
            @RequestParam(defaultValue = "NO") String sort,
            @RequestParam(required = false) String search,
            ServerWebExchange exchange
    ) {
        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize);

        return Mono.zip(
                        itemService.getItemsSorted(pageable, sort, search),
                        sessionItemsCountsService.getCartItems(exchange),
                        sessionItemsCountsService.checkItemsCount(exchange)
                )
                .map(tuple -> {
                    Page<List<Item>> itemPage = tuple.getT1();
                    var cartItems = tuple.getT2();
                    Integer totalItemsCounts = tuple.getT3();

                    return Rendering.view("public_items")
                            .modelAttribute("items", itemPage.getContent())
                            .modelAttribute("sort", sort)
                            .modelAttribute("search", search)
                            .modelAttribute("cartItems", cartItems)
                            .modelAttribute("totalItemsCounts", totalItemsCounts)
                            .modelAttribute("paging", new PagingInfoDto(
                                    itemPage.getNumber() + 1,
                                    itemPage.getTotalPages(),
                                    itemPage.getSize(),
                                    itemPage.hasPrevious(),
                                    itemPage.hasNext()
                            ))
                            .modelAttribute("ordersAction", ordersAction)
                            .modelAttribute("cartAction", cartAction)
                            .modelAttribute("publicItemsAction", publicItemsAction)
                            .modelAttribute("itemsToCartAction", itemsToCartAction)
                            .build();
                });
    }

    @GetMapping(publicItemsAction + "/{id}")
    public Mono<Rendering> getItemById(
            @PathVariable Long id,
            ServerWebExchange exchange
    ) {
        // Получаем item из кэша или БД
        Mono<Item> itemMono = redisCacheItemService.getItemFromCache(id)
                .switchIfEmpty(Mono.defer(() ->
                        itemService.getItemById(id)
                                .switchIfEmpty(Mono.error(new RuntimeException("Item not found")))
                                .flatMap(item ->
                                        // Сохраняем в кэш и возвращаем item
                                        redisCacheItemService.saveItemToCache(item).thenReturn(item)
                                )
                ));

        return Mono.zip(
                        itemMono,
                        itemService.getItemById(id).switchIfEmpty(Mono.error(new RuntimeException("Item not found"))),
                        sessionItemsCountsService.getCartItems(exchange),
                        sessionItemsCountsService.checkItemsCount(exchange),
                        exchange.getSession()
                )
                .map(tuple -> {

                    Item item = tuple.getT1();

                    var cartItems = tuple.getT3();
                    Integer totalItemsCounts = tuple.getT4();
                    Integer itemCount = cartItems.get(id);
                    var session = tuple.getT5();

                    // Получаем toast из сессии
                    String toastMessage = (String) session.getAttributes().get("toastMessage");
                    String toastType = (String) session.getAttributes().get("toastType");

                    // Удаляем из сессии после получения
                    session.getAttributes().remove("toastMessage");
                    session.getAttributes().remove("toastType");

                    return Rendering.view("public_item")
                            .modelAttribute("item", item)
                            .modelAttribute("ordersAction", ordersAction)
                            .modelAttribute("cartAction", cartAction)
                            .modelAttribute("publicItemsAction", publicItemsAction)
                            .modelAttribute("itemCounts", itemCount != null ? itemCount : 0)
                            .modelAttribute("itemsToCartAction", itemsToCartAction)
                            .modelAttribute("totalItemsCounts", totalItemsCounts)
                            .modelAttribute("buyAction", buyAction)
                            .modelAttribute("toastMessage", toastMessage)
                            .modelAttribute("toastType", toastType)
                            .build();
                });
    }
}
