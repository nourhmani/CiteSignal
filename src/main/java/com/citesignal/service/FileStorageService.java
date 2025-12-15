package com.citesignal.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class FileStorageService {
    
    private static final Logger logger = LoggerFactory.getLogger(FileStorageService.class);
    
    @Value("${app.upload.dir:uploads}")
    private String uploadDir;
    
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final List<String> ALLOWED_CONTENT_TYPES = List.of(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
    );
    
    public String storeFile(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Le fichier est vide");
        }
        
        // Vérifier la taille
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Le fichier est trop volumineux (max 10MB)");
        }
        
        // Vérifier le type MIME
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Type de fichier non autorisé. Seuls les images sont acceptées.");
        }
        
        // Créer le répertoire s'il n'existe pas
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        // Générer un nom de fichier unique
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String uniqueFilename = UUID.randomUUID().toString() + extension;
        
        // Sauvegarder le fichier
        Path filePath = uploadPath.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        
        logger.info("Fichier sauvegardé: {}", filePath);
        return uniqueFilename;
    }
    
    public List<String> storeFiles(List<MultipartFile> files) throws IOException {
        List<String> storedFilenames = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file != null && !file.isEmpty()) {
                storedFilenames.add(storeFile(file));
            }
        }
        return storedFilenames;
    }
    
    public Path loadFile(String filename) {
        return Paths.get(uploadDir).resolve(filename).normalize();
    }
    
    public boolean deleteFile(String filename) {
        try {
            Path filePath = loadFile(filename);
            return Files.deleteIfExists(filePath);
        } catch (IOException e) {
            logger.error("Erreur lors de la suppression du fichier: {}", filename, e);
            return false;
        }
    }
    
    public String getUploadDir() {
        return uploadDir;
    }
    
    public String getFileUrl(String filename) {
        return "/uploads/" + filename;
    }
}

