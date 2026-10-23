package com.vn.datphong.controller;


import com.vn.datphong.dto.request.FakeBankAccountRequest;
import com.vn.datphong.dto.request.FakeBankFaultRequest;
import com.vn.datphong.entity.FakeBankAccount;
import com.vn.datphong.repository.FakeBankAccountRepository;
import com.vn.datphong.util.FakeBankFaults;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/fake-bank")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class FakeBankAdminController {
    private final FakeBankAccountRepository accounts;
    private final FakeBankFaults faults;


    @PutMapping("/accounts")
    @Transactional
    public Map<String, Object> upsert(@Valid @RequestBody FakeBankAccountRequest req) {
        FakeBankAccount a = accounts.findForUpdate(req.getAccountNo()).orElseGet(() -> {
            FakeBankAccount n = new FakeBankAccount();
            n.setAccountNo(req.getAccountNo());
            return n;
        });
        a.setHolderName(req.getHolderName());
        a.setBalance(req.getBalance());
        accounts.save(a);
        return Map.of("accountNo", a.getAccountNo(), "balance", a.getBalance());
    }

    @GetMapping("/accounts/{accountNo}")
    @Transactional(readOnly = true)
    public Map<String, Object> get(@PathVariable String accountNo) {
        FakeBankAccount a = accounts.findByAccountNo(accountNo).orElseThrow(
                () -> com.vn.datphong.exception.DomainException.notFound("ACCOUNT_NOT_FOUND", "Không có tài khoản"));
        return Map.of("accountNo", a.getAccountNo(), "holder", a.getHolderName(), "balance", a.getBalance());
    }

    @PutMapping("/faults")
    public Map<String, Integer> setFaults(@RequestBody FakeBankFaultRequest req) {
        faults.set(req.getRefundFailures() == null ? 0 : req.getRefundFailures(), req.getRefundTimeouts() == null ? 0 : req.getRefundTimeouts());
        return Map.of("refundFailures", faults.pendingFailures(), "refundTimeouts", faults.pendingTimeouts());
    }
}
