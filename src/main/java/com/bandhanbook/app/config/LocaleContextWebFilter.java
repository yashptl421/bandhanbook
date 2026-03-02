package com.bandhanbook.app.config;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Locale;

@Component
public class LocaleContextWebFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String lang = exchange.getRequest()
                .getHeaders()
                .getFirst("language");
        Locale locale = (lang != null) ? Locale.forLanguageTag(lang) : Locale.ENGLISH;

        LocaleContextHolder.setLocale(locale);
        return chain.filter(exchange);
    }
}