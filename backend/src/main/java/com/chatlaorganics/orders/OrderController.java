package com.chatlaorganics.orders;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {
    private final JdbcTemplate jdbcTemplate;

    public OrderController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public OrderSummary create(@Valid @RequestBody CreateOrderRequest request) {
        BigDecimal total = request.items().stream()
                .map(item -> item.unitPrice().multiply(BigDecimal.valueOf(item.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        String orderNumber = "CHATLA-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        jdbcTemplate.update("INSERT INTO orders (order_number, customer_name, phone, address_line, city, pincode, payment_method, status, total) VALUES (?, ?, ?, ?, ?, ?, ?, 'NEW', ?)",
                orderNumber, request.customerName(), request.phone(), request.addressLine(), request.city(), request.pincode(), request.paymentMethod(), total);
        Long orderId = jdbcTemplate.queryForObject("SELECT id FROM orders WHERE order_number = ?", Long.class, orderNumber);
        for (OrderItem item : request.items()) {
            jdbcTemplate.update("INSERT INTO order_items (order_id, product_id, product_name, quantity, unit_price) VALUES (?, ?, ?, ?, ?)", orderId, item.productId(), item.productName(), item.quantity(), item.unitPrice());
        }
        return new OrderSummary(orderNumber, total, "NEW");
    }

    @GetMapping
    public List<AdminOrder> list() {
        return jdbcTemplate.query("SELECT id, order_number, customer_name, phone, city, payment_method, status, total, created_at FROM orders ORDER BY created_at DESC", (rs, row) -> new AdminOrder(
                rs.getLong("id"), rs.getString("order_number"), rs.getString("customer_name"), rs.getString("phone"), rs.getString("city"), rs.getString("payment_method"), rs.getString("status"), rs.getBigDecimal("total"), rs.getTimestamp("created_at").toLocalDateTime()));
    }

    public record CreateOrderRequest(
            @NotBlank String customerName,
            @NotBlank @Pattern(regexp = "[6-9][0-9]{9}") String phone,
            @NotBlank String addressLine,
            @NotBlank String city,
            @NotBlank @Pattern(regexp = "[0-9]{6}") String pincode,
            @NotBlank String paymentMethod,
            @NotEmpty List<@Valid OrderItem> items) {
    }

    public record OrderItem(@NotNull Long productId, @NotBlank String productName, @NotNull Integer quantity, @NotNull BigDecimal unitPrice) {
    }

    public record OrderSummary(String orderNumber, BigDecimal total, String status) {
    }

    public record AdminOrder(Long id, String orderNumber, String customerName, String phone, String city, String paymentMethod, String status, BigDecimal total, LocalDateTime createdAt) {
    }
}
