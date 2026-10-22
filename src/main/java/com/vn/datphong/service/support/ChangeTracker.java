package com.vn.datphong.service.support;

import com.vn.datphong.exception.BusinessException;
import java.util.*;
import java.util.function.Consumer;

/** Chỉ ghi các trường thực sự thay đổi; null trong PATCH nghĩa là giữ nguyên. */
public class ChangeTracker {
    private final Map<String, Object> before = new LinkedHashMap<>();
    private final Map<String, Object> after = new LinkedHashMap<>();

    public <T> void set(String field, T oldValue, T newValue, Consumer<T> setter) {
        if (newValue == null || Objects.equals(oldValue, newValue)) return;
        before.put(field, auditValue(field, oldValue));
        after.put(field, auditValue(field, newValue));
        setter.accept(newValue);
    }

    private Object auditValue(String field, Object value) {
        if (value == null) return null;
        return Set.of("fullName", "email", "phone", "avatar").contains(field) ? "[REDACTED]" : value;
    }

    public boolean hasChanges() { return !before.isEmpty(); }
    public Map<String, Object> before() { return before; }
    public Map<String, Object> after() { return after; }

    public static void requireVersion(Long actual, Long requested) {
        if (requested == null || !Objects.equals(actual, requested)) {
            throw new BusinessException(409, "VERSION_CONFLICT", "Dữ liệu đã thay đổi, hãy tải lại");
        }
    }
}
