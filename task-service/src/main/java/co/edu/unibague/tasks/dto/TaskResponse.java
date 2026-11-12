package co.edu.unibague.tasks.dto;

import co.edu.unibague.tasks.model.Task;

import java.time.Instant;

public record TaskResponse(Long id, String title, String description, boolean completed,
                           String owner, Instant createdAt) {

    public static TaskResponse from(Task t) {
        return new TaskResponse(t.getId(), t.getTitle(), t.getDescription(), t.isCompleted(),
                t.getOwner(), t.getCreatedAt());
    }
}
