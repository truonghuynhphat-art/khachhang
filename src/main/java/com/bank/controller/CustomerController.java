package com.bank.controller;

import com.bank.entity.Customer;
import com.bank.service.CustomerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.bank.repository.UserRepository;
import com.bank.entity.User;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;
    private final UserRepository userRepository;

    // public CustomerController(CustomerService customerService) {
    //     this.customerService = customerService;
    // }

    public CustomerController(CustomerService customerService, UserRepository userRepository) {
        this.customerService = customerService;
        this.userRepository = userRepository;
    }

    @GetMapping
    // public List<Customer> getAll() { return customerService.getAllCustomers(); }
    public List<Customer> getAll() {
        if (isAdmin()) {
            return customerService.getAllCustomers();
        }
        return currentCustomer().map(List::of).orElse(List.of());
    }


    @GetMapping("/search")
    public List<Customer> search(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String location) {
        return customerService.search(name, location);
    }

    @GetMapping("/{id}")
    // public Optional<Customer> getById(@PathVariable Long id) { return customerService.getCustomerById(id); }
    public ResponseEntity<Customer> getById(@PathVariable Long id) {
        if (!isAdmin() && !ownsCustomerId(id)) {
            return ResponseEntity.status(403).build();
        }
        return customerService.getCustomerById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    private boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    private Optional<Customer> currentCustomer() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .map(User::getCustomer)
                .filter(c -> c != null);
    }

    private boolean ownsCustomerId(Long customerId) {
        return currentCustomer().map(c -> c.getId().equals(customerId)).orElse(false);
    }    

    @PostMapping
    public Customer create(@RequestBody Customer customer) { return customerService.createCustomer(customer); }

    @PutMapping("/{id}")
    public Customer update(@PathVariable Long id, @RequestBody Customer customer) {
        return customerService.updateCustomer(id, customer);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) { customerService.deleteCustomer(id); }
}