package com.bank.service;

import com.bank.entity.Customer;
import com.bank.exception.BusinessException;
import com.bank.repository.CustomerRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }

    public Optional<Customer> getCustomerById(Long id) {
        return customerRepository.findById(id);
    }

    public List<Customer> search(String name, String location) {
        if (name != null) {
            return customerRepository.findByNameContainingIgnoreCase(name);
        }
        if (location != null) {
            return customerRepository.findByLocationIgnoreCase(location);
        }
        return customerRepository.findAll();
    }

    public Customer createCustomer(Customer customer) {
        return customerRepository.save(customer);
    }

    public Customer updateCustomer(Long id, Customer customer) {
        Customer existing = customerRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Customer not found", HttpStatus.NOT_FOUND));
        existing.setName(customer.getName());
        existing.setLocation(customer.getLocation());
        return customerRepository.save(existing);
    }

    public void deleteCustomer(Long id) {
        if (!customerRepository.existsById(id)) {
            throw new BusinessException("Customer not found", HttpStatus.NOT_FOUND);
        }
        customerRepository.deleteById(id);
    }
}