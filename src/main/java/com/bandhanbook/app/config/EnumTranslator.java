package com.bandhanbook.app.config;

import com.bandhanbook.app.model.constants.EventType;
import com.bandhanbook.app.model.constants.Status;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EnumTranslator {

    private final MessageUtil messageUtil;

    public String translateStatus(Status status) {
        return messageUtil.get("status." + status.name());
    }

    public String translateEventType(EventType type) {
        return messageUtil.get("eventType." + type.name());
    }
}