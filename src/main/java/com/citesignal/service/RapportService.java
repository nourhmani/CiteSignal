package com.citesignal.service;

import com.citesignal.model.Rapport;
import com.citesignal.model.User;
import com.citesignal.model.Incident;
import com.citesignal.repository.RapportRepository;
import com.citesignal.repository.IncidentRepository;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class RapportService {

    @Autowired
    private RapportRepository rapportRepository;

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private StatisticsService statisticsService;

    @Transactional
    public Rapport genererRapportStatistiques(User user, LocalDate dateDebut, LocalDate dateFin, Rapport.FormatExport format) {
        try {
            // Récupérer les statistiques
            Map<String, Object> stats = statisticsService.getGeneralStatistics();

            // Récupérer les incidents de la période - Utilisez la bonne méthode du repository
            List<Incident> incidents = incidentRepository.findByCreatedAtBetween(
                    dateDebut.atStartOfDay(),
                    dateFin.atTime(23, 59, 59)
            );

            // Générer le contenu du rapport
            StringBuilder contenu = new StringBuilder();
            contenu.append("RAPPORT DE STATISTIQUES DES INCIDENTS - CITESIGNAL\n");
            contenu.append("=".repeat(60)).append("\n\n");
            contenu.append("Période : ").append(dateDebut.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            contenu.append(" à ").append(dateFin.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append("\n");
            contenu.append("Date de génération : ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))).append("\n");
            contenu.append("Généré par : ").append(user.getNom()).append(" ").append(user.getPrenom()).append("\n");
            contenu.append("-".repeat(60)).append("\n\n");

            // Statistiques générales
            contenu.append("STATISTIQUES GÉNÉRALES\n");
            contenu.append("-".repeat(30)).append("\n");
            contenu.append(String.format("• Total des incidents : %d\n", stats.get("totalIncidents")));
            contenu.append(String.format("• Incidents des 30 derniers jours : %d\n", stats.get("recentIncidents")));
            contenu.append(String.format("• Incidents résolus (30 jours) : %d\n", stats.get("resolvedLast30Days")));
            contenu.append(String.format("• Taux de résolution : %.1f%%\n\n", stats.get("resolutionRate")));

            // Répartition par statut
            Map<String, Long> incidentsByStatus = (Map<String, Long>) stats.get("incidentsByStatus");
            if (incidentsByStatus != null && !incidentsByStatus.isEmpty()) {
                contenu.append("RÉPARTITION PAR STATUT\n");
                contenu.append("-".repeat(30)).append("\n");
                incidentsByStatus.forEach((statut, count) ->
                        contenu.append(String.format("• %s : %d\n", statut, count)));
                contenu.append("\n");
            }

            // Répartition par catégorie
            Map<String, Long> incidentsByCategory = (Map<String, Long>) stats.get("incidentsByCategory");
            if (incidentsByCategory != null && !incidentsByCategory.isEmpty()) {
                contenu.append("RÉPARTITION PAR CATÉGORIE\n");
                contenu.append("-".repeat(30)).append("\n");
                incidentsByCategory.forEach((categorie, count) ->
                        contenu.append(String.format("• %s : %d\n", categorie, count)));
                contenu.append("\n");
            }

            // Répartition par quartier
            Map<String, Long> incidentsByQuartier = (Map<String, Long>) stats.get("incidentsByQuartier");
            if (incidentsByQuartier != null && !incidentsByQuartier.isEmpty()) {
                contenu.append("RÉPARTITION PAR QUARTIER\n");
                contenu.append("-".repeat(30)).append("\n");
                incidentsByQuartier.forEach((quartier, count) ->
                        contenu.append(String.format("• %s : %d\n", quartier, count)));
                contenu.append("\n");
            }

            // Détail des incidents de la période
            if (!incidents.isEmpty()) {
                contenu.append("DÉTAIL DES INCIDENTS DE LA PÉRIODE (").append(incidents.size()).append(" incidents)\n");
                contenu.append("-".repeat(60)).append("\n");
                contenu.append(String.format("%-6s %-40s %-20s %-15s %-10s\n",
                        "ID", "Titre", "Catégorie", "Statut", "Date"));
                contenu.append("-".repeat(100)).append("\n");

                for (Incident incident : incidents) {
                    contenu.append(String.format("%-6d %-40s %-20s %-15s %-10s\n",
                            incident.getId(),
                            incident.getTitre().length() > 38 ? incident.getTitre().substring(0, 35) + "..." : incident.getTitre(),
                            // Categorie est une énumération, utilisez .name() au lieu de .getNom()
                            incident.getCategorie() != null ?
                                    (incident.getCategorie().name().length() > 18 ?
                                            incident.getCategorie().name().substring(0, 15) + "..." :
                                            incident.getCategorie().name()) : "N/A",
                            incident.getStatut().toString().length() > 12 ?
                                    incident.getStatut().toString().substring(0, 9) + "..." :
                                    incident.getStatut().toString(),
                            // Utilisez createdAt au lieu de dateSignalement
                            incident.getCreatedAt() != null ?
                                    incident.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "N/A"
                    ));
                }
            } else {
                contenu.append("AUCUN INCIDENT POUR LA PÉRIODE SÉLECTIONNÉE\n");
            }

            // Créer le rapport
            Rapport rapport = new Rapport();
            rapport.setTitre(String.format("Rapport statistiques %s - %s",
                    dateDebut.format(DateTimeFormatter.ofPattern("ddMMyyyy")),
                    dateFin.format(DateTimeFormatter.ofPattern("ddMMyyyy"))));
            rapport.setContenu(contenu.toString());
            rapport.setType(Rapport.TypeRapport.STATISTIQUES_GENERALES);
            rapport.setDateDebut(dateDebut);
            rapport.setDateFin(dateFin);
            rapport.setCreatedBy(user);
            rapport.setFormatExport(format);

            // Générer le fichier
            String fileName = genererNomFichier(rapport);
            String filePath = genererFichierRapport(rapport, fileName, format, incidents);
            rapport.setFichierChemin(filePath);

            return rapportRepository.save(rapport);

        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la génération du rapport: " + e.getMessage(), e);
        }
    }

    private String genererNomFichier(Rapport rapport) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String baseName = "rapport_stats_" + timestamp;

        switch (rapport.getFormatExport()) {
            case PDF:
                return baseName + ".pdf";
            case CSV:
                return baseName + ".csv";
            default:
                return baseName + ".txt";
        }
    }

    private String genererFichierRapport(Rapport rapport, String fileName,
                                         Rapport.FormatExport format, List<Incident> incidents) {
        try {
            // Créer le dossier uploads/rapports s'il n'existe pas
            Path rapportsDir = Paths.get("uploads", "rapports");
            if (!Files.exists(rapportsDir)) {
                Files.createDirectories(rapportsDir);
            }

            Path filePath = rapportsDir.resolve(fileName);

            switch (format) {
                case CSV:
                    genererCSV(rapport, filePath, incidents);
                    break;
                case PDF:
                    genererPDF(rapport, filePath, incidents);
                    break;
                default:
                    genererTexte(rapport, filePath);
            }

            return filePath.toString();

        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la génération du fichier: " + e.getMessage(), e);
        }
    }

    private void genererCSV(Rapport rapport, Path filePath, List<Incident> incidents) throws IOException {
        try (FileWriter writer = new FileWriter(filePath.toFile())) {
            writer.write("\"RAPPORT DE STATISTIQUES DES INCIDENTS - CITESIGNAL\"\n");
            writer.write("\"Période\",\"" + rapport.getDateDebut() + " à " + rapport.getDateFin() + "\"\n");
            writer.write("\"Date de génération\",\"" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) + "\"\n");
            writer.write("\"Généré par\",\"" + rapport.getCreatedBy().getNom() + " " + rapport.getCreatedBy().getPrenom() + "\"\n\n");

            writer.write("SECTION,DONNÉES\n");

            // Statistiques générales
            Map<String, Object> stats = statisticsService.getGeneralStatistics();
            writer.write("\"Total incidents\"," + stats.get("totalIncidents") + "\n");
            writer.write("\"Incidents 30 derniers jours\"," + stats.get("recentIncidents") + "\n");
            writer.write("\"Incidents résolus (30 jours)\"," + stats.get("resolvedLast30Days") + "\n");
            writer.write("\"Taux de résolution\"," + stats.get("resolutionRate") + "\n\n");

            // Détail des incidents
            writer.write("DÉTAIL DES INCIDENTS\n");
            writer.write("ID,Titre,Description,Catégorie,Statut,Quartier,Priorité,Date création\n");

            for (Incident incident : incidents) {
                String quartierNom = incident.getQuartier() != null ?
                        incident.getQuartier().getNom() : "N/A";
                String description = incident.getDescription() != null ?
                        incident.getDescription() : "";

                writer.write(String.format("\"%d\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"\n",
                        incident.getId(),
                        escapeCsv(incident.getTitre()),
                        escapeCsv(description),
                        escapeCsv(incident.getCategorie() != null ? incident.getCategorie().name() : "N/A"),
                        escapeCsv(incident.getStatut().toString()),
                        escapeCsv(quartierNom),
                        escapeCsv(incident.getPriorite() != null ? incident.getPriorite().name() : "N/A"),
                        incident.getCreatedAt() != null ?
                                incident.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "N/A"
                ));
            }

            writer.flush();
        }
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        return value.replace("\"", "\"\"");
    }

    private void genererPDF(Rapport rapport, Path filePath, List<Incident> incidents) throws Exception {
        Document document = new Document();
        PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(filePath.toFile()));

        document.open();

        // En-tête
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
        Paragraph title = new Paragraph("RAPPORT DE STATISTIQUES DES INCIDENTS", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(20);
        document.add(title);

        // Informations du rapport
        Font infoFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
        Paragraph info = new Paragraph();
        info.add(new Chunk("Période : ", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
        info.add(new Chunk(rapport.getDateDebut() + " à " + rapport.getDateFin() + "\n", infoFont));
        info.add(new Chunk("Date de génération : ", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
        info.add(new Chunk(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) + "\n", infoFont));
        info.add(new Chunk("Généré par : ", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
        info.add(new Chunk(rapport.getCreatedBy().getNom() + " " + rapport.getCreatedBy().getPrenom() + "\n\n", infoFont));
        info.setSpacingAfter(20);
        document.add(info);

        // Statistiques générales
        Map<String, Object> stats = statisticsService.getGeneralStatistics();
        Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
        Paragraph sectionTitle = new Paragraph("STATISTIQUES GÉNÉRALES", sectionFont);
        sectionTitle.setSpacingAfter(10);
        document.add(sectionTitle);

        PdfPTable statsTable = new PdfPTable(2);
        statsTable.setWidthPercentage(100);
        statsTable.setSpacingBefore(10);
        statsTable.setSpacingAfter(20);

        statsTable.addCell(createCell("Total incidents", true));
        statsTable.addCell(createCell(stats.get("totalIncidents").toString(), false));
        statsTable.addCell(createCell("Incidents des 30 derniers jours", true));
        statsTable.addCell(createCell(stats.get("recentIncidents").toString(), false));
        statsTable.addCell(createCell("Incidents résolus (30 jours)", true));
        statsTable.addCell(createCell(stats.get("resolvedLast30Days").toString(), false));
        statsTable.addCell(createCell("Taux de résolution", true));
        statsTable.addCell(createCell(stats.get("resolutionRate") + "%", false));

        document.add(statsTable);

        // Table des incidents
        if (!incidents.isEmpty()) {
            Paragraph incidentsTitle = new Paragraph("DÉTAIL DES INCIDENTS (" + incidents.size() + " incidents)", sectionFont);
            incidentsTitle.setSpacingAfter(10);
            document.add(incidentsTitle);

            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10);

            // En-têtes
            table.addCell(createCell("ID", true));
            table.addCell(createCell("Titre", true));
            table.addCell(createCell("Catégorie", true));
            table.addCell(createCell("Statut", true));
            table.addCell(createCell("Date création", true));

            // Données
            Font dataFont = FontFactory.getFont(FontFactory.HELVETICA, 8);
            for (Incident incident : incidents) {
                table.addCell(new PdfPCell(new Phrase(String.valueOf(incident.getId()), dataFont)));
                table.addCell(new PdfPCell(new Phrase(
                        incident.getTitre().length() > 30 ? incident.getTitre().substring(0, 27) + "..." : incident.getTitre(),
                        dataFont)));
                table.addCell(new PdfPCell(new Phrase(
                        incident.getCategorie() != null ? incident.getCategorie().name() : "N/A",
                        dataFont)));
                table.addCell(new PdfPCell(new Phrase(incident.getStatut().toString(), dataFont)));
                table.addCell(new PdfPCell(new Phrase(
                        incident.getCreatedAt() != null ?
                                incident.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "N/A",
                        dataFont)));
            }

            document.add(table);
        } else {
            Paragraph noData = new Paragraph("Aucun incident pour la période sélectionnée", infoFont);
            noData.setAlignment(Element.ALIGN_CENTER);
            noData.setSpacingBefore(20);
            document.add(noData);
        }

        // Pied de page
        document.add(new Paragraph(" "));
        Paragraph footer = new Paragraph("Rapport généré automatiquement par le système CiteSignal",
                FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8));
        footer.setAlignment(Element.ALIGN_CENTER);
        document.add(footer);

        document.close();
        writer.close();
    }

    private PdfPCell createCell(String text, boolean isHeader) {
        Font font = isHeader ?
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9) :
                FontFactory.getFont(FontFactory.HELVETICA, 9);

        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(5);
        cell.setBackgroundColor(isHeader ? new BaseColor(220, 220, 220) : BaseColor.WHITE);
        return cell;
    }

    private void genererTexte(Rapport rapport, Path filePath) throws IOException {
        Files.write(filePath, rapport.getContenu().getBytes());
    }

    public Rapport findById(Long id) {
        return rapportRepository.findById(id).orElseThrow(() ->
                new RuntimeException("Rapport non trouvé avec l'ID: " + id));
    }

    public byte[] getFileContent(Rapport rapport) throws IOException {
        if (rapport.getFichierChemin() == null) {
            throw new RuntimeException("Fichier non trouvé pour ce rapport");
        }

        Path filePath = Paths.get(rapport.getFichierChemin());
        if (!Files.exists(filePath)) {
            throw new RuntimeException("Fichier physique non trouvé: " + filePath);
        }

        return Files.readAllBytes(filePath);
    }
}