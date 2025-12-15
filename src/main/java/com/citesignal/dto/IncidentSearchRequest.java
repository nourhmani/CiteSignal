package com.citesignal.dto;

import com.citesignal.model.Incident;
import lombok.Data;

import java.time.LocalDate;

@Data
public class IncidentSearchRequest {
    private Incident.Statut statut;
    private Incident.Categorie categorie;
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

