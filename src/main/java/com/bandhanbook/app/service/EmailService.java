package com.bandhanbook.app.service;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public interface EmailService {
    public Mono<Void> sendSupportEmail(String to, String subject, String content);

    public Mono<Void> sendNoReplyEmail(String to, String subject, String content);

    public Mono<Void> sendEmail(String from, String to, String subject, String content);
}
