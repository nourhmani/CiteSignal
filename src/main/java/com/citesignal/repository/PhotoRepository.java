package com.citesignal.repository;

import com.citesignal.model.Photo;
import com.citesignal.model.Incident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PhotoRepository extends JpaRepository<Photo, Long> {
    List<Photo> findByIncident(Incident incident);
    void deleteByIncident(Incident incident);
}

