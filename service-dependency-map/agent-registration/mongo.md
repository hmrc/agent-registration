# Analysis of MongoDB Collections in agent-registration

This document provides a comprehensive overview of the MongoDB collections used by the agent-registration microservice.

## 1. agent-application

- **Repository File:** `app/uk/gov/hmrc/agentregistration/repository/AgentApplicationRepo.scala`

- **Purpose:** This is the primary collection for storing agent registration applications. It manages the complete lifecycle of an agent's application to register with HMRC, from initial creation through submission. The collection supports the agent registration workflow by storing all application data including business details, contact information, Anti-Money Laundering Supervision (AMLS) details, and application state.

- **Schema Highlights:**
  - `_id`: The internal user ID from Government Gateway, used as the primary key
  - `internalUserId`: The authenticated user's internal identifier from Government Gateway
  - `createdAt`: Timestamp when the application was initially created
  - `lastUpdated`: Automatically generated timestamp for the last modification (TTL index for expiration)
  - `applicationState`: Current state of the application (InProgress or Submitted)
  - `utr`: Optional Unique Taxpayer Reference number for the business
  - `businessDetails`: Optional business information (sole trader, limited company, or partnership details)
  - `applicantContactDetails`: Optional contact information for the person applying
  - `amlsDetails`: Optional Anti-Money Laundering Supervision details including supervisory body and registration

- **Sample Document:**

```json
{
  "_id": "123456789012345678",
  "internalUserId": "123456789012345678",
  "createdAt": { "$date": "2025-10-15T09:00:00.000Z" },
  "lastUpdated": { "$date": "2025-10-16T10:30:00.000Z" },
  "applicationState": "InProgress",
  "utr": "1234567890",
  "businessDetails": {
    "safeId": "XE0001234567890",
    "businessType": "LimitedCompany",
    "companyProfile": {
      "companyName": "Example Agent Services Ltd",
      "companyNumber": "12345678",
      "dateOfIncorporation": { "$date": "2020-01-15T00:00:00.000Z" },
      "companyStatus": "Active"
    }
  },
  "applicantContactDetails": {
    "applicantName": {
      "firstName": "John",
      "lastName": "Smith"
    }
  },
  "amlsDetails": {
    "supervisoryBody": "FCA",
    "amlsRegistrationNumber": "12345678",
    "amlsExpiryDate": { "$date": "2026-12-31T00:00:00.000Z" },
    "amlsEvidence": {
      "reference": "evidence-ref-123",
      "uploadStatus": "UploadedSuccessfully"
    }
  }
}
```
