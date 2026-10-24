# Securely Authentication App


WEBSITE URL-> [securely-nine.vercel.app](https://securely-nine.vercel.app/)
A full-stack authentication application built with **Spring Boot, React, MySQL, JWT, and Docker**. It demonstrates user registration, login, email verification, password recovery, and protected API access.

## Features

- User registration and login
- JWT-based authentication
- Email verification workflow
- Forgot-password and reset-password workflow
- Role-based authorization for protected endpoints
- Login rate-limiting filter
- MySQL database integration
- React frontend served through Nginx
- Docker Compose setup for local development
- Environment-based configuration for application secrets

## Tech Stack

| Layer | Technology |
|---|---|
| Frontend | React, Vite, JavaScript, CSS |
| Backend | Java, Spring Boot, Spring Security |
| Authentication | JWT |
| Database | MySQL |
| Email | SMTP |
| Web server | Nginx |
| Containerization | Docker, Docker Compose |
| Build tools | Maven, npm |

## Architecture

mermaid
flowchart TD
    A[User / Browser] --> B[React Frontend]
    B --> C[Nginx Reverse Proxy]
    C --> D[Spring Boot REST API]
    D --> E[Spring Security and JWT]
    D --> F[(MySQL Database)]
    D --> G[SMTP Email Service]


## Project Structure

text
Securely-Authentication-App/
├── backend/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   └── resources/
│   │   └── test/
│   ├── dockerfile
│   ├── pom.xml
│   └── env.example
├── frontend/
│   ├── src/
│   ├── public/
│   ├── nginx.conf
│   ├── dockerfile
│   └── package.json
├── docker-compose.yml
└── .gitignore


## Getting Started

### 1. Clone the repository

bash
git clone https://github.com/princethakur0881/Securely-Authentication-App.git
cd Securely-Authentication-App


### 2. Configure environment variables

Create the root .env file using the variable names expected by docker-compose.yml and backend/src/main/resources/application.properties.

Typical configuration categories include:

- MySQL database name and credentials
- SMTP username and app password
- JWT signing secret, if configured through environment variables

Use placeholders in example files. **Never commit actual passwords, app passwords, or JWT secrets.**

### 3. Start the application

bash
docker compose up -d --build


### 4. Check running containers

bash
docker compose ps


View logs:

bash
docker compose logs --tail=100


### 5. Open the frontend

With the current local Docker port mapping:

http://localhost:3000

### 6. Stop the application

bash
docker compose down


To remove database volumes as well, use docker compose down -v only when you intentionally want to delete the stored database data.

## API Overview

| Endpoint | Purpose |
|---|---|
| POST /auth/register | Register a user |
| POST /auth/login | Authenticate a user |
| /auth/...| Email verification and password recovery |
| /api/admin/... | Admin-related protected endpoints |

Check the corresponding controller classes for the exact HTTP methods, request fields, and responses.

### JWT Authorization

For protected endpoints, include the token in the request header:

http
Authorization: Bearer <your-jwt-token>


Protected resources should validate the token and the user's authorization before returning data.

## Authentication Flow

1. The user submits registration details.
2. The backend validates the request and stores the user with a hashed password.
3. The email-verification process confirms the email address.
4. The user logs in with valid credentials.
5. The backend issues a JWT after successful authentication.
6. The frontend sends the token with protected API requests.
7. Spring Security checks authentication and authorization.
8. Password recovery uses the configured email/reset workflow.

## Security Considerations

- Never store plaintext passwords.
- Keep .env files and credentials out of Git.
- Protect administrative APIs on the backend, not just in the frontend.
- Do not allow public registration to assign privileged roles.
- Configure CORS for trusted origins only.
- Use HTTPS and appropriate token storage in production.
- Review token expiry, reset-token expiry, and rate limiting.
- Rotate credentials if they are ever exposed.

This project is intended for learning and portfolio use. Perform security testing before deploying it to production.

## Development

### Backend tests

cd backend
./mvnw test


### Frontend development

bash
cd frontend
npm install
npm run dev


Check frontend/package.json for the available scripts and docker-compose.yml for the configured service ports.

## Roadmap

- [ ] Expand automated tests for registration and login
- [ ] Test email verification and password recovery
- [ ] Add authorization tests for protected endpoints
- [ ] Document all API request and response examples
- [ ] Add application screenshots
- [ ] Configure continuous integration
- [ ] Add production deployment instructions

## Author

**Prince Kumar**

- GitHub: [@princethakur0881](https://github.com/princethakur0881)
- Repository: [Securely-Authentication-App](https://github.com/princethakur0881/Securely-Authentication-App)

---

Built as a full-stack authentication and security learning project.
