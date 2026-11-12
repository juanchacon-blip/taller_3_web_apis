package co.edu.unibague.tasks.service;

import co.edu.unibague.tasks.dto.TaskRequest;
import co.edu.unibague.tasks.dto.TaskResponse;
import co.edu.unibague.tasks.model.Task;
import co.edu.unibague.tasks.repository.TaskRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Reglas de autorización: un USER solo ve y modifica sus propias tareas;
 * un ADMIN puede ver y modificar las de todos.
 */
@Service
@Transactional
public class TaskService {

    private final TaskRepository repository;

    public TaskService(TaskRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> list(String username, boolean admin) {
        List<Task> tasks = admin ? repository.findAll() : repository.findByOwnerOrderByIdAsc(username);
        return tasks.stream().map(TaskResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public TaskResponse get(Long id, String username, boolean admin) {
        return TaskResponse.from(find(id, username, admin));
    }

    public TaskResponse create(TaskRequest request, String username) {
        Task task = new Task(request.title(), request.description(),
                Boolean.TRUE.equals(request.completed()), username);
        return TaskResponse.from(repository.save(task));
    }

    public TaskResponse update(Long id, TaskRequest request, String username, boolean admin) {
        Task task = find(id, username, admin);
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setCompleted(Boolean.TRUE.equals(request.completed()));
        return TaskResponse.from(repository.save(task));
    }

    public void delete(Long id, String username, boolean admin) {
        repository.delete(find(id, username, admin));
    }

    // 404 tanto si no existe como si es de otro usuario, para no filtrar su existencia
    private Task find(Long id, String username, boolean admin) {
        return repository.findById(id)
                .filter(t -> admin || t.getOwner().equals(username))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tarea no encontrada"));
    }
}
