package com.mediconnect.notification;

import com.mediconnect.common.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Sends through SMTP when mail is configured. Without SMTP (local demo) it logs the
 * email instead, so the outbox flow still runs end to end.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailSender {

    private final ObjectProvider<JavaMailSender> mailSender;
    private final AppProperties props;

    /** @return a short description of how the message was delivered */
    public String send(String to, String subject, String body) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (!props.mail().enabled() || sender == null) {
            log.info("[mail disabled] To: {} | Subject: {}", to, subject);
            return "LOGGED";
        }
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(props.mail().from());
        msg.setTo(to);
        msg.setSubject(subject);
        msg.setText(body);
        sender.send(msg);
        return "SENT";
    }
}
