#!/bin/bash

# Change to project root
cd /home/romia/IdeaProjects/Virtual-Bank-System

# Load environment variables from .env file
if [ -f .env ]; then
    export $(cat .env | grep -v '^#' | xargs)
    echo "Loaded environment variables from .env"
    echo "OPENAI_API_KEY is set: ${OPENAI_API_KEY:0:10}..."
else
    echo "Warning: .env file not found"
fi

# Run the AI agent service
cd ai-agent-service
mvn clean spring-boot:run
