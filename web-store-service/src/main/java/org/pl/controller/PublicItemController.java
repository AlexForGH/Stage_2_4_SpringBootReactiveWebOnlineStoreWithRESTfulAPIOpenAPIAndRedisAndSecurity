package org.pl.controller;

import org.pl.dao.Item;
import org.pl.dto.PagingInfoDto;
import org.pl.service.ItemService;
import org.pl.service.RedisCacheItemService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.result.view.Rendering;
import reactor.core.publisher.Mono;

import static org.pl.controller.Actions.loginAction;
import static org.pl.controller.Actions.publicItemsAction;


@Controller
@RequestMapping()
public class PublicItemController {

    private final RedisCacheItemService redisCacheItemService;
    private final ItemService itemService;

    public PublicItemController(
            RedisCacheItemService redisCacheItemService,
            ItemService itemService
    ) {
        this.redisCacheItemService = redisCacheItemService;
        this.itemService = itemService;
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
            @RequestParam(required = false) String search
    ) {
        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize);

        return itemService.getItemsSorted(pageable, sort, search)
                .map(itemPage -> Rendering.view("public_items")
                        .modelAttribute("items", itemPage.getContent())
                        .modelAttribute("sort", sort)
                        .modelAttribute("search", search)
                        .modelAttribute("paging", new PagingInfoDto(
                                itemPage.getNumber() + 1,
                                itemPage.getTotalPages(),
                                itemPage.getSize(),
                                itemPage.hasPrevious(),
                                itemPage.hasNext()
                        ))
                        .modelAttribute("loginAction", loginAction)
                        .modelAttribute("publicItemsAction", publicItemsAction)
                        .build());
    }

    @GetMapping(publicItemsAction + "/{id}")
    public Mono<Rendering> getItemById(@PathVariable Long id) {
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

        return itemMono.map(item -> Rendering.view("public_item")
                .modelAttribute("item", item)
                .modelAttribute("publicItemsAction", publicItemsAction)
                .build());
    }
}
