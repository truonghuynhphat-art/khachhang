package com.bank.service;

import com.bank.entity.Account;
import com.bank.entity.AccountStatus;
import com.bank.entity.Customer;
import com.bank.entity.CustomerType;
import com.bank.entity.CustomerTypeName;
import com.bank.exception.BusinessException;
import com.bank.repository.AccountRepository;
import com.bank.repository.AccountStatusHistoryRepository;
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

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock private AccountRepository accountRepository;
    @Mock private AccountStatusHistoryRepository statusHistoryRepository;

    @InjectMocks
    private AccountService accountService;

    private Account account;

    @BeforeEach
    void setUp() {
        CustomerType type = new CustomerType(CustomerTypeName.INDIVIDUAL, new BigDecimal("200000"));
        Customer customer = new Customer("Ann", "Hanoi", type);
        account = new Account("100002", new BigDecimal("500000"), customer,
                new BigDecimal("300000"), LocalDate.now());
        // Account mặc định khởi tạo với status ACTIVE (theo constructor hiện có)
    }

    @Test
    void changeStatus_success_fromActiveToLocked() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        Account result = accountService.changeStatus(1L, AccountStatus.LOCKED, "Suspicious activity");

        assertEquals(AccountStatus.LOCKED, result.getStatus());
        verify(statusHistoryRepository).save(argThat(history ->
                history.getOldStatus() == AccountStatus.ACTIVE
                        && history.getNewStatus() == AccountStatus.LOCKED
                        && "Suspicious activity".equals(history.getReason())));
    }

    @Test
    void changeStatus_throws_whenAccountAlreadyClosed() {
        account.setStatus(AccountStatus.CLOSED);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> accountService.changeStatus(1L, AccountStatus.LOCKED, "test"));

        assertTrue(ex.getMessage().contains("closed"));
        verify(accountRepository, never()).save(any());
        verify(statusHistoryRepository, never()).save(any());
    }

    @Test
    void changeStatus_throws_whenSameStatusRequested() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));

        assertThrows(BusinessException.class,
                () -> accountService.changeStatus(1L, AccountStatus.ACTIVE, "no-op"));

        verify(statusHistoryRepository, never()).save(any());
    }

    @Test
    void changeStatus_throws_whenAccountNotFound() {
        when(accountRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class,
                () -> accountService.changeStatus(99L, AccountStatus.LOCKED, "test"));
    }

    @Test
    void changeStatus_throws_whenNewStatusIsNull() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));

        assertThrows(BusinessException.class,
                () -> accountService.changeStatus(1L, null, "test"));
    }

    @Test
    void getStatusHistory_throws_whenAccountDoesNotExist() {
        when(accountRepository.existsById(99L)).thenReturn(false);

        assertThrows(BusinessException.class, () -> accountService.getStatusHistory(99L));
    }

    @Test
    void createAccount_delegatesToRepository() {
        when(accountRepository.save(account)).thenReturn(account);

        Account result = accountService.createAccount(account);

        assertSame(account, result);
        verify(accountRepository).save(account);
    }

    @Test
    void deleteAccount_throws_whenNotFound() {
        when(accountRepository.existsById(5L)).thenReturn(false);

        assertThrows(BusinessException.class, () -> accountService.deleteAccount(5L));
        verify(accountRepository, never()).deleteById(any());
    }
}
