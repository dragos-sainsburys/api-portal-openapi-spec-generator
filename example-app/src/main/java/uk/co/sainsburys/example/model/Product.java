package uk.co.sainsburys.example.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "products")
@Schema(description = "Product entity representing items in the catalogue")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Unique identifier for the product", example = "1")
    private Long id;

    @Column(nullable = false, unique = true)
    @Schema(description = "Stock Keeping Unit - unique product code", example = "SKU-12345", required = true)
    private String sku;

    @Column(nullable = false)
    @Schema(description = "Product name", example = "Organic Bananas", required = true)
    private String name;

    @Column(length = 1000)
    @Schema(description = "Detailed product description", example = "Fresh organic bananas from sustainable farms")
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    @Schema(description = "Product price", example = "2.99", required = true)
    private BigDecimal price;

    @Column(nullable = false)
    @Schema(description = "Available stock quantity", example = "150", required = true)
    private Integer stockQuantity;

    @Schema(description = "Product category", example = "Fruits & Vegetables")
    private String category;

    // Constructors
    public Product() {
    }

    public Product(String sku, String name, String description, BigDecimal price, Integer stockQuantity, String category) {
        this.sku = sku;
        this.name = name;
        this.description = description;
        this.price = price;
        this.stockQuantity = stockQuantity;
        this.category = category;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(Integer stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}

