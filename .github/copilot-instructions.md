# GitHub Copilot Instructions for the Agent Registration Microservice

## About This Project
This repository contains the agent-registration microservice for the HMRC Agents Platform. It provides APIs for managing agent application registrations.

## How to Understand API Endpoints
To understand the behavior of a specific API endpoint, do not read the source code directly. Instead, refer to the structured documentation located in `service-dependency-map/`.

The documentation for this microservice is organized as follows:

`service-dependency-map/agent-registration/`

Inside the microservice directory, you will find:
1. **JSON Definitions (`*.json`):** These are the primary source of truth. They contain the structured, machine-readable definition of each controller's endpoints and their interaction sequences.
2. **Markdown Documentation (`*.md`):** Human-readable documentation with embedded sequence diagrams.
3. **Mermaid Diagrams (`diagrams/*.mmd`):** The raw sequence diagrams for each endpoint.

**Your Workflow:**
1. When asked about an API, first locate the relevant controller JSON file in `service-dependency-map/agent-registration/`.
2. Find the JSON file corresponding to the controller in question (e.g., `ApplicationController.json`).
3. Use the information in the JSON file as the primary source to answer questions about the API's dependencies, logic, and sequence of operations.

## Available Controllers
- **ApplicationController:** Manages agent application CRUD operations
  - `GET /application` - Retrieves agent application by internal user ID
  - `POST /application` - Creates or updates agent application

## Database Collections
- **agent-application:** Stores agent application data with TTL-based expiration