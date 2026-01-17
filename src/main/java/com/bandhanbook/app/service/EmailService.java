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
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromEmail;

    @Value("${app.mail.support}")
    private String supportEmail;

    public Mono<Void> sendThankYouMail(String to, String name) {
        return Mono.fromRunnable(() -> {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom(fromEmail);
            mail.setTo(to);
            mail.setSubject("Thank you for contacting us");
            mail.setText("""
                    Hello %s,

                    Thank you for contacting BandhanBook.
                    We have received your request and our team will reach you shortly.

                    Regards,
                    BandhanBook Team
                    """.formatted(name));

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
}
