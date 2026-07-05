package org.intern.shopeefoodclone.infras.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SmtpEmailService implements EmailService {

    @Async("emailExecutor")
    @Override
    public void sendEmail(String to, String subject, String body) {

        log.info("[AsyncEmail] Sending email to: {} | Subject: {} | Body: {}", to, subject, body);
    }

    @Async("emailExecutor")
    @Override
    public void sendOtpEmail(String to, String otpCode) {
        String subject = "ShopeeFood Account Verification OTP";
        String body = String.format("Welcome to ShopeeFood! Your verification code is: %s. It will expire in 5 minutes.", otpCode);
        log.info("[AsyncEmail] Sending OTP email to: {} | OTP Code: {}", to, otpCode);
        // Execute real email dispatch here
    }
}
