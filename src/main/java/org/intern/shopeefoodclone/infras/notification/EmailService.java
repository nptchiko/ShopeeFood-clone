package org.intern.shopeefoodclone.infras.notification;

public interface EmailService {
    void sendEmail(String to, String subject, String body);
    void sendOtpEmail(String to, String otpCode);
}
