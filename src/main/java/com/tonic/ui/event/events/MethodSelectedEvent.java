package com.tonic.ui.event.events;

import com.tonic.ui.event.Event;
import com.tonic.ui.model.MethodEntryModel;

/**
 * Fired when a method is selected in the navigator.
 */
public class MethodSelectedEvent extends Event {

    private final MethodEntryModel methodEntry;

    public MethodSelectedEvent(Object source, MethodEntryModel methodEntry) {
        super(source);
        this.methodEntry = methodEntry;
    }

    public MethodEntryModel getMethodEntry() {
        return methodEntry;
    }
}
