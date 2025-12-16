package com.citesignal.model;

public enum StatutIncident {
    SIGNALE,
    PRIS_EN_CHARGE,
    EN_RESOLUTION,
    RESOLU,
    CLOTURE;

    public String getLibelle() {
        return switch (this) {
            case SIGNALE -> "Signalé";
            case PRIS_EN_CHARGE -> "Pris en charge";
            case EN_RESOLUTION -> "En résolution";
            case RESOLU -> "Résolu";
            case CLOTURE -> "Clôturé";
        };
    }
}