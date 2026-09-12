package com.bank.controller;

import com.bank.entity.CustomerType;
import com.bank.repository.CustomerTypeRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customer-types")
public class CustomerTypeController {

    private final CustomerTypeRepository customerTypeRepository;

    public CustomerTypeController(CustomerTypeRepository customerTypeRepository) {
        this.customerTypeRepository = customerTypeRepository;
    }

    @GetMapping
    public List<CustomerType> getAll() {
        return customerTypeRepository.findAll();
    }
}