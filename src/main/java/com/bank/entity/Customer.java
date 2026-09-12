package com.bank.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "customer")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String location;

    @ManyToOne
    @JoinColumn(name = "customer_type_id", nullable = false)
    private CustomerType customerType;

    public Customer() {}

    public Customer(String name, String location, CustomerType customerType) {
        this.name = name;
        this.location = location;
        this.customerType = customerType;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public CustomerType getCustomerType() { return customerType; }
    public void setCustomerType(CustomerType customerType) { this.customerType = customerType; }
}