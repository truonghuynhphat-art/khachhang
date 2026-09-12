package com.bank.service;

import com.bank.entity.*;
import com.bank.exception.BusinessException;
import com.bank.repository.AccountRepository;
import com.bank.repository.RecurringTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecurringTransactionServiceTest {

    @Mock private RecurringTransactionRepository recurringRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private TransactionService transactionService;

    @InjectMocks
    private RecurringTransactionService recurringTransactionService;

    private Account account;
    private static final Long OWNER_CUSTOMER_ID = 2L;

    @BeforeEach
    void setUp() {
        CustomerType type = new CustomerType(CustomerTypeName.INDIVIDUAL, new BigDecimal("200000"));
        Customer customer = new Customer("Ann", "Hanoi", type);
        // Customer.id chỉ được JPA gán khi persist thật; set thủ công qua reflection
        // để mô phỏng "customer này đã có id = 2" trong môi trường unit test (không có DB).
        ReflectionTestUtils.setField(customer, "id", OWNER_CUSTOMER_ID);
        account = new Account("100002", new BigDecimal("500000"), customer,
                new BigDecimal("300000"), LocalDate.now());
    }

    @Test
    void create_success_whenAdmin_forAnyAccount() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(recurringRepository.save(any(RecurringTransaction.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        RecurringTransaction result = recurringTransactionService.create(
                1L, "DEPOSIT", new BigDecimal("50000"), "Hanoi", Frequency.DAILY,
                /* requesterCustomerId */ 999L, /* isAdmin */ true);

        assertEquals("DEPOSIT", result.getType());
        assertNotNull(result.getNextRunDate());
        assertTrue(result.isActive());
    }

    @Test
    void create_throws_whenCustomerIsNotOwner() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> recurringTransactionService.create(
                        1L, "DEPOSIT", new BigDecimal("50000"), "Hanoi", Frequency.DAILY,
                        /* requesterCustomerId khác chủ tài khoản */ 999L, /* isAdmin */ false));

        assertTrue(ex.getMessage().contains("own account"));
        verify(recurringRepository, never()).save(any());
    }

    @Test
    void create_throws_whenTypeIsInvalid() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));

        assertThrows(BusinessException.class,
                () -> recurringTransactionService.create(
                        1L, "TRANSFER", new BigDecimal("50000"), "Hanoi", Frequency.DAILY, 999L, true));
    }

    @Test
    void create_throws_whenAmountIsZeroOrNegative() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));

        assertThrows(BusinessException.class,
                () -> recurringTransactionService.create(
                        1L, "DEPOSIT", BigDecimal.ZERO, "Hanoi", Frequency.DAILY, 999L, true));
    }

    @Test
    void create_throws_whenAccountNotFound() {
        when(accountRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class,
                () -> recurringTransactionService.create(
                        99L, "DEPOSIT", new BigDecimal("1000"), "Hanoi", Frequency.DAILY, 1L, false));
    }

    @Test
    void findMine_returnsAllForAdmin() {
        when(recurringRepository.findAll()).thenReturn(java.util.List.of());

        recurringTransactionService.findMine(1L, true);

        verify(recurringRepository).findAll();
        verify(recurringRepository, never()).findByAccountOwnerId(any());
    }

    @Test
    void findMine_filtersByOwnerForCustomer() {
        when(recurringRepository.findByAccountOwnerId(OWNER_CUSTOMER_ID)).thenReturn(java.util.List.of());

        recurringTransactionService.findMine(OWNER_CUSTOMER_ID, false);

        verify(recurringRepository).findByAccountOwnerId(OWNER_CUSTOMER_ID);
        verify(recurringRepository, never()).findAll();
    }

    @Test
    void cancel_throws_whenNotOwnerAndNotAdmin() {
        RecurringTransaction rt = new RecurringTransaction(account, "DEPOSIT", new BigDecimal("1000"),
                "Hanoi", Frequency.DAILY, java.time.LocalDateTime.now());
        when(recurringRepository.findById(1L)).thenReturn(Optional.of(rt));

        assertThrows(BusinessException.class,
                () -> recurringTransactionService.cancel(1L, 999L, false));

        verify(recurringRepository, never()).save(any());
    }
}
