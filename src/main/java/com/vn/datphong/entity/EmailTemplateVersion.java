package com.vn.datphong.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.*;
import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "email_template_versions", uniqueConstraints = {
    @UniqueConstraint(name = "uq_email_template_versions_0", columnNames = {"template_id", "version_no"})
})
public class EmailTemplateVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "template_id", nullable = false)
    private EmailTemplate template;

    @Column(name = "version_no", nullable = false)
    private Integer versionNo;

    @Column(name = "subject", nullable = false, columnDefinition = "TEXT")
    private String subject;

    @Column(name = "html_body", nullable = false, columnDefinition = "TEXT")
    private String htmlBody;

    @Column(name = "text_body", nullable = false, columnDefinition = "TEXT")
    private String textBody;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "allowed_variables", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> allowedVariables;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "required_variables", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> requiredVariables;

    @Column(name = "published_at", nullable = true)
    private Instant publishedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "published_by_id", nullable = true)
    private User publishedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

}
