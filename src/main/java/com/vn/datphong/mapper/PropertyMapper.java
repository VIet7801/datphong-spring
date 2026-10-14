package com.vn.datphong.mapper;

import com.vn.datphong.dto.request.PropertyCreateRequest;
import com.vn.datphong.dto.response.PropertyResponse;
import com.vn.datphong.entity.CancellationPolicy;
import com.vn.datphong.entity.Property;
import com.vn.datphong.entity.User;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.text.Normalizer;
import java.util.Locale;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PropertyMapper {

    public static Property toEntity(PropertyCreateRequest req, User owner, CancellationPolicy policy) {
        return Property.builder()
                .owner(owner)
                .name(req.getName())
                .slug(toSlug(req.getName()))
                .description(req.getDescription())
                .addressLine(req.getAddressLine())
                .city(req.getCity())
                .country(req.getCountry() != null ? req.getCountry() : "VN")
                .latitude(req.getLatitude())
                .longitude(req.getLongitude())
                .timezone(req.getTimezone() != null ? req.getTimezone() : "Asia/Bangkok")
                .checkInFrom(req.getCheckInFrom())
                .checkOutUntil(req.getCheckOutUntil())
                .publicContact(req.getPublicContact())
                .cancellationPolicy(policy)
                .build();
    }

    public static PropertyResponse toResponse(Property e) {
        if (e == null) return null;
        return PropertyResponse.builder()
                .id(e.getId())
                .ownerId(e.getOwner() != null ? e.getOwner().getId() : null)
                .ownerName(e.getOwner() != null ? e.getOwner().getFullName() : null)
                .name(e.getName())
                .slug(e.getSlug())
                .description(e.getDescription())
                .addressLine(e.getAddressLine())
                .city(e.getCity())
                .country(e.getCountry())
                .latitude(e.getLatitude())
                .longitude(e.getLongitude())
                .timezone(e.getTimezone())
                .checkInFrom(e.getCheckInFrom())
                .checkOutUntil(e.getCheckOutUntil())
                .publicContact(e.getPublicContact())
                .status(e.getStatus())
                .cancellationPolicyId(e.getCancellationPolicy() != null ? e.getCancellationPolicy().getId() : null)
                .cancellationPolicyName(e.getCancellationPolicy() != null ? e.getCancellationPolicy().getName() : null)
                .version(e.getVersion())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }

    private static String toSlug(String value) {
        if (value == null || value.isBlank()) return null;
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        return normalized.isBlank() ? "property" : normalized;
    }
}
