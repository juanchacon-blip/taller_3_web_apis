package co.edu.unibague.tasks.model;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "tasks")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private boolean completed;

    /** Username (claim "sub" del JWT) del dueño de la tarea. */
    @Column(nullable = false, length = 50)
    private String owner;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected Task() {
    }

    public Task(String title, String description, boolean completed, String owner) {
        this.title = title;
        this.description = description;
        this.completed = completed;
        this.owner = owner;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
    public String getOwner() { return owner; }
    public Instant getCreatedAt() { return createdAt; }
}
