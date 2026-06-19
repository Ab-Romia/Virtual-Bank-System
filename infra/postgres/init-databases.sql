-- Creates one database per service on the shared Postgres server.
-- Database-per-service keeps the services loosely coupled: no service can read
-- or write another's tables, which is the property that makes the architecture
-- a real microservices topology rather than a shared-database monolith.

CREATE DATABASE vbank_user;
CREATE DATABASE vbank_account;
CREATE DATABASE vbank_transaction;
CREATE DATABASE vbank_audit;
CREATE DATABASE vbank_ai;

-- The AI assistant stores embeddings with pgvector; enable the extension in its
-- database. The image (pgvector/pgvector) ships the extension.
\connect vbank_ai
CREATE EXTENSION IF NOT EXISTS vector;
