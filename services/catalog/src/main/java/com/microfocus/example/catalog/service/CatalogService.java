package com.microfocus.example.catalog.service;

import com.microfocus.example.catalog.repository.ProductRepository;
import com.microfocus.example.contracts.model.Product;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Product Catalog Service
 */
@Service
public class CatalogService {
    
    private final ProductRepository productRepository;
    
    public CatalogService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }
    
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }
    
    public Product getProductById(Long id) {
        return productRepository.findById(id);
    }
    
    public List<Product> searchProducts(String searchTerm) {
        return productRepository.searchProducts(searchTerm);
    }
    
    public List<Product> getProductsByCategory(String category) {
        return productRepository.findByCategory(category);
    }
    
    public List<Product> getAllProductsSorted(String sortBy, String order) {
        return productRepository.findAllSorted(sortBy, order);
    }

    // New CRUD operations
    public Product createProduct(Product product) {
        return productRepository.save(product);
    }

    public Product updateProduct(Long id, Product product) {
        return productRepository.update(id, product);
    }

    public boolean deleteProduct(Long id) {
        return productRepository.delete(id);
    }
}
