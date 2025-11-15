#!/bin/bash

echo "======================================"
echo "Virtual Bank System - Starting Infrastructure"
echo "======================================"
echo ""

if ! docker info > /dev/null 2>&1; then
    echo "Error: Docker is not running. Please start Docker and try again."
    exit 1
fi

echo "Starting infrastructure services..."
echo "- PostgreSQL databases"
echo "- Zookeeper"
echo "- Kafka"
echo ""

docker-compose up -d

echo ""
echo "Waiting for services to start..."
sleep 10

echo ""
docker-compose ps

echo ""
echo "======================================"
echo "Infrastructure is ready!"
echo "======================================"
echo ""
echo "You can now start the backend services and frontend."
echo ""
