package com.citesignal.dto;

import com.citesignal.model.Incident;
import com.citesignal.model.StatutIncident;
import com.citesignal.model.CategorieIncident;
import lombok.Data;

import java.time.LocalDate;

@Data
public class IncidentSearchRequest {
    private StatutIncident statut;
    private CategorieIncident categorie;
    private Long quartierId;
    private Long departementId;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private String recherche;
    private int page = 0;
    private int size = 10;
    private String sortBy = "createdAt";
    private String sortDir = "DESC";
}

