package com.tonic.ui.event.events;

import com.tonic.ui.event.Event;
import com.tonic.ui.model.ProjectModel;

/**
 * Fired when a project (JAR or class files) is loaded.
 */
public class ProjectLoadedEvent extends Event {

    private final ProjectModel project;

    public ProjectLoadedEvent(Object source, ProjectModel project) {
        super(source);
        this.project = project;
    }

    public ProjectModel getProject() {
        return project;
    }
}
