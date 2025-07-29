# Virtual Bank System - Microservices Architecture

This project is a fully functional, distributed virtual banking system designed to demonstrate modern application architecture principles. It utilizes a microservices approach, with independent services for handling users, accounts, and transactions. The system also features a Backend for Frontend (BFF) service to aggregate data for client applications and asynchronous, centralized logging via Apache Kafka.

## Core Architectural Concepts

-   **Microservices:** The system is divided into loosely coupled, independently deployable services, each responsible for a specific business domain (Users, Accounts, Transactions, Logging). This promotes scalability, resilience, and maintainability.
-   **Backend for Frontend (BFF):** A dedicated BFF service acts as an aggregation layer for front-end clients. It orchestrates calls to multiple downstream microservices to create optimized, consolidated responses for the UI, reducing chattiness and simplifying the client-side logic.
-   **API Gateway Integration:** The architecture is designed to be fronted by an API Gateway like WSO2. The gateway would serve as the single entry point for all external traffic, handling routing, authentication (OAuth2, API Key), rate-limiting, and request transformation.
-   **Asynchronous Logging:** All microservices produce logs for requests and responses, sending them to a central Apache Kafka topic. A dedicated Logging service consumes these messages and persists them for centralized monitoring, auditing, and debugging.

## High-Level Architecture
External Clients (Web/Mobile Apps)
|
v
+------------------------+
|   WSO2 API Gateway     |  (Handles Auth, Routing, Throttling)
+------------------------+
|
v
+------------------------+
|   BFF Service          |  (Aggregates and Orchestrates)
+------------------------+
|         |        |
v         v        v
+----------+ +----------+ +-------------+
| User Svc | | Acct Svc | | Transact Svc|
+----------+ +----------+ +-------------+
|         |        |        |
+---------+--------+--------+
|
v
+------------------------+
|   Apache Kafka Topic   |
+------------------------+
|
v
+------------------------+
|   Logging Service      |  (Consumes logs and saves to DB)
+------------------------+

## Microservices Breakdown

| Service              | Description                                                                                             | Port |
| -------------------- | ------------------------------------------------------------------------------------------------------- | ---- |
| **BFF Service** | Aggregates data from other services to provide a consolidated view for the frontend (e.g., user dashboard). | 8080 |
| **User Service** | Manages user registration, login, and profile information.                                              | 8081 |
| **Account Service** | Manages user bank accounts, including creation, balance updates, and status changes.                    | 8082 |
| **Transaction Service** | Handles the initiation and execution of financial transfers and provides transaction history.             | 8083 |
| **Logging Service** | A Kafka consumer that listens for log messages from all services and persists them to a database.     | -    |

## Technology Stack

-   **Backend:** Java 11+
-   **Framework:** Spring Boot
-   **Messaging:** Apache Kafka
-   **API Gateway:** Designed for WSO2 API Manager
-   **Database:** H2 (in-memory, configurable to MySQL/PostgreSQL)
-   **Build Tool:** Maven / Gradle
-   **API Testing:** Postman

## Getting Started

### Prerequisites

-   JDK 11 or higher
-   Maven or Gradle
-   Docker and Docker Compose (for running Kafka and other dependencies)
-   Postman or a similar API client for testing

### Installation & Setup

1.  **Clone the repository:**
    ```bash
    git clone <your-repository-url>
    cd <your-repository-directory>
    ```

2.  **Start Dependencies (Kafka):**
    A `docker-compose.yml` file should be used to easily start Kafka and Zookeeper.
    ```bash
    docker-compose up -d
    ```

3.  **Configure Microservices:**
    Review the `application.properties` file in each microservice's `src/main/resources` directory. Ensure the service URLs (`user.service.url`, `account.service.url`, etc.) and Kafka server (`spring.kafka.producer.bootstrap-servers`) are correctly configured for your local environment (e.g., `localhost`).

4.  **Build and Run the Services:**
    Open a separate terminal for each microservice (`user-service`, `account-service`, `transaction-service`, `bff-service`, `logging-service`).

    Navigate to the root directory of each service and run:
    ```bash
    # Using Maven
    mvn spring-boot:run

    # Or using Gradle
    ./gradlew bootRun
    ```
    Start the services in order: `user-service`, `account-service`, `transaction-service`, `logging-service`, and finally `bff-service`.

## API Documentation

The complete API specification is available in the `openapi.yaml` file in the root of this repository. This file details all available endpoints, request/response schemas, and security requirements. You can use tools like the [Swagger Editor](https://editor.swagger.io/) to view and interact with the documentation.
