# Agent Registration Service Documentation

This directory contains comprehensive API documentation for the agent-registration microservice.

## Documentation Structure

### Primary Sources of Truth

- **`ApplicationController.json`** - Machine-readable definition of all ApplicationController endpoints and their interaction sequences

### Human-Readable Documentation  

- **`ApplicationController.md`** - Detailed documentation with embedded sequence diagrams for all endpoints

### Visual Diagrams

- **`diagrams/ApplicationController.GET.application.mmd`** - Sequence diagram for GET /application endpoint
- **`diagrams/ApplicationController.POST.application.mmd`** - Sequence diagram for POST /application endpoint

## Controllers and Endpoints

### ApplicationController

Manages agent application CRUD operations:

- **GET /application** - Retrieves agent application by authenticated user's internal user ID
- **POST /application** - Creates or updates agent application for authenticated user

## Database Collections

- **agent-application** - Stores agent application data with TTL-based expiration

## Usage Guidelines

When working with this API or answering questions about its behavior:

1. Refer to the JSON files first as the primary source of truth
2. Use the Markdown files for human-readable explanations
3. Reference the Mermaid diagrams for visual understanding of interaction flows

## Authentication

All endpoints require Government Gateway authentication with Agent affinity group. The service validates:

- User has Agent affinity group
- User does not have HMRC-AS-AGENT enrolment assigned
- Internal user ID is extracted from enrolments for data association
