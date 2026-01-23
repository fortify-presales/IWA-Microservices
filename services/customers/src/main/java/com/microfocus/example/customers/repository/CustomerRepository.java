package com.microfocus.example.customers.repository;

import com.microfocus.example.contracts.model.Customer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Customer Repository
 * WARNING: Stores passwords in plain text - intentional vulnerability
 */
@Repository
public class CustomerRepository {
    
    private final JdbcTemplate jdbcTemplate;
    
    public CustomerRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        initializeDatabase();
    }
    
    private void initializeDatabase() {
        jdbcTemplate.execute(
            "CREATE TABLE IF NOT EXISTS customers (" +
            "id BIGINT PRIMARY KEY AUTO_INCREMENT, " +
            "username VARCHAR(100) UNIQUE, " +
            "password VARCHAR(100), " + // Plain text password storage
            "email VARCHAR(255), " +
            "first_name VARCHAR(100), " +
            "last_name VARCHAR(100), " +
            "phone VARCHAR(20), " +
            "address VARCHAR(255), " +
            "city VARCHAR(100), " +
            "state VARCHAR(50), " +
            "zip_code VARCHAR(20), " +
            "created_at TIMESTAMP, " +
            "last_login TIMESTAMP, " +
            "is_active BOOLEAN)"
        );
        
        // Insert test users with plain text passwords
        String insertSql = "INSERT INTO customers (username, password, email, first_name, last_name, " +
                          "phone, created_at, is_active) VALUES " +
                          "('admin', 'admin123', 'admin@pharmacy.com', 'Admin', 'User', '555-0001', CURRENT_TIMESTAMP, true), " +
                          "('john.doe', 'password123', 'john.doe@example.com', 'John', 'Doe', '555-0002', CURRENT_TIMESTAMP, true), " +
                          "('jane.smith', 'pass1234', 'jane.smith@example.com', 'Jane', 'Smith', '555-0003', CURRENT_TIMESTAMP, true)";
        
        try {
            jdbcTemplate.execute(insertSql);
        } catch (Exception e) {
            // Data already exists
        }
    }
    
    /**
     * VULNERABILITY: SQL Injection via string concatenation
     * This method is intentionally vulnerable for demonstration purposes
     * 
     * SECURE ALTERNATIVE: Use parameterized queries instead:
     * String sql = "SELECT * FROM customers WHERE username = ? AND password = ?";
     * List<Customer> customers = jdbcTemplate.query(sql, new CustomerRowMapper(), username, password);
     * 
     * Also, passwords should NEVER be stored in plain text. Use bcrypt or similar:
     * BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
     * encoder.matches(password, customer.getPassword());
     */
    public Customer authenticateUser(String username, String password) {
        // Intentionally vulnerable - SQL injection and plain text passwords
        String sql = "SELECT * FROM customers WHERE username = '" + username + 
                    "' AND password = '" + password + "'";
        
        List<Customer> customers = jdbcTemplate.query(sql, new CustomerRowMapper());
        return customers.isEmpty() ? null : customers.get(0);
    }
    
    public Customer findByUsername(String username) {
        String sql = "SELECT * FROM customers WHERE username = ?";
        List<Customer> customers = jdbcTemplate.query(sql, new CustomerRowMapper(), username);
        return customers.isEmpty() ? null : customers.get(0);
    }
    
    public Customer findById(Long id) {
        String sql = "SELECT * FROM customers WHERE id = ?";
        List<Customer> customers = jdbcTemplate.query(sql, new CustomerRowMapper(), id);
        return customers.isEmpty() ? null : customers.get(0);
    }
    
    public List<Customer> findAll() {
        String sql = "SELECT * FROM customers ORDER BY username";
        return jdbcTemplate.query(sql, new CustomerRowMapper());
    }
    
    /**
     * VULNERABILITY: Creates customer with plain text password
     */
    public Customer createCustomer(Customer customer) {
        String sql = "INSERT INTO customers (username, password, email, first_name, last_name, " +
                    "phone, address, city, state, zip_code, created_at, is_active) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        jdbcTemplate.update(sql, 
            customer.getUsername(),
            customer.getPassword(), // Stored in plain text
            customer.getEmail(),
            customer.getFirstName(),
            customer.getLastName(),
            customer.getPhone(),
            customer.getAddress(),
            customer.getCity(),
            customer.getState(),
            customer.getZipCode(),
            LocalDateTime.now(),
            true
        );
        
        return findByUsername(customer.getUsername());
    }
    
    public void updateLastLogin(Long customerId) {
        String sql = "UPDATE customers SET last_login = ? WHERE id = ?";
        jdbcTemplate.update(sql, LocalDateTime.now(), customerId);
    }
    
    private static class CustomerRowMapper implements RowMapper<Customer> {
        @Override
        public Customer mapRow(ResultSet rs, int rowNum) throws SQLException {
            Customer customer = new Customer();
            customer.setId(rs.getLong("id"));
            customer.setUsername(rs.getString("username"));
            customer.setPassword(rs.getString("password")); // Returns plain text password
            customer.setEmail(rs.getString("email"));
            customer.setFirstName(rs.getString("first_name"));
            customer.setLastName(rs.getString("last_name"));
            customer.setPhone(rs.getString("phone"));
            customer.setAddress(rs.getString("address"));
            customer.setCity(rs.getString("city"));
            customer.setState(rs.getString("state"));
            customer.setZipCode(rs.getString("zip_code"));
            customer.setIsActive(rs.getBoolean("is_active"));
            
            java.sql.Timestamp createdTs = rs.getTimestamp("created_at");
            if (createdTs != null) {
                customer.setCreatedAt(createdTs.toLocalDateTime());
            }
            
            java.sql.Timestamp lastLoginTs = rs.getTimestamp("last_login");
            if (lastLoginTs != null) {
                customer.setLastLogin(lastLoginTs.toLocalDateTime());
            }
            
            return customer;
        }
    }
}
