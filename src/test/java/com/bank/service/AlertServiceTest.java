package com.bank.service;

import com.bank.entity.*;
import com.bank.repository.AlertRepository;
import com.bank.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * AlertService không phải bean được inject qua @InjectMocks vì các tham số
 * ngưỡng (amountThreshold, rapidCount, rapidWindowSeconds) đến từ @Value trong
 * application.properties - ở đây khởi tạo trực tiếp qua constructor với giá trị
 * cố định để test độc lập với cấu hình Spring.
 */
@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    @Mock private AlertRepository alertRepository;
    @Mock private TransactionRepository transactionRepository;

    private AlertService alertService;
    private Account account;

    private static final BigDecimal AMOUNT_THRESHOLD = new BigDecimal("5000000");
    private static final int RAPID_COUNT = 3;
    private static final int RAPID_WINDOW_SECONDS = 60;

    @BeforeEach
    void setUp() {
        alertService = new AlertService(alertRepository, transactionRepository,
                AMOUNT_THRESHOLD, RAPID_COUNT, RAPID_WINDOW_SECONDS);

        CustomerType type = new CustomerType(CustomerTypeName.INDIVIDUAL, new BigDecimal("200000"));
        Customer customer = new Customer("Ann", "Hanoi", type);
        account = new Account("100002", new BigDecimal("500000"), customer,
                new BigDecimal("300000"), LocalDate.now());
    }

    @Test
    void checkAndFlag_createsAlert_whenAmountExceedsThreshold() {
        Transaction tx = new Transaction(account, "DEPOSIT", new BigDecimal("6000000"), BigDecimal.ZERO, "Hanoi");
        when(transactionRepository.countByAccountIdAndCreatedAtAfter(any(), any())).thenReturn(1L);

        alertService.checkAndFlag(tx);

        verify(alertRepository).save(argThat(alert -> alert.getType() == AlertType.AMOUNT_THRESHOLD));
    }

    @Test
    void checkAndFlag_noAlert_whenAmountBelowThreshold() {
        Transaction tx = new Transaction(account, "DEPOSIT", new BigDecimal("100000"), BigDecimal.ZERO, "Hanoi");
        when(transactionRepository.countByAccountIdAndCreatedAtAfter(any(), any())).thenReturn(1L);

        alertService.checkAndFlag(tx);

        verify(alertRepository, never()).save(argThat(alert -> alert.getType() == AlertType.AMOUNT_THRESHOLD));
    }

    @Test
    void checkAndFlag_createsAlert_whenRapidSuccessionExceedsLimit() {
        Transaction tx = new Transaction(account, "DEPOSIT", new BigDecimal("1000"), BigDecimal.ZERO, "Hanoi");
        // 4 giao dịch trong khung thời gian, ngưỡng cho phép tối đa 3 -> phải bị flag
        when(transactionRepository.countByAccountIdAndCreatedAtAfter(any(), any())).thenReturn(4L);

        alertService.checkAndFlag(tx);

        verify(alertRepository).save(argThat(alert -> alert.getType() == AlertType.RAPID_SUCCESSION));
    }

    @Test
    void checkAndFlag_noAlert_whenTransactionCountWithinLimit() {
        Transaction tx = new Transaction(account, "DEPOSIT", new BigDecimal("1000"), BigDecimal.ZERO, "Hanoi");
        when(transactionRepository.countByAccountIdAndCreatedAtAfter(any(), any())).thenReturn(2L);

        alertService.checkAndFlag(tx);

        verify(alertRepository, never()).save(any());
    }

    @Test
    void checkAndFlag_neverThrows_evenWhenBothConditionsTrigger() {
        // Giao dịch vừa vượt ngưỡng số tiền, vừa là giao dịch thứ 5 liên tiếp -> 2 alert, không throw
        Transaction tx = new Transaction(account, "DEPOSIT", new BigDecimal("6000000"), BigDecimal.ZERO, "Hanoi");
        when(transactionRepository.countByAccountIdAndCreatedAtAfter(any(), any())).thenReturn(5L);

        alertService.checkAndFlag(tx);

        verify(alertRepository, times(2)).save(any());
    }
}
