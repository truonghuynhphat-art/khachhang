package com.bank.controller;

import com.bank.entity.Account;
import com.bank.entity.User;
import com.bank.exception.BusinessException;
import com.bank.repository.UserRepository;
import com.bank.service.AccountService;
import com.bank.service.ReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Optional;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;
    private final AccountService accountService;
    private final UserRepository userRepository;

    public ReportController(ReportService reportService, AccountService accountService,
                             UserRepository userRepository) {
        this.reportService = reportService;
        this.accountService = accountService;
        this.userRepository = userRepository;
    }

    @GetMapping("/excel")
    public ResponseEntity<byte[]> excel(
            @RequestParam(required = false) Long accountId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) throws Exception {

        Long resolvedAccountId = resolveAccountId(accountId);
        byte[] bytes = reportService.generateExcel(resolvedAccountId, from, to);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=report.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    @GetMapping("/pdf")
    public ResponseEntity<byte[]> pdf(
            @RequestParam(required = false) Long accountId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) throws Exception {

        Long resolvedAccountId = resolveAccountId(accountId);
        byte[] bytes = reportService.generatePdf(resolvedAccountId, from, to);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=report.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(bytes);
    }

    /**
     * Admin: dùng đúng accountId truyền vào, hoặc null (báo cáo toàn hệ thống) nếu không truyền.
     * Customer: bắt buộc phải truyền accountId và phải là tài khoản của chính họ - không được xem toàn hệ thống.
     */
    private Long resolveAccountId(Long requestedAccountId) {
        if (isAdmin()) {
            return requestedAccountId; // null hợp lệ => toàn hệ thống
        }

        if (requestedAccountId == null) {
            throw new BusinessException("accountId is required for customer reports", HttpStatus.BAD_REQUEST);
        }
        Account account = accountService.getAccountById(requestedAccountId)
                .orElseThrow(() -> new BusinessException("Account not found", HttpStatus.NOT_FOUND));
        Long ownId = currentCustomerId().orElse(-1L);
        if (!account.getOwner().getId().equals(ownId)) {
            throw new BusinessException("You can only view reports for your own account", HttpStatus.FORBIDDEN);
        }
        return requestedAccountId;
    }

    private boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    private Optional<Long> currentCustomerId() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .map(User::getCustomer).filter(c -> c != null).map(c -> c.getId());
    }
}