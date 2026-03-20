package com.bandhanbook.app.service;

import com.bandhanbook.app.payload.request.ContactUsRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
@RequiredArgsConstructor
public class SMTPEmailService implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromEmail;

    @Value("${app.mail.support}")
    private String supportEmail;

    public Mono<Void> sendSupportEmail(String to, String subject, String content) {
        return sendEmail(supportEmail, to, subject, content);
    }

    public Mono<Void> sendNoReplyEmail(String to, String subject, String content) {
        return sendEmail(fromEmail, to, subject, content);
    }

    public Mono<Void> sendEmail(String from, String to, String subject, String message) {
        return Mono.fromRunnable(() -> {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom(from);
            mail.setTo(to);
            mail.setSubject(subject);
            mail.setText(message);

            mailSender.send(mail);
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    public Mono<Void> notifyAdmin(ContactUsRequest req) {
        return Mono.fromRunnable(() -> {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom(fromEmail);
            mail.setTo(supportEmail);
            mail.setSubject("New Contact Us Request");
            mail.setText("""
                    Name: %s
                    Email: %s
                    Message:
                    %s
                    """.formatted(req.getName(), req.getEmail(), req.getMessage()));

            mailSender.send(mail);
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    public Mono<Void> sendForgotPasswordOtp(String to, String name, String otp) {

        return Mono.fromRunnable(() -> {

            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom(fromEmail);
            mail.setTo(to);
            mail.setSubject("BandhanBook Password Reset OTP");

            mail.setText("""
                    Hello %s,
                    
                    We received a request to reset your password.
                    
                    Your One-Time Password (OTP) is:
                    
                    %s
                    
                    This OTP will expire in 10 minutes.
                    
                    If you did not request a password reset, please ignore this email.
                    
                    Regards,
                    BandhanBook Team
                    """.formatted(name, otp));

            mailSender.send(mail);

        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    public Mono<Void> sendCandidateRegistrationOtp(String to, String name, String otp) {

        return Mono.fromRunnable(() -> {

            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom(fromEmail);
            mail.setTo(to);
            mail.setSubject("BandhanBook Registration OTP");

            mail.setText("""
                    Hello %s,
                    
                    Welcome to BandhanBook!
                    
                    Your One-Time Password (OTP) for registration is:
                    
                    %s
                    
                    This OTP will expire in 10 minutes.
                    
                    Please do not share this OTP with anyone.
                    
                    Regards,
                    BandhanBook Team
                    """.formatted(name, otp));

            mailSender.send(mail);

        }).subscribeOn(Schedulers.boundedElastic()).then();
    }
}
