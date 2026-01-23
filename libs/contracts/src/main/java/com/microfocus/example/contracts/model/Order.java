package com.microfocus.example.contracts.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Order entity
 * WARNING: Deliberately vulnerable - insecure direct object reference
 */
public class Order implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Long id;
    private Long customerId;
    private String orderNumber;
    private LocalDateTime orderDate;
    private String status; // PENDING, PROCESSING, SHIPPED, DELIVERED, CANCELLED
    private BigDecimal totalAmount;
    private String shippingAddress;
    private String billingAddress;
    private String paymentMethod;
    private String trackingNumber;
    private List<OrderItem> items;
    
    public Order() {}
    
    public Order(Long id, Long customerId, String orderNumber, BigDecimal totalAmount) {
        this.id = id;
        this.customerId = customerId;
        this.orderNumber = orderNumber;
        this.totalAmount = totalAmount;
        this.orderDate = LocalDateTime.now();
        this.status = "PENDING";
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    
    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }
    
    public LocalDateTime getOrderDate() { return orderDate; }
    public void setOrderDate(LocalDateTime orderDate) { this.orderDate = orderDate; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    
    public String getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(String shippingAddress) { this.shippingAddress = shippingAddress; }
    
    public String getBillingAddress() { return billingAddress; }
    public void setBillingAddress(String billingAddress) { this.billingAddress = billingAddress; }
    
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    
    public String getTrackingNumber() { return trackingNumber; }
    public void setTrackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber; }
    
    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }
}
