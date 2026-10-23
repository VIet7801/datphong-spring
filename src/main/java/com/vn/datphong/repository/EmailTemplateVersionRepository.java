package com.vn.datphong.repository;

import com.vn.datphong.entity.EmailTemplateVersion;
import com.vn.datphong.enums.MailEventType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EmailTemplateVersionRepository extends JpaRepository<EmailTemplateVersion, Long> {
    @EntityGraph(attributePaths = "template")
    @Query("""
            select v from EmailTemplateVersion v join v.template t
            where t.templateCode = :type and t.locale = :locale
              and t.status = com.vn.datphong.enums.TemplateStatus.ACTIVE and v.versionNo = t.activeVersion
              and v.publishedAt is not null
            """)
    Optional<EmailTemplateVersion> findActive(@Param("type") MailEventType type, @Param("locale") String locale);
}
