package co.edu.unibague.tasks.repository;

import co.edu.unibague.tasks.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByOwnerOrderByIdAsc(String owner);
}
