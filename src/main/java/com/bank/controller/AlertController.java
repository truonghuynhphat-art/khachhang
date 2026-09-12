package com.bank.controller;

import com.bank.entity.Alert;
import com.bank.entity.AlertStatus;
import com.bank.exception.BusinessException;
import com.bank.repository.AlertRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    private final AlertRepository alertRepository;

    public AlertController(AlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    @GetMapping
    public List<Alert> getAll() {
        return alertRepository.findAll();
    }

    @PatchMapping("/{id}/review")
    public Alert review(@PathVariable Long id) {
        Alert alert = alertRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Alert not found", HttpStatus.NOT_FOUND));
        alert.setStatus(AlertStatus.REVIEWED);
        return alertRepository.save(alert);
    }
}