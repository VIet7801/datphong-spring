package com.vn.datphong.service.identity;

import com.vn.datphong.config.AuthProperties;
import com.vn.datphong.entity.*;
import com.vn.datphong.enums.*;
import com.vn.datphong.exception.DomainException;
import com.vn.datphong.repository.*;
import com.vn.datphong.util.Masking;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.*;
import java.util.*;

/** Hàng đợi email xác minh; SMTP luôn chạy ngoài transaction DB. */
@Service
@RequiredArgsConstructor
public class VerificationMailService {
    private final MailEventRepository mails;
    private final EmailTemplateVersionRepository templates;
    private final JavaMailSender sender;
    private final org.springframework.transaction.PlatformTransactionManager transactionManager;
    private final Clock clock;
    private final AuthProperties properties;

    @Transactional(propagation = Propagation.MANDATORY)
    public void enqueue(User user, VerificationToken token, String raw) {
        for (MailEvent old : mails.findByRecipient_IdAndEventTypeAndStatusIn(user.getId(),
                MailEventType.EMAIL_VERIFICATION, List.of(MailStatus.QUEUED, MailStatus.RETRY))) {
            old.setStatus(MailStatus.CANCELLED);
            old.setPayloadSnapshot(Map.of());
        }
        EmailTemplateVersion template = templates.findActive(MailEventType.EMAIL_VERIFICATION, properties.getMailLocale())
                .orElseThrow(() -> new DomainException(HttpStatus.SERVICE_UNAVAILABLE, "VERIFICATION_MAIL_NOT_CONFIGURED",
                        "Chưa khởi tạo mẫu email xác minh"));
        Map<String, Object> payload = Map.of("token", raw,
                "expiresMinutes", Long.toString(properties.getVerificationTtl().toMinutes()));
        String traceId = MDC.get("traceId") == null ? UUID.randomUUID().toString() : MDC.get("traceId");
        mails.save(MailEvent.builder().eventId(UUID.randomUUID()).eventType(MailEventType.EMAIL_VERIFICATION)
                .businessKey("verification:" + token.getId()).recipient(user)
                .recipientIdentity(user.getEmail()).recipientAddress(user.getEmail())
                .recipientMasked(Masking.email(user.getEmail())).templateVersion(template)
                .payloadSnapshot(payload).dueAt(clock.instant()).expiresAt(token.getExpiresAt())
                .correlationId(traceId).build());
    }

    @Scheduled(fixedDelayString = "${app.auth.mail-poll-ms:10000}", initialDelayString = "${app.auth.mail-poll-ms:10000}")
    public void dispatch() {
        TransactionTemplate transactions = new TransactionTemplate(transactionManager);
        for (int i = 0; i < 10; i++) {
            MailEvent mail = transactions.execute(tx -> claim());
            if (mail == null) return;
            boolean sent;
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(properties.getMailFrom());
                message.setTo(mail.getRecipientAddress());
                message.setSubject(render(mail.getTemplateVersion().getSubject(), mail.getPayloadSnapshot()));
                message.setText(render(mail.getTemplateVersion().getTextBody(), mail.getPayloadSnapshot()));
                sender.send(message);
                sent = true;
            } catch (MailException failure) {
                sent = false; // Không log SMTP exception: có thể chứa nguyên nội dung email/token.
            }
            boolean delivered = sent;
            transactions.executeWithoutResult(tx -> finish(mail, delivered));
        }
    }

    private MailEvent claim() {
        Instant now = clock.instant();
        List<MailEvent> ready = mails.findReadyForUpdate(now, PageRequest.of(0, 1));
        if (ready.isEmpty()) return null;
        MailEvent mail = ready.get(0);
        if (!mail.getExpiresAt().isAfter(now)) {
            mail.setStatus(MailStatus.CANCELLED);
            mail.setPayloadSnapshot(Map.of());
            return null;
        }
        mail.setStatus(MailStatus.SENDING);
        mail.setLeaseOwner(UUID.randomUUID().toString());
        mail.setLeaseUntil(now.plusSeconds(60));
        return mail;
    }

    private void finish(MailEvent claimed, boolean delivered) {
        MailEvent mail = mails.findByIdForUpdate(claimed.getId()).orElseThrow();
        if (!Objects.equals(mail.getLeaseOwner(), claimed.getLeaseOwner())) return;
        mail.setLeaseOwner(null);
        mail.setLeaseUntil(null);
        if (delivered) {
            mail.setStatus(MailStatus.SENT);
            mail.setLastErrorCode(null);
            mail.setSentAt(clock.instant());
            mail.setPayloadSnapshot(Map.of()); // Không giữ mã xác minh sau khi gửi.
        } else {
            mail.setRetryCount(mail.getRetryCount() + 1);
            mail.setStatus(MailStatus.RETRY);
            mail.setLastErrorCode("SMTP_UNAVAILABLE");
            mail.setDueAt(clock.instant().plusSeconds(Math.min(300, 15L * mail.getRetryCount())));
        }
    }

    private String render(String template, Map<String, Object> payload) {
        return template.replace("{{token}}", Objects.toString(payload.get("token"), ""))
                .replace("{{expiresMinutes}}", Objects.toString(payload.get("expiresMinutes"), ""));
    }
}
