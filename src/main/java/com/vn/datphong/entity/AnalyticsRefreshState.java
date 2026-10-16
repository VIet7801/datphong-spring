package com.vn.datphong.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.*;
import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "analytics_refresh_state")
public class AnalyticsRefreshState {

    @Id
    @Column(name = "property_id", nullable = false)
    private Long propertyId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", insertable = false, updatable = false)
    private Property property;

    @Column(name = "data_version", nullable = false)
    private Long dataVersion;

    @Column(name = "refreshed_at", nullable = false)
    private Instant refreshedAt;

    @Column(name = "source_cutoff_at", nullable = true)
    private Instant sourceCutoffAt;

}
