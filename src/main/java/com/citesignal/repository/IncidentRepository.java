package com.citesignal.repository;

import com.citesignal.model.Incident;
import com.citesignal.model.User;
import com.citesignal.model.CategorieIncident;
import com.citesignal.model.StatutIncident;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, Long> {
    
    @EntityGraph(attributePaths = {"citoyen", "agent", "quartier", "departement"})
    Page<Incident> findByCitoyen(User citoyen, Pageable pageable);
    
    @EntityGraph(attributePaths = {"citoyen", "agent", "quartier", "departement"})
    Page<Incident> findByAgent(User agent, Pageable pageable);
    
    @EntityGraph(attributePaths = {"citoyen", "agent", "quartier", "departement", "photos"})
    @Override
    Optional<Incident> findById(Long id);
    
    List<Incident> findByCitoyen(User citoyen);
    
    List<Incident> findByAgent(User agent);
    
    Page<Incident> findByStatut(StatutIncident statut, Pageable pageable);
    
    Page<Incident> findByCategorie(CategorieIncident categorie, Pageable pageable);
    
    Page<Incident> findByQuartierId(Long quartierId, Pageable pageable);
    
    Page<Incident> findByDepartementId(Long departementId, Pageable pageable);
    
    @EntityGraph(attributePaths = {"citoyen", "agent", "quartier", "departement"})
    @Query("SELECT i FROM Incident i WHERE " +
           "(:statut IS NULL OR i.statut = :statut) AND " +
           "(:categorie IS NULL OR i.categorie = :categorie) AND " +
           "(:quartierId IS NULL OR i.quartier.id = :quartierId) AND " +
           "(:departementId IS NULL OR i.departement.id = :departementId) AND " +
           "(:dateDebut IS NULL OR i.createdAt >= :dateDebut) AND " +
           "(:dateFin IS NULL OR i.createdAt <= :dateFin) AND " +
           "(:recherche IS NULL OR LOWER(i.titre) LIKE LOWER(CONCAT('%', :recherche, '%')) OR " +
           "LOWER(i.description) LIKE LOWER(CONCAT('%', :recherche, '%')) OR " +
           "LOWER(i.adresse) LIKE LOWER(CONCAT('%', :recherche, '%')))")
    Page<Incident> searchIncidents(
            @Param("statut") StatutIncident statut,
            @Param("categorie") CategorieIncident categorie,
            @Param("quartierId") Long quartierId,
            @Param("departementId") Long departementId,
            @Param("dateDebut") LocalDateTime dateDebut,
            @Param("dateFin") LocalDateTime dateFin,
            @Param("recherche") String recherche,
            Pageable pageable
    );
    
    @Query("SELECT COUNT(i) FROM Incident i WHERE i.statut = :statut")
    Long countByStatut(@Param("statut") StatutIncident statut);
    
    @Query("SELECT COUNT(i) FROM Incident i WHERE i.categorie = :categorie")
    Long countByCategorie(@Param("categorie") CategorieIncident categorie);
    
    @Query("SELECT i FROM Incident i WHERE i.createdAt >= :dateDebut AND i.createdAt <= :dateFin")
    List<Incident> findByDateRange(@Param("dateDebut") LocalDateTime dateDebut, 
                                    @Param("dateFin") LocalDateTime dateFin);
    
    @Query("SELECT q.nom, COUNT(i) FROM Incident i JOIN i.quartier q GROUP BY q.nom ORDER BY COUNT(i) DESC")
    List<Object[]> countIncidentsByQuartier();
    List<Incident> findByCreatedAtBetween(LocalDateTime dateDebut, LocalDateTime dateFin);

}

