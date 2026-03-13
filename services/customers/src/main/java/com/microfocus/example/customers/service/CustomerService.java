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

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
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

    public Customer updateCustomer(Customer customer) {
        return customerRepository.updateCustomer(customer);
    }
}
