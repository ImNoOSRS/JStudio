package com.tonic.ui.event;

/**
 * Base class for all JStudio events.
 */
public abstract class Event {

    private final long timestamp;
    private final Object source;

    protected Event(Object source) {
        this.timestamp = System.currentTimeMillis();
        this.source = source;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public Object getSource() {
        return source;
    }
}
