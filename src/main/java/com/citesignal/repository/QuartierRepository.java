package com.citesignal.repository;

import com.citesignal.model.Quartier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface QuartierRepository extends JpaRepository<Quartier, Long> {
    Optional<Quartier> findByNom(String nom);
    boolean existsByNom(String nom);
}

