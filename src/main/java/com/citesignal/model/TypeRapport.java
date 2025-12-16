package com.citesignal.model;

public enum TypeRapport {
    STATISTIQUES_GENERALES,
    INCIDENTS_PAR_CATEGORIE,
    INCIDENTS_PAR_QUARTIER,
    PERFORMANCE_AGENTS,
    DELAIS_RESOLUTION,
    SATISFACTION_CITOYENS,
    CUSTOM;

    public String getLibelle() {
        return switch (this) {
            case STATISTIQUES_GENERALES -> "Statistiques générales";
            case INCIDENTS_PAR_CATEGORIE -> "Incidents par catégorie";
            case INCIDENTS_PAR_QUARTIER -> "Incidents par quartier";
            case PERFORMANCE_AGENTS -> "Performance des agents";
            case DELAIS_RESOLUTION -> "Délais de résolution";
            case SATISFACTION_CITOYENS -> "Satisfaction des citoyens";
            case CUSTOM -> "Personnalisé";
        };
    }
}