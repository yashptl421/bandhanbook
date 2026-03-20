package com.bandhanbook.app.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Primary
public class ZohoMailService implements EmailService{

    private final RestTemplate restTemplate;

    @Value("${zoho.client.id}")
    private String clientId;

    @Value("${zoho.client.secret}")
    private String clientSecret;

    @Value("${zoho.refresh.token}")
    private String refreshToken;

    @Value("${zoho.mail.account.id}")
    private String accountId;

    @Value("${app.mail.from}")
    private String fromEmail;

    @Value("${app.mail.support}")
    private String supportEmail;

    public String getAccessToken() {
        String url = "https://accounts.zoho.in/oauth/v2/token";

        String body = "refresh_token=" + refreshToken +
                "&client_id=" + clientId +
                "&client_secret=" + clientSecret +
                "&grant_type=refresh_token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<String> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

        return (String) response.getBody().get("access_token");
    }

    public Mono<Void> sendEmail(String to, String subject, String content) {
        String accessToken = getAccessToken();

        String url = "https://mail.zoho.in/api/accounts/" + accountId + "/messages";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("fromAddress", supportEmail);
        body.put("toAddress", to);
        body.put("subject", subject);
        body.put("content", content);
        body.put("mailFormat", "html");

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        restTemplate.postForEntity(url, request, String.class);
       return Mono.empty();
    }
}