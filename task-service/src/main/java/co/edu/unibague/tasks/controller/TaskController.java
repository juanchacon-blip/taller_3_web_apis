package co.edu.unibague.tasks.controller;

import co.edu.unibague.tasks.dto.TaskRequest;
import co.edu.unibague.tasks.dto.TaskResponse;
import co.edu.unibague.tasks.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService service;

    public TaskController(TaskService service) {
        this.service = service;
    }

    @GetMapping
    public List<TaskResponse> list(Authentication auth) {
        return service.list(auth.getName(), isAdmin(auth));
    }

    @GetMapping("/{id}")
    public TaskResponse get(@PathVariable Long id, Authentication auth) {
        return service.get(id, auth.getName(), isAdmin(auth));
    }

    @PostMapping
    public ResponseEntity<TaskResponse> create(@Valid @RequestBody TaskRequest request, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request, auth.getName()));
    }

    @PutMapping("/{id}")
    public TaskResponse update(@PathVariable Long id, @Valid @RequestBody TaskRequest request, Authentication auth) {
        return service.update(id, request, auth.getName(), isAdmin(auth));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication auth) {
        service.delete(id, auth.getName(), isAdmin(auth));
        return ResponseEntity.noContent().build();
    }

    private boolean isAdmin(Authentication auth) {
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
