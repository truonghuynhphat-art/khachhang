package com.bank.repository;

import com.bank.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// public interface CustomerRepository extends JpaRepository<Customer, Long> {
// }

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    List<Customer> findByNameContainingIgnoreCase(String name);
    List<Customer> findByLocationIgnoreCase(String location);
}