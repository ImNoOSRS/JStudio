package com.tonic.ui.event.events;

import com.tonic.ui.event.Event;

/**
 * Fired to update the status bar message.
 */
public class StatusMessageEvent extends Event {

    private final String message;
    private final MessageType type;

    public StatusMessageEvent(Object source, String message) {
        this(source, message, MessageType.INFO);
    }

    public StatusMessageEvent(Object source, String message, MessageType type) {
        super(source);
        this.message = message;
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public MessageType getType() {
        return type;
    }

    public enum MessageType {
        INFO,
        WARNING,
        ERROR
    }
}
