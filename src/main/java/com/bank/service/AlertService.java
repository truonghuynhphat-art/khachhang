package com.bank.service;

import com.bank.entity.Alert;
import com.bank.entity.AlertType;
import com.bank.entity.Transaction;
import com.bank.repository.AlertRepository;
import com.bank.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class AlertService {

    private final AlertRepository alertRepository;
    private final TransactionRepository transactionRepository;

    private final BigDecimal amountThreshold;
    private final int rapidCount;
    private final int rapidWindowSeconds;

    public AlertService(AlertRepository alertRepository,
                         TransactionRepository transactionRepository,
                         @Value("${app.alert.amount-threshold}") BigDecimal amountThreshold,
                         @Value("${app.alert.rapid-count}") int rapidCount,
                         @Value("${app.alert.rapid-window-seconds}") int rapidWindowSeconds) {
        this.alertRepository = alertRepository;
        this.transactionRepository = transactionRepository;
        this.amountThreshold = amountThreshold;
        this.rapidCount = rapidCount;
        this.rapidWindowSeconds = rapidWindowSeconds;
    }

    /** Gọi sau khi 1 transaction đã được lưu thành công. Không throw - không được làm hỏng giao dịch gốc. */
    public void checkAndFlag(Transaction tx) {
        if (tx.getAmount().compareTo(amountThreshold) > 0) {
            String desc = String.format("Amount %s exceeds threshold %s", tx.getAmount(), amountThreshold);
            alertRepository.save(new Alert(tx, AlertType.AMOUNT_THRESHOLD, desc));
        }

        LocalDateTime cutoff = tx.getCreatedAt().minusSeconds(rapidWindowSeconds);
        long recentCount = transactionRepository.countByAccountIdAndCreatedAtAfter(
                tx.getAccount().getId(), cutoff);

        if (recentCount > rapidCount) {
            String desc = String.format("%d transactions on account %d within %d seconds",
                    recentCount, tx.getAccount().getId(), rapidWindowSeconds);
            alertRepository.save(new Alert(tx, AlertType.RAPID_SUCCESSION, desc));
        }
    }
}