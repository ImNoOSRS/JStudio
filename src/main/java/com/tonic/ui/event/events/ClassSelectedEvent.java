package com.tonic.ui.event.events;

import com.tonic.ui.event.Event;
import com.tonic.ui.model.ClassEntryModel;

/**
 * Fired when a class is selected in the navigator.
 */
public class ClassSelectedEvent extends Event {

    private final ClassEntryModel classEntry;

    public ClassSelectedEvent(Object source, ClassEntryModel classEntry) {
        super(source);
        this.classEntry = classEntry;
    }

    public ClassEntryModel getClassEntry() {
        return classEntry;
    }
}
