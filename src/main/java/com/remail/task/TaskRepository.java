package com.remail.task;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<TaskEntity, Long> {

    Optional<TaskEntity> findBySourceMessageId(String sourceMessageId);

    Optional<TaskEntity> findFirstByStatusNotOrderByUpdatedAtDesc(TaskStatus status);

    List<TaskEntity> findAllByOrderByUpdatedAtDesc();
}