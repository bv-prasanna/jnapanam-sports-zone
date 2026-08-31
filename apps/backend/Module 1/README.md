# Jnapanam Sports Zone Backend — Module 01

This is the first runnable backend module. It includes the platform foundation and customer authentication.

## Included

- Java 21 and Spring Boot 4.1
- PostgreSQL 17 with Docker Compose
- Flyway versioned schema
- Customer registration and login
- JWT access tokens and rotating refresh tokens
- Logout and authenticated profile endpoint
- Roles: CUSTOMER, ADMIN, OPERATIONS, COACH, ORGANIZER, SUPER_ADMIN
- Standard validation and API errors
- Swagger/OpenAPI documentation
- Actuator health endpoint
- Unit tests

## Requirements

- Java 21
- Maven 3.6.3+
- Docker Desktop

## Start PostgreSQL

From this folder:

```powershell
docker compose up -d
docker compose ps
```

## Run tests

```powershell
mvn clean test
```

## Start the backend

```powershell
mvn spring-boot:run
```

Open:

- Health: http://localhost:8080/actuator/health
- Swagger: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

## API sequence

### 1. Register

`POST /api/v1/auth/register`

```json
{
  "fullName": "Prasanna Kumar",
  "email": "prasanna@example.com",
  "mobile": "+918296022107",
  "password": "ChangeMe123!"
}
```

### 2. Login

`POST /api/v1/auth/login`

```json
{
  "email": "prasanna@example.com",
  "password": "ChangeMe123!"
}
```

### 3. View profile

`GET /api/v1/profile/me` with header:

```text
Authorization: Bearer <accessToken>
```

### 4. Refresh

`POST /api/v1/auth/refresh`

```json
{
  "refreshToken": "<refreshToken>"
}
```

Refresh tokens rotate: after a successful refresh, replace the old token with the newly returned token.

## Important production configuration

Copy `.env.example` to `.env` only for reference. Set environment variables through your deployment platform. Never commit a production database password or JWT secret.

Generate a production JWT secret using PowerShell:

```powershell
$bytes = New-Object byte[] 32
[System.Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
[Convert]::ToBase64String($bytes)
```

## Next module

Module 02 adds sports, facilities, operating hours, closures, pricing foundations and slot generation.
