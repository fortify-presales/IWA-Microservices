package com.microfocus.example.orders.service;

import com.microfocus.example.contracts.model.Order;
import com.microfocus.example.orders.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.Base64;
import java.util.List;

/**
 * Order Service
 * WARNING: Contains insecure deserialization vulnerability
 */
@Service
public class OrderService {
    
    private final OrderRepository orderRepository;
    
    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }
    
    public Order getOrderById(Long id) {
        return orderRepository.findById(id);
    }
    
    public List<Order> getOrdersByCustomerId(Long customerId) {
        return orderRepository.findByCustomerId(customerId);
    }
    
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }
    
    public Order createOrder(Order order) {
        // Generate order number
        String orderNumber = "ORD-" + System.currentTimeMillis();
        order.setOrderNumber(orderNumber);
        return orderRepository.createOrder(order);
    }
    
    public void updateOrderStatus(Long orderId, String status) {
        orderRepository.updateOrderStatus(orderId, status);
    }

    /**
     * VULNERABILITY: SQL Injection (propagated)
     * Delegates search to repository which constructs SQL via string concatenation.
     */
    public List<Order> searchOrders(String q) {
        return orderRepository.searchByQuery(q);
    }
    
    /**
     * VULNERABILITY: Insecure Deserialization
     * This method deserializes user-supplied data without validation
     * Can lead to Remote Code Execution (RCE) attacks
     * 
     * SECURE ALTERNATIVES:
     * 1. Use JSON instead of Java serialization:
     *    ObjectMapper mapper = new ObjectMapper();
     *    Order order = mapper.readValue(jsonString, Order.class);
     * 
     * 2. If Java serialization is required, implement whitelist validation:
     *    - Use ObjectInputFilter (Java 9+)
     *    - Validate class types before deserialization
     *    - Implement custom ObjectInputStream with resolveClass override
     * 
     * 3. Use secure serialization libraries like Protocol Buffers or MessagePack
     * 
     * 4. Sign and encrypt serialized data to prevent tampering
     */
    public Order deserializeOrder(String serializedData) throws IOException, ClassNotFoundException {
        byte[] data = Base64.getDecoder().decode(serializedData);
        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        ObjectInputStream ois = new ObjectInputStream(bis);
        
        // Intentionally vulnerable - deserializes without validation
        Object obj = ois.readObject();
        ois.close();
        
        if (obj instanceof Order) {
            return (Order) obj;
        }
        
        throw new IllegalArgumentException("Invalid order data");
    }
    
    /**
     * Serialize order to Base64 string
     */
    public String serializeOrder(Order order) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(order);
        oos.close();
        
        return Base64.getEncoder().encodeToString(bos.toByteArray());
    }
}
