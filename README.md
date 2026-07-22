# Employee Management System (EMS) — NADRA

A modern, robust, and secure Employee Management System built with Java 25 and Spring Boot 4.1.0. This project follows a **Layer-wise Hexagonal Architecture** (Ports and Adapters) to ensure a clean separation of concerns, high testability, and maintainability.

## 🚀 Technology Stack

- **Java Version:** 25
- **Framework:** Spring Boot 4.1.0 (WebMVC)
- **Security:** Spring Security, JWT (JSON Web Tokens), TOTP (Google Authenticator compatible 2FA)
- **Database:** PostgreSQL (via Spring JDBC with `JdbcClient`)
- **Documentation:** OpenAPI / Swagger (`springdoc-openapi`)
- **Logging:** Structured JSON Logging for Grafana/Loki (`logstash-logback-encoder`)
- **Tools:** Maven, Lombok

## 🏗️ Architecture

The project is structured around a full Layer-wise Hexagonal Architecture. 
Instead of grouping by module, the top-level packages represent architectural layers:

- **`com.nadra.ems.domain`**: The core business logic and domain models. Contains business models, use case interfaces (inbound ports), and repository interfaces (outbound ports).
- **`com.nadra.ems.adapter`**: The external-facing components.
  - **`in.web`**: REST Controllers, DTOs, and Request Mappers.
  - **`out.persistence`**: JDBC Repository implementations and Data Access Exception Translators.
- **`com.nadra.ems.infrastructure`**: Application configurations, security settings, JWT filters, and password encoders.
- **`com.nadra.ems.common`**: Shared exceptions, standardized API responses, and global configurations (e.g., Swagger config, Global Exception Handler).

## 🔒 Security Features

- **JWT Authentication:** Stateless authentication using signed JWT tokens for high performance and scalability.
- **Two-Factor Authentication (2FA):** Integrated TOTP (Time-based One-Time Password) for enhanced account security.
- **Role-Based Access Control (RBAC):** Access control configured seamlessly via Spring Security.
- **Account Lockout:** Built-in safeguards that automatically lock accounts after consecutive failed login attempts.

## 🛠️ Getting Started

### Prerequisites

- Java 25
- Maven
- PostgreSQL Database

### Setup

1. **Database Configuration:** Ensure your PostgreSQL credentials match the settings in `src/main/resources/application.yml` or `application.properties`.
2. **Build the Application:**
   ```bash
   ./mvnw clean compile
   ```
3. **Run the Application:**
   ```bash
   ./mvnw spring-boot:run
   ```

## 📚 API Documentation

Once the application is running, you can access the Swagger UI to view and interactively test all REST API endpoints:

- **Swagger UI:** `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON Docs:** `http://localhost:8080/v3/api-docs`
