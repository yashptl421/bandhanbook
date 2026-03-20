package com.bandhanbook.app.service;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public interface EmailService {
    public Mono<Void> sendEmail(String to, String subject, String content);
}
