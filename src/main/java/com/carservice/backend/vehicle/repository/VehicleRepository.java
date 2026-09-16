package com.carservice.backend.vehicle.repository;

import com.carservice.backend.vehicle.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    List<Vehicle> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    List<Vehicle> findAllByUserId(Long userId);

    Optional<Vehicle> findByIdAndUserId(Long id, Long userId);

    boolean existsByRegistrationNumber(String registrationNumber);

    boolean existsByRegistrationNumberAndIdNot(String registrationNumber, Long id);

    boolean existsByRegistrationNumberAndUserId(String registrationNumber, Long userId);

    Optional<Vehicle> findByRegistrationNumber(String registrationNumber);

    long countByUserId(Long userId);

    @Query("SELECT v.user.id, COUNT(v) FROM Vehicle v WHERE v.user.id IN :userIds GROUP BY v.user.id")
    List<Object[]> countVehiclesByUserIds(@Param("userIds") Collection<Long> userIds);
}

