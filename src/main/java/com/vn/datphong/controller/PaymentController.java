package com.vn.datphong.controller;

import com.vn.datphong.dto.request.FakeBankWebhookRequest;
import com.vn.datphong.dto.request.PaymentRequest;
import com.vn.datphong.dto.response.PaymentResponse;
import com.vn.datphong.service.PaymentIntentService;
import com.vn.datphong.service.PaymentReconcileService;
import com.vn.datphong.service.PaymentWebhookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentIntentService intents;
    private final PaymentWebhookService webhook;
    private final PaymentReconcileService reconcile;


    @PostMapping("/intents")
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse create(@Valid @RequestBody PaymentRequest req,
                              @RequestHeader(value = "Idempotency-Key", required = false) String key) {
        return intents.create(req, key);
    }

    @GetMapping("/{id}")
    public PaymentResponse get(@PathVariable Long id) {
        return intents.get(id);
    }

    @GetMapping("/booking/{bookingId}")
    public List<PaymentResponse> forBooking(@PathVariable Long bookingId) {
        return intents.forBooking(bookingId);
    }

    @PostMapping("/fake-bank/webhook")
    public Map<String, String> webhook(@RequestBody FakeBankWebhookRequest payload) {
        return Map.of("status", webhook.receive(payload));
    }

    @PostMapping("/{id}/reconcile")
    @PreAuthorize("hasAnyRole('SUPPORT','ADMIN')")
    public Map<String, String> reconcile(@PathVariable Long id) {
        return Map.of("status", reconcile.reconcile(id).name());
    }
}
