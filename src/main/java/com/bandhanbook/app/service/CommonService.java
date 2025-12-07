package com.bandhanbook.app.service;

import com.bandhanbook.app.exception.CommontException;
import com.bandhanbook.app.model.Address;
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
import java.util.Optional;

@Service
public class CommonService {
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ResourceLoader resourceLoader;
    private List<Country> cachedCountry = null;
    private static List<States> states = null;
    private static List<City> cities = null;
    private static List<Country> countries = null;

    private CommonService(ObjectMapper objectMapper, ResourceLoader resourceLoader) throws IOException {
        Resource CountryResource =
                resourceLoader.getResource("classpath:Json/country.json");
        try (InputStream inputStream = CountryResource.getInputStream()) {
            countries = Arrays.asList(objectMapper.readValue(inputStream, Country[].class));
        }
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

    public Flux<States> getStates(int countryId) throws IOException {
        return Flux.fromIterable(states)
                .filter(state -> state.getCountry_id() == countryId)
                .switchIfEmpty(Flux.error(new CommontException("Currently states are not available for selected country")));
    }

    public Flux<City> getCities(int state_id) throws IOException {
        return Flux.fromIterable(cities)
                .filter(state -> state.getState_id() == state_id).switchIfEmpty(Flux.error(new CommontException("Currently cities are not available for selected state")));
    }

    public Address getAddressByIds(String localAddress, int countryId, int stateId, int cityId, String zip) {
        Address address = new Address();

        Optional<Country> country = countries.stream().filter(c -> c.getId() == countryId).findFirst();
        address.setCountry(country.orElse(new Country(101, "India")));

        Optional<States> state = states.stream().filter(s -> s.getId() == stateId).findFirst();
        address.setState(state.orElse(null));

        Optional<City> city = cities.stream().filter(cit -> cit.getId() == cityId).findFirst();
        address.setCity(city.orElse(null));
        address.setAddress(localAddress);
        address.setZip(zip);

        return address;
    }
}
