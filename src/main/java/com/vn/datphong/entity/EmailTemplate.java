package com.vn.datphong.entity;

import com.vn.datphong.enums.MailEventType;
import com.vn.datphong.enums.TemplateStatus;

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
@Table(name = "email_templates", uniqueConstraints = {
    @UniqueConstraint(name = "uq_email_templates_0", columnNames = {"template_code", "locale"})
})
public class EmailTemplate extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "template_code", nullable = false)
    private MailEventType templateCode;

    @Column(name = "locale", nullable = false)
    private String locale;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private TemplateStatus status = TemplateStatus.DRAFT;

    @Column(name = "active_version", nullable = true)
    private Integer activeVersion;

    @Version
    private Long version;

}
