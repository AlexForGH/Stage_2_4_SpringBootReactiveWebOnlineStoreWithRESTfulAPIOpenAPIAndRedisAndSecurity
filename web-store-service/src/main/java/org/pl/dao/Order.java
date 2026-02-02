package org.pl.dao;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Table(name = "orders")
public class Order {
    @Id
    @Column("id")
    private Long id;
    @Column("order_number")
    private String orderNumber;
    @Column("total_amount")
    private BigDecimal totalAmount;
    @Column("order_date")
    private LocalDateTime orderDate;
    @Column("user_id")
    private UUID userId;

    public Order() {
    }

    public Order(String orderNumber, BigDecimal totalAmount, LocalDateTime orderDate, UUID userId) {
        this.orderNumber = orderNumber;
        this.totalAmount = totalAmount;
        this.orderDate = orderDate;
        this.userId = userId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public LocalDateTime getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(LocalDateTime orderDate) {
        this.orderDate = orderDate;
    }
}
