package com.citesignal.model;

public enum CategorieIncident {
    INFRASTRUCTURE,
    PROPRETE,
    SECURITE,
    SIGNALISATION,
    ECLAIRAGE,
    EAU_ASSANISSEMENT,
    AUTRE;

    public String getLibelle() {
        return switch (this) {
            case INFRASTRUCTURE -> "Infrastructure";
            case PROPRETE -> "Propreté";
            case SECURITE -> "Sécurité";
            case SIGNALISATION -> "Signalisation";
            case ECLAIRAGE -> "Éclairage";
            case EAU_ASSANISSEMENT -> "Eau et Assainissement";
            case AUTRE -> "Autre";
        };
    }
}