package org.intern.shopeefoodclone.infras.notification;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = lombok.AccessLevel.PRIVATE)
public class SmtpEmailService implements EmailService {

    JavaMailSender javaMailSender;

    @NonFinal
    @Value("${spring.mail.username:noreply@fastbite.com}")
    String fromEmail;

    @Async("emailExecutor")
    @Override
    public void sendEmail(String to, String subject, String body) {
        log.info("[AsyncEmail] Sending email to: {} | Subject: {} | Body: {}", to, subject, body);
        sendEmailInternal(to, subject, body);
    }

    @Async("emailExecutor")
    @Override
    public void sendOtpEmail(String to, String otpCode) {
        String subject = "FastBite Account Verification OTP";
        String body = String.format("Welcome to ShopeeFood! Your verification code is: %s. It will expire in 5 minutes.", otpCode);
        log.info("[AsyncEmail] Sending OTP email to: {} | OTP Code: {}", to, otpCode);
        sendEmailInternal(to, subject, body);
    }

    private void sendEmailInternal(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            javaMailSender.send(message);
            log.info("[AsyncEmail] Email sent successfully to: {}", to);
            CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            log.error("[AsyncEmail] Failed to send email to: {}", to, e);
            CompletableFuture.failedFuture(e);
        }
    }
}

