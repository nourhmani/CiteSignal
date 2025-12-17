package com.citesignal.repository;

import com.citesignal.model.Rapport;
import com.citesignal.model.TypeRapport;
import com.citesignal.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RapportRepository extends JpaRepository<Rapport, Long> {
    List<Rapport> findByCreatedBy(User user);
    Page<Rapport> findByCreatedBy(User user, Pageable pageable);
    List<Rapport> findByType(TypeRapport type);
}

