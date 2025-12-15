# CiteSignal - AI Coding Guidelines

## Architecture Overview

CiteSignal is a Spring Boot 3.2.0 municipal incident reporting platform with MVC architecture:

- **Backend**: Spring Boot (Web, Data JPA, Security, Thymeleaf, Mail, Validation, WebSocket)
- **Database**: MySQL with JPA/Hibernate (ddl-auto=update)
- **Security**: JWT-based authentication with role-based access (CITOYEN, AGENT_MUNICIPAL, ADMINISTRATEUR)
- **Frontend**: Thymeleaf templates with Bootstrap 5, French localization
- **File Handling**: Multipart uploads stored in `uploads/` directory

## Core Domain Model

- **User**: nom, prenom, email, telephone, role, enabled, emailVerified
- **Incident**: titre, description, categorie (INFRASTRUCTURE/PROPRETE/SECURITE/etc.), statut (SIGNALE->PRIS_EN_CHARGE->EN_RESOLUTION->RESOLU/CLOTURE), priorite (BASSE/MOYENNE/HAUTE/URGENTE), adresse, lat/lng, photos, notifications
- **Relationships**: Incident belongs to citoyen (User), assigned to agent (User), located in quartier/departement

## Development Workflow

- **Build**: `mvn clean install` or `./mvnw clean install`
- **Run**: `mvn spring-boot:run` or `./mvnw spring-boot:run`
- **Database**: Auto-creates schema on startup (hibernate.hbm2ddl.auto=update)
- **Config**: Edit `src/main/resources/application.properties` for DB credentials, JWT secret, email settings

## Coding Patterns

- **Entities**: JPA with Lombok (@Data, @NoArgsConstructor, @AllArgsConstructor), validation annotations
- **DTOs**: Request/response objects in `dto/` package with validation
- **Controllers**: @Controller with Thymeleaf views, @PreAuthorize("hasRole('ROLE')") for security
- **Services**: @Service with @Transactional, business logic separated from controllers
- **Security**: JWT tokens, UserPrincipal for authenticated user access
- **Templates**: Thymeleaf with Spring Security extras, French text, Bootstrap styling
- **File Uploads**: MultipartFile handling, stored via FileStorageService

## Key Conventions

- **Language**: French for UI text, English for code/comments
- **URLs**: RESTful paths (/incidents, /users), CSRF protected forms
- **Validation**: Bean validation on DTOs with custom French messages
- **Error Handling**: RedirectAttributes for flash messages, try-catch in controllers
- **Dependencies**: Injected with @Autowired (field injection pattern)
- **Enums**: Defined inside entities (Incident.Categorie, Incident.Statut, etc.)

## Common Tasks

- **Add new incident category**: Update Incident.Categorie enum and template dropdowns
- **New user role**: Add to User.Role enum, update security config and templates
- **Email notifications**: Use EmailService with thymeleaf templates in resources/templates
- **File uploads**: Handle MultipartFile in controllers, save via FileStorageService
- **Statistics**: Use StatisticsService for dashboard metrics

## Testing

- Unit tests in `src/test/java/` (currently minimal)
- Integration tests would use @SpringBootTest with TestContainers for MySQL

## Deployment Notes

- Production: Change JWT secret, disable ddl-auto, configure proper DB
- Email: Use app-specific passwords or SMTP service
- Static files: Served from `src/main/resources/static/`
