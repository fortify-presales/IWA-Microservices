package com.microfocus.example.contracts.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Product entity representing pharmacy products
 * WARNING: Deliberately vulnerable - contains insecure serialization
 */
public class Product implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Long id;
    private String name;
    private String description;
    private String category;
    private BigDecimal price;
    private Integer stockQuantity;
    private String imageUrl;
    private Boolean requiresPrescription;
    private String manufacturer;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public Product() {}
    
    public Product(Long id, String name, String description, String category, 
                   BigDecimal price, Integer stockQuantity, Boolean requiresPrescription) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category;
        this.price = price;
        this.stockQuantity = stockQuantity;
        this.requiresPrescription = requiresPrescription;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    
    public Integer getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(Integer stockQuantity) { this.stockQuantity = stockQuantity; }
    
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    
    public Boolean getRequiresPrescription() { return requiresPrescription; }
    public void setRequiresPrescription(Boolean requiresPrescription) { 
        this.requiresPrescription = requiresPrescription; 
    }
    
    public String getManufacturer() { return manufacturer; }
    public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
