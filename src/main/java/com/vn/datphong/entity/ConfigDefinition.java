package com.vn.datphong.entity;

import com.vn.datphong.enums.ConfigValueType;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "config_definitions")
public class ConfigDefinition {

    @Id
    @Column(name = "config_key", nullable = false)
    private String configKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "value_type", nullable = false)
    private ConfigValueType valueType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "allowed_scopes", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> allowedScopes;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "min_value", nullable = true, precision = 19, scale = 2)
    private BigDecimal minValue;

    @Column(name = "max_value", nullable = true, precision = 19, scale = 2)
    private BigDecimal maxValue;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "allowed_values", nullable = true, columnDefinition = "jsonb")
    private Map<String, Object> allowedValues;

    @Column(name = "publicly_readable", nullable = false)
    private Boolean publiclyReadable;

    @Column(name = "definition_version", nullable = false)
    private Integer definitionVersion;

}
