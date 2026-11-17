package com.safetyall.backend.worker;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkerRepository extends JpaRepository<Worker, Long> {

    Optional<Worker> findByDeviceId(String deviceId);

    List<Worker> findByEndedAtIsNullOrderByIdAsc();

    List<Worker> findByEndedAtIsNullAndStatusOrderByIdAsc(String status);
}
