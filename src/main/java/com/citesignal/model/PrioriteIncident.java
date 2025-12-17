package com.citesignal.model;

public enum PrioriteIncident {
    BASSE,
    MOYENNE,
    HAUTE,
    URGENTE;

    public String getLibelle() {
        return switch (this) {
            case BASSE -> "Basse";
            case MOYENNE -> "Moyenne";
            case HAUTE -> "Haute";
            case URGENTE -> "Urgente";
        };
    }
}