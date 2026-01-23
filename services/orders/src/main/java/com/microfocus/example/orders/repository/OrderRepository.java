package com.microfocus.example.orders.repository;

import com.microfocus.example.contracts.model.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Order Repository
 * WARNING: Contains IDOR vulnerabilities - no authorization checks
 */
@Repository
public class OrderRepository {
    
    private final JdbcTemplate jdbcTemplate;
    
    public OrderRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        initializeDatabase();
    }
    
    private void initializeDatabase() {
        jdbcTemplate.execute(
            "CREATE TABLE IF NOT EXISTS orders (" +
            "id BIGINT PRIMARY KEY AUTO_INCREMENT, " +
            "customer_id BIGINT, " +
            "order_number VARCHAR(50) UNIQUE, " +
            "order_date TIMESTAMP, " +
            "status VARCHAR(50), " +
            "total_amount DECIMAL(10,2), " +
            "shipping_address VARCHAR(500), " +
            "billing_address VARCHAR(500), " +
            "payment_method VARCHAR(50), " +
            "tracking_number VARCHAR(100))"
        );
        
        // Insert sample orders
        String insertSql = "INSERT INTO orders (customer_id, order_number, order_date, status, " +
                          "total_amount, shipping_address, payment_method) VALUES " +
                          "(1, 'ORD-2024-001', CURRENT_TIMESTAMP, 'DELIVERED', 34.98, '123 Main St', 'Credit Card'), " +
                          "(2, 'ORD-2024-002', CURRENT_TIMESTAMP, 'PROCESSING', 24.99, '456 Oak Ave', 'PayPal'), " +
                          "(3, 'ORD-2024-003', CURRENT_TIMESTAMP, 'SHIPPED', 19.99, '789 Elm Rd', 'Credit Card')";
        
        try {
            jdbcTemplate.execute(insertSql);
        } catch (Exception e) {
            // Data already exists
        }
    }
    
    /**
     * VULNERABILITY: IDOR - No authorization check, any user can access any order
     */
    public Order findById(Long id) {
        String sql = "SELECT * FROM orders WHERE id = ?";
        List<Order> orders = jdbcTemplate.query(sql, new OrderRowMapper(), id);
        return orders.isEmpty() ? null : orders.get(0);
    }
    
    /**
     * VULNERABILITY: IDOR - Returns all orders regardless of user
     */
    public List<Order> findByCustomerId(Long customerId) {
        String sql = "SELECT * FROM orders WHERE customer_id = ? ORDER BY order_date DESC";
        return jdbcTemplate.query(sql, new OrderRowMapper(), customerId);
    }
    
    public List<Order> findAll() {
        String sql = "SELECT * FROM orders ORDER BY order_date DESC";
        return jdbcTemplate.query(sql, new OrderRowMapper());
    }
    
    public Order createOrder(Order order) {
        String sql = "INSERT INTO orders (customer_id, order_number, order_date, status, " +
                    "total_amount, shipping_address, billing_address, payment_method) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        
        jdbcTemplate.update(sql,
            order.getCustomerId(),
            order.getOrderNumber(),
            LocalDateTime.now(),
            "PENDING",
            order.getTotalAmount(),
            order.getShippingAddress(),
            order.getBillingAddress(),
            order.getPaymentMethod()
        );
        
        String selectSql = "SELECT * FROM orders WHERE order_number = ?";
        List<Order> orders = jdbcTemplate.query(selectSql, new OrderRowMapper(), order.getOrderNumber());
        return orders.isEmpty() ? null : orders.get(0);
    }
    
    /**
     * VULNERABILITY: IDOR - No check if order belongs to customer
     */
    public void updateOrderStatus(Long orderId, String status) {
        String sql = "UPDATE orders SET status = ? WHERE id = ?";
        jdbcTemplate.update(sql, status, orderId);
    }
    
    /**
     * VULNERABILITY: SQL Injection
     * This method builds SQL by concatenating the user-supplied query string
     * directly into the SQL statement. An attacker can inject SQL via the `q`
     * parameter.
     *
     * SECURE ALTERNATIVES:
     * - Use parameterized queries with `?` and pass values to `jdbcTemplate.query`.
     * - Use a prepared statement or an ORM that parameterizes inputs.
     */
    public List<Order> searchByQuery(String q) {
        String sql = "SELECT * FROM orders WHERE " +
                     "order_number LIKE '%" + q + "%' OR " +
                     "shipping_address LIKE '%" + q + "%' OR " +
                     "billing_address LIKE '%" + q + "%' OR " +
                     "payment_method LIKE '%" + q + "%' " +
                     "ORDER BY order_date DESC";

        return jdbcTemplate.query(sql, new OrderRowMapper());
    }
    
    private static class OrderRowMapper implements RowMapper<Order> {
        @Override
        public Order mapRow(ResultSet rs, int rowNum) throws SQLException {
            Order order = new Order();
            order.setId(rs.getLong("id"));
            order.setCustomerId(rs.getLong("customer_id"));
            order.setOrderNumber(rs.getString("order_number"));
            order.setStatus(rs.getString("status"));
            order.setTotalAmount(rs.getBigDecimal("total_amount"));
            order.setShippingAddress(rs.getString("shipping_address"));
            order.setBillingAddress(rs.getString("billing_address"));
            order.setPaymentMethod(rs.getString("payment_method"));
            order.setTrackingNumber(rs.getString("tracking_number"));
            
            java.sql.Timestamp orderDateTs = rs.getTimestamp("order_date");
            if (orderDateTs != null) {
                order.setOrderDate(orderDateTs.toLocalDateTime());
            }
            
            return order;
        }
    }
}
