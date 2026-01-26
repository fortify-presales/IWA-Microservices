package com.microfocus.example.customers.service;

import com.microfocus.example.contracts.model.Customer;
import com.microfocus.example.customers.repository.CustomerRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Customer Service
 */
@Service
public class CustomerService {
    
    private final CustomerRepository customerRepository;
    private final JwtService jwtService;
    
    public CustomerService(CustomerRepository customerRepository, JwtService jwtService) {
        this.customerRepository = customerRepository;
        this.jwtService = jwtService;
    }
    
    public String authenticateAndGenerateToken(String username, String password) {
        Customer customer = customerRepository.authenticateUser(username, password);
        
        if (customer != null) {
            customerRepository.updateLastLogin(customer.getId());
            return jwtService.generateToken(customer.getUsername(), customer.getId());
        }
        
        return null;
    }
    
    public Customer getCustomerById(Long id) {
        return customerRepository.findById(id);
    }
    
    public Customer getCustomerByUsername(String username) {
        return customerRepository.findByUsername(username);
    }
    
    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }
    
    public Customer registerCustomer(Customer customer) {
        return customerRepository.createCustomer(customer);
    }
    
    public boolean validateToken(String token, String username) {
        return jwtService.validateToken(token, username);
    }
    
    public String extractUsernameFromToken(String token) {
        return jwtService.extractUsername(token);
    }

    public Customer updateCustomer(Customer customer) {
        return customerRepository.updateCustomer(customer);
    }
}
