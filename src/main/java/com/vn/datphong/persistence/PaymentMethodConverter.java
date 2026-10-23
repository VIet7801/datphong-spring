package com.vn.datphong.persistence;

import com.vn.datphong.enums.PaymentMethodType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** API giữ mã FAKE_BANK; catalog PostgreSQL lưu mã FAKE_BANK_DEBIT. */
@Converter
public class PaymentMethodConverter implements AttributeConverter<PaymentMethodType, String> {
    @Override
    public String convertToDatabaseColumn(PaymentMethodType method) {
        if (method == null) return null;
        return method == PaymentMethodType.FAKE_BANK ? "FAKE_BANK_DEBIT" : method.name();
    }

    @Override
    public PaymentMethodType convertToEntityAttribute(String value) {
        if (value == null) return null;
        return "FAKE_BANK_DEBIT".equals(value) ? PaymentMethodType.FAKE_BANK : PaymentMethodType.valueOf(value);
    }
}
