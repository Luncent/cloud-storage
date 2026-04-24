package it.luncent.cloud_storage.common.events.registration;

import org.springframework.context.ApplicationEvent;

public class RegistrationEvent extends ApplicationEvent {
    public RegistrationEvent(Object source) {
        super(source);
    }
}
