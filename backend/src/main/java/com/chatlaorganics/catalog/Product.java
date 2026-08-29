package com.chatlaorganics.catalog;

import java.math.BigDecimal;

public record Product(
        Long id,
        String name,
        String slug,
        String category,
        String description,
        BigDecimal price,
        BigDecimal salePrice,
        int stockQuantity,
        String imageUrl,
        boolean published) {
}
