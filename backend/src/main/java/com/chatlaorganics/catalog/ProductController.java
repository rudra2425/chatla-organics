package com.chatlaorganics.catalog;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {
    private final JdbcTemplate jdbcTemplate;

    public ProductController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public List<Product> list() {
        return jdbcTemplate.query("SELECT id, name, slug, category, description, price, sale_price, stock_quantity, image_url, published FROM products ORDER BY created_at DESC", (resultSet, rowNumber) -> new Product(
            resultSet.getLong("id"), resultSet.getString("name"), resultSet.getString("slug"), resultSet.getString("category"), resultSet.getString("description"),
                resultSet.getBigDecimal("price"), resultSet.getBigDecimal("sale_price"), resultSet.getInt("stock_quantity"),
                resultSet.getString("image_url"), resultSet.getBoolean("published")));
    }

    @GetMapping("/{id}")
    public Product get(@PathVariable Long id) {
        return jdbcTemplate.queryForObject("SELECT id, name, slug, category, description, price, sale_price, stock_quantity, image_url, published FROM products WHERE id = ?", this::mapProduct, id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Product create(@Valid @RequestBody ProductRequest request) {
        jdbcTemplate.update("INSERT INTO products (name, slug, category, description, price, sale_price, stock_quantity, image_url, published) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
            request.name(), request.slug(), request.category(), request.description(), request.price(), request.salePrice(), request.stockQuantity(), request.imageUrl(), request.published());
        return jdbcTemplate.queryForObject("SELECT id, name, slug, category, description, price, sale_price, stock_quantity, image_url, published FROM products WHERE slug = ?", this::mapProduct, request.slug());
    }

    @PutMapping("/{id}")
    public Product update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        jdbcTemplate.update("UPDATE products SET name = ?, slug = ?, category = ?, description = ?, price = ?, sale_price = ?, stock_quantity = ?, image_url = ?, published = ? WHERE id = ?",
                request.name(), request.slug(), request.category(), request.description(), request.price(), request.salePrice(), request.stockQuantity(), request.imageUrl(), request.published(), id);
        return get(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        jdbcTemplate.update("DELETE FROM products WHERE id = ?", id);
    }

    private Product mapProduct(java.sql.ResultSet resultSet, int rowNumber) throws java.sql.SQLException {
        return new Product(resultSet.getLong("id"), resultSet.getString("name"), resultSet.getString("slug"), resultSet.getString("category"), resultSet.getString("description"),
                resultSet.getBigDecimal("price"), resultSet.getBigDecimal("sale_price"), resultSet.getInt("stock_quantity"), resultSet.getString("image_url"), resultSet.getBoolean("published"));
    }

    public record ProductRequest(
            @NotBlank String name,
            @NotBlank String slug,
            String category,
            String description,
            @NotNull @DecimalMin("0.00") BigDecimal price,
            @DecimalMin("0.00") BigDecimal salePrice,
            int stockQuantity,
            String imageUrl,
            boolean published) {
    }
}
