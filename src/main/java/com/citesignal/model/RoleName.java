package com.citesignal.model;

public enum RoleName {
    CITOYEN,
    AGENT_MUNICIPAL,
    ADMINISTRATEUR,
    SUPERADMIN;

    public String getLibelle() {
        return switch (this) {
            case CITOYEN -> "Citoyen";
            case AGENT_MUNICIPAL -> "Agent Municipal";
            case ADMINISTRATEUR -> "Administrateur";
            case SUPERADMIN -> "Super Administrateur";
        };
    }

    public String getAuthority() {
        return "ROLE_" + this.name();
    }
}