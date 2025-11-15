# Virtual Bank System - Setup Guide

## Prerequisites

- **Java 21** (check with `java -version`)
- **Maven** (check with `mvn -version`)
- **Docker & Docker Compose** (check with `docker --version` and `docker-compose --version`)
- **Node.js 18+** and **npm** (check with `node -version` and `npm -version`)

## Step 1: Start Infrastructure (Kafka + PostgreSQL)

Start Kafka, Zookeeper, and PostgreSQL using Docker Compose:

```bash
docker-compose up -d
```

This will:
- Start Zookeeper on port 2181
- Start Kafka on port 9092
- Start PostgreSQL on port 5432
- Automatically create three databases:
  - `virtual_bank_user_service`
  - `virtual_bank_account_service`
  - `virtual_bank_logging_service`

**Verify containers are running:**
```bash
docker ps
```

You should see three containers: `zookeeper`, `kafka`, and `postgres-virtual-bank`

## Step 2: Start the Microservices

Open **5 separate terminal windows** and start each service in order:

### Terminal 1 - User Service
```bash
cd user-service
mvn spring-boot:run
```
Wait for: "Started UserServiceApplication" (Port 8081)

### Terminal 2 - Account Service
```bash
cd account-service
mvn spring-boot:run
```
Wait for: "Started AccountServiceApplication" (Port 8082)

### Terminal 3 - Transaction Service
```bash
cd transaction-service
mvn spring-boot:run
```
Wait for: "Started TransactionServiceApplication" (Port 8083)

### Terminal 4 - Logging Service
```bash
cd logging-service
mvn spring-boot:run
```
Wait for: "Started LoggingServiceApplication" (Port 8085)

### Terminal 5 - BFF Service
```bash
cd bff-service
mvn spring-boot:run
```
Wait for: "Started BffServiceApplication" (Port 8080)

## Step 3: Start the Frontend

In a **6th terminal**:

```bash
cd frontend
npm install   # Only needed first time
npm run dev
```

The frontend will be available at: **http://localhost:5173**

## Troubleshooting

### Database Connection Issues

If you see database connection errors, verify PostgreSQL is running:
```bash
docker logs postgres-virtual-bank
```

Check if databases were created:
```bash
docker exec -it postgres-virtual-bank psql -U postgres -c "\l"
```

### Kafka Connection Issues

If services can't connect to Kafka:
```bash
docker logs kafka
```

Ensure Kafka is healthy before starting microservices.

### Port Already in Use

If a port is already in use, find and kill the process:
```bash
# Linux/Mac
lsof -i :8081  # Replace with the port number
kill -9 <PID>

# Windows
netstat -ano | findstr :8081
taskkill /PID <PID> /F
```

### Clean Restart

To completely reset everything:
```bash
# Stop all containers
docker-compose down -v

# Remove volumes (deletes all data)
docker volume prune -f

# Start fresh
docker-compose up -d
```

## Database Credentials

All services use the same PostgreSQL credentials:
- **Host:** localhost
- **Port:** 5432
- **Username:** postgres
- **Password:** 8368

## Service Ports

| Service | Port | URL |
|---------|------|-----|
| BFF Service | 8080 | http://localhost:8080 |
| User Service | 8081 | http://localhost:8081 |
| Account Service | 8082 | http://localhost:8082 |
| Transaction Service | 8083 | http://localhost:8083 |
| Logging Service | 8085 | (No REST API) |
| Frontend | 5173 | http://localhost:5173 |

## Testing the Application

1. Open http://localhost:5173 in your browser
2. Register a new user
3. Log in with your credentials
4. Create a savings or checking account
5. Create another account for testing transfers
6. Initiate a transfer between accounts
7. View transaction history

## Stopping Everything

### Stop Docker containers:
```bash
docker-compose down
```

### Stop microservices:
Press `Ctrl+C` in each terminal running a Spring Boot service

### Stop frontend:
Press `Ctrl+C` in the terminal running Vite
