package com.microfocus.example.catalog.repository;

import com.microfocus.example.contracts.model.Product;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Product Repository with intentional SQL injection vulnerabilities
 * WARNING: Uses string concatenation for SQL queries - DO NOT USE IN PRODUCTION
 */
@Repository
public class ProductRepository {
    
    private final JdbcTemplate jdbcTemplate;
    
    public ProductRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        initializeDatabase();
    }
    
    private void initializeDatabase() {
        jdbcTemplate.execute(
            "CREATE TABLE IF NOT EXISTS products (" +
            "id BIGINT PRIMARY KEY AUTO_INCREMENT, " +
            "name VARCHAR(255), " +
            "description TEXT, " +
            "category VARCHAR(100), " +
            "price DECIMAL(10,2), " +
            "stock_quantity INT, " +
            "image_url VARCHAR(500), " +
            "requires_prescription BOOLEAN, " +
            "manufacturer VARCHAR(255), " +
            "created_at TIMESTAMP, " +
            "updated_at TIMESTAMP)"
        );
        
        // Insert sample data
        String insertSql = "INSERT INTO products (name, description, category, price, stock_quantity, " +
                          "requires_prescription, manufacturer, created_at, updated_at) VALUES " +
                          "('Aspirin 500mg', 'Pain reliever and fever reducer', 'Pain Relief', 9.99, 100, false, 'PharmaCorp', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP), " +
                          "('Amoxicillin 250mg', 'Antibiotic for bacterial infections', 'Antibiotics', 24.99, 50, true, 'MediPharm', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP), " +
                          "('Ibuprofen 200mg', 'Anti-inflammatory pain reliever', 'Pain Relief', 12.99, 150, false, 'HealthCo', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP), " +
                          "('Lisinopril 10mg', 'Blood pressure medication', 'Cardiovascular', 19.99, 75, true, 'CardioMed', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP), " +
                          "('Vitamin D3 1000IU', 'Vitamin D supplement', 'Vitamins', 14.99, 200, false, 'NutriLife', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";
        
        try {
            jdbcTemplate.execute(insertSql);
        } catch (Exception e) {
            // Data already exists
        }
    }
    
    /**
     * VULNERABILITY: SQL Injection via string concatenation
     * This method is intentionally vulnerable for demonstration purposes
     */
    public List<Product> searchProducts(String searchTerm) {
        // Intentionally vulnerable - concatenating user input directly into SQL
        String sql = "SELECT * FROM products WHERE name LIKE '%" + searchTerm + "%' " +
                    "OR description LIKE '%" + searchTerm + "%' " +
                    "OR category LIKE '%" + searchTerm + "%'";
        
        return jdbcTemplate.query(sql, new ProductRowMapper());
    }
    
    /**
     * VULNERABILITY: SQL Injection via string concatenation
     */
    public List<Product> findByCategory(String category) {
        // Intentionally vulnerable
        String sql = "SELECT * FROM products WHERE category = '" + category + "'";
        return jdbcTemplate.query(sql, new ProductRowMapper());
    }
    
    public List<Product> findAll() {
        String sql = "SELECT * FROM products ORDER BY name";
        return jdbcTemplate.query(sql, new ProductRowMapper());
    }
    
    public Product findById(Long id) {
        String sql = "SELECT * FROM products WHERE id = ?";
        List<Product> products = jdbcTemplate.query(sql, new ProductRowMapper(), id);
        return products.isEmpty() ? null : products.get(0);
    }
    
    /**
     * VULNERABILITY: SQL Injection via dynamic ORDER BY
     */
    public List<Product> findAllSorted(String sortField, String sortOrder) {
        // Intentionally vulnerable - allows SQL injection through ORDER BY
        String sql = "SELECT * FROM products ORDER BY " + sortField + " " + sortOrder;
        return jdbcTemplate.query(sql, new ProductRowMapper());
    }

    // --------- New CRUD methods (intentionally simple for demo) ---------
    public Product save(Product product) {
        String sql = "INSERT INTO products (name, description, category, price, stock_quantity, image_url, requires_prescription, manufacturer, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[] {"id"});
            ps.setString(1, product.getName());
            ps.setString(2, product.getDescription());
            ps.setString(3, product.getCategory());
            ps.setBigDecimal(4, product.getPrice() == null ? BigDecimal.ZERO : product.getPrice());
            ps.setInt(5, product.getStockQuantity() == null ? 0 : product.getStockQuantity());
            ps.setString(6, product.getImageUrl());
            ps.setBoolean(7, product.getRequiresPrescription() == null ? false : product.getRequiresPrescription());
            ps.setString(8, product.getManufacturer());
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            return findById(key.longValue());
        }
        return null;
    }

    public Product update(Long id, Product product) {
        String sql = "UPDATE products SET name = ?, description = ?, category = ?, price = ?, stock_quantity = ?, image_url = ?, requires_prescription = ?, manufacturer = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        jdbcTemplate.update(sql,
                product.getName(),
                product.getDescription(),
                product.getCategory(),
                product.getPrice() == null ? BigDecimal.ZERO : product.getPrice(),
                product.getStockQuantity() == null ? 0 : product.getStockQuantity(),
                product.getImageUrl(),
                product.getRequiresPrescription() == null ? false : product.getRequiresPrescription(),
                product.getManufacturer(),
                id);
        return findById(id);
    }

    public boolean delete(Long id) {
        String sql = "DELETE FROM products WHERE id = ?";
        int rows = jdbcTemplate.update(sql, id);
        return rows > 0;
    }

    private static class ProductRowMapper implements RowMapper<Product> {
        @Override
        public Product mapRow(ResultSet rs, int rowNum) throws SQLException {
            Product product = new Product();
            product.setId(rs.getLong("id"));
            product.setName(rs.getString("name"));
            product.setDescription(rs.getString("description"));
            product.setCategory(rs.getString("category"));
            product.setPrice(rs.getBigDecimal("price"));
            product.setStockQuantity(rs.getInt("stock_quantity"));
            product.setImageUrl(rs.getString("image_url"));
            product.setRequiresPrescription(rs.getBoolean("requires_prescription"));
            product.setManufacturer(rs.getString("manufacturer"));
            
            java.sql.Timestamp createdTs = rs.getTimestamp("created_at");
            if (createdTs != null) {
                product.setCreatedAt(createdTs.toLocalDateTime());
            }
            
            java.sql.Timestamp updatedTs = rs.getTimestamp("updated_at");
            if (updatedTs != null) {
                product.setUpdatedAt(updatedTs.toLocalDateTime());
            }
            
            return product;
        }
    }
}
