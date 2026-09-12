package com.bank.repository;

import com.bank.entity.CustomerType;
import com.bank.entity.CustomerTypeName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerTypeRepository extends JpaRepository<CustomerType, Long> {
    Optional<CustomerType> findByName(CustomerTypeName name);
}