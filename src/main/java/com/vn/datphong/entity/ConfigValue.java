package com.vn.datphong.entity;

import com.vn.datphong.enums.ConfigScope;
import com.vn.datphong.enums.ConfigValueType;

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
@Table(name = "config_values")
public class ConfigValue extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "config_key", nullable = false)
    private String configKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false)
    private ConfigScope scope;

    @Column(name = "scope_id", nullable = true)
    private Long scopeId;

    @Column(name = "value_text", nullable = false, columnDefinition = "TEXT")
    private String valueText;

    @Enumerated(EnumType.STRING)
    @Column(name = "value_type", nullable = false)
    private ConfigValueType valueType;

    @Column(name = "policy_version", nullable = false)
    private Integer policyVersion;

    @Column(name = "effective_at", nullable = false)
    private Instant effectiveAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "updated_by_id", nullable = false)
    private User updatedBy;

    @Column(name = "change_reason", nullable = false)
    private String changeReason;

    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "config_key", insertable = false, updatable = false)
    private ConfigDefinition definition;

}
