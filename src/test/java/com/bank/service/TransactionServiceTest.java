package com.bank.service;

import com.bank.entity.*;
import com.bank.exception.BusinessException;
import com.bank.repository.AccountRepository;
import com.bank.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock private TransactionRepository transactionRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private AlertService alertService;

    @InjectMocks
    private TransactionService transactionService;

    private Account account;
    private CustomerType individualType;
    private Customer customer;

    @BeforeEach
    void setUp() {
        individualType = new CustomerType(CustomerTypeName.INDIVIDUAL, new BigDecimal("200000"));
        customer = new Customer("Ann", "Hanoi", individualType);
        account = new Account("100002", new BigDecimal("500000"), customer,
                new BigDecimal("300000"), LocalDate.now());

        // Trả về đúng entity được truyền vào khi save (giữ nguyên hành vi thật của repository)
        // when(transactionRepository.save(any(Transaction.class)))
        lenient().when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void withdraw_success_whenWithinBothLimits() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));

        Transaction result = transactionService.withdraw(1L, new BigDecimal("100000"), "Hanoi");

        assertEquals("WITHDRAW", result.getType());
        assertEquals(new BigDecimal("1000.00"), result.getFee().setScale(2));
        assertEquals(new BigDecimal("400000"), account.getBalance());
        verify(alertService).checkAndFlag(result);
    }

    @Test
    void withdraw_throws_whenExceedsCustomerTypeLimit_evenIfWithinAccountLimit() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));

        // 250,000 < transactionLimit (300,000) NHƯNG > customerType limit (200,000)
        BusinessException ex = assertThrows(BusinessException.class,
                () -> transactionService.withdraw(1L, new BigDecimal("250000"), "Hanoi"));

        assertTrue(ex.getMessage().contains("200000"));
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void withdraw_throws_whenInsufficientBalance() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));

        assertThrows(BusinessException.class,
                () -> transactionService.withdraw(1L, new BigDecimal("999999999"), "Hanoi"));
    }

    @Test
    void withdraw_throws_whenAmountIsZeroOrNegative() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));

        assertThrows(BusinessException.class,
                () -> transactionService.withdraw(1L, BigDecimal.ZERO, "Hanoi"));
    }

    @Test
    void withdraw_throws_whenAccountNotFound() {
        when(accountRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class,
                () -> transactionService.withdraw(99L, new BigDecimal("1000"), "Hanoi"));
    }

    @Test
    void deposit_success_increasesBalance() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));

        Transaction result = transactionService.deposit(1L, new BigDecimal("50000"), "Hanoi");

        assertEquals("DEPOSIT", result.getType());
        assertEquals(new BigDecimal("550000"), account.getBalance());
        verify(alertService).checkAndFlag(result);
    }

    @Test
    void transfer_success_movesBalanceBetweenAccounts() {
        Account toAccount = new Account("100001", new BigDecimal("100000"), customer,
                new BigDecimal("500000"), LocalDate.now());

        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(toAccount));

        Transaction result = transactionService.transfer(1L, 2L, new BigDecimal("100000"), "Hanoi");

        assertEquals("TRANSFER_OUT", result.getType());
        assertEquals(new BigDecimal("400000"), account.getBalance());
        assertEquals(new BigDecimal("200000"), toAccount.getBalance());
        verify(alertService, times(2)).checkAndFlag(any());
    }

    @Test
    void transfer_throws_whenSameAccount() {
        assertThrows(BusinessException.class,
                () -> transactionService.transfer(1L, 1L, new BigDecimal("1000"), "Hanoi"));
    }
}