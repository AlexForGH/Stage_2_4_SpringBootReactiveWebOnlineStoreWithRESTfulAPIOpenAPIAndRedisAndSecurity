package org.pl.repository;

import org.pl.dao.OrderItem;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

import java.util.UUID;

public interface OrderItemRepository extends ReactiveCrudRepository<OrderItem, Long> {
    @Query("SELECT * FROM order_items")
    Flux<OrderItem> findAllWithAssociations();

    @Query("SELECT * FROM order_items WHERE order_id = :orderId")
    Flux<OrderItem> findByOrderIdWithAssociations(Long orderId);

    @Query("""
        SELECT oi.* 
        FROM order_items oi
        JOIN orders o ON oi.order_id = o.id
        WHERE o.user_id = :userId
    """)
    Flux<OrderItem> findByUserId(UUID userId);
}
