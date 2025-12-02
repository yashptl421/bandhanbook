package com.bandhanbook.app.service;

import com.bandhanbook.app.repository.EventParticipantsRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class EventParticipantsService {

    private final EventParticipantsRepository eventParticipantRepo;

}
