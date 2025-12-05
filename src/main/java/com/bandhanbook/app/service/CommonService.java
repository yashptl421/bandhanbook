package com.bandhanbook.app.service;

import com.bandhanbook.app.exception.CommontException;
import com.bandhanbook.app.model.City;
import com.bandhanbook.app.model.Country;
import com.bandhanbook.app.model.States;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

@Service
public class CommonService {
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ResourceLoader resourceLoader;
    private List<Country> cachedCountry = null;
    private static List<States> states = null;
    private static List<City> cities = null;

    private CommonService(ObjectMapper objectMapper, ResourceLoader resourceLoader) throws IOException {
        Resource resource =
                resourceLoader.getResource("classpath:Json/state.json");
        try (InputStream inputStream = resource.getInputStream()) {
            states = Arrays.asList(objectMapper.readValue(inputStream, States[].class));
        }
        Resource cityResource =
                resourceLoader.getResource("classpath:Json/city.json");
        try (InputStream inputStream = cityResource.getInputStream()) {
            cities = Arrays.asList(objectMapper.readValue(inputStream, City[].class));
        }
    }

    public Mono<List<Country>> getCountry() {
        if (cachedCountry != null) {
            return Mono.just(cachedCountry);
        }

        return Mono.fromCallable(() -> {
                    Resource resource =
                            resourceLoader.getResource("classpath:Json/country.json");

                    try (InputStream inputStream = resource.getInputStream()) {
                        return Arrays.asList(objectMapper.readValue(inputStream, Country[].class));
                    }
                })
                .doOnNext(list -> cachedCountry = list)        // store in cache
                .subscribeOn(Schedulers.boundedElastic()); // File I/O → must run on elastic thread
    }

    public Flux<States> getStates(String countryId) throws IOException {
        return Flux.fromIterable(states)
                .filter(state -> state.getCountry_id().equals(countryId))
                .switchIfEmpty(Flux.error(new CommontException("Currently states are not available for selected country")));
    }

    public Flux<City> getCities(String state_id) throws IOException {
        return Flux.fromIterable(cities)
                .filter(state -> state.getState_id().equals(state_id)).switchIfEmpty(Flux.error(new CommontException("Currently cities are not available for selected state")));
    }
}
