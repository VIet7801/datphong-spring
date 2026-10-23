package com.vn.datphong.dto.request;

import com.vn.datphong.util.Hashing;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FakeBankWebhookRequest {

    public static final String PAYMENT_SUCCEEDED = "PAYMENT_SUCCEEDED";
    public static final String PAYMENT_FAILED = "PAYMENT_FAILED";

    private String eventId;
    private String type;
    private String reference;
    private String idempotencyKey;
    private BigDecimal amount;
    private String currency;
    private String failureCode;
    private long timestamp;
    private String signature;

    public String canonical() {
        return String.join("\n", n(eventId), n(type), n(reference), n(idempotencyKey),
                amount == null ? "" : amount.stripTrailingZeros().toPlainString(), n(currency), n(failureCode),
                Long.toString(timestamp));
    }

    public static FakeBankWebhookRequest signed(String secret, String eventId, String type, String reference, String key,
                                                BigDecimal amount, String currency, String failureCode, long timestamp) {
        FakeBankWebhookRequest unsigned = new FakeBankWebhookRequest(eventId, type, reference, key, amount, currency,
                failureCode, timestamp, null);
        return new FakeBankWebhookRequest(eventId, type, reference, key, amount, currency, failureCode, timestamp,
                Hashing.hmacSha256Hex(secret, unsigned.canonical()));
    }

    public boolean validSignature(String secret) {
        return signature != null && Hashing.constantTimeEquals(signature, Hashing.hmacSha256Hex(secret, canonical()));
    }

    private static String n(String s) {
        return s == null ? "" : s;
    }
}
