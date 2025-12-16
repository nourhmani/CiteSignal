package com.citesignal.model;

public enum FormatExport {
    CSV,
    PDF,
    EXCEL;

    public String getLibelle() {
        return switch (this) {
            case CSV -> "CSV";
            case PDF -> "PDF";
            case EXCEL -> "Excel";
        };
    }

    public String getExtension() {
        return switch (this) {
            case CSV -> ".csv";
            case PDF -> ".pdf";
            case EXCEL -> ".xlsx";
        };
    }
}