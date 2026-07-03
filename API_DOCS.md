# REMAIL - API Documentation

REMAIL is a Gmail-to-Calendar task extraction service that automatically identifies registration deadlines from emails and syncs them to Google Calendar.

## Quick Start

### Prerequisites
- Java 21+
- Maven 3.8+
- Google OAuth2 credentials (optional, for calendar sync)

### Running the Application

```bash
# Build the project
mvn clean package

# Run the application
java -jar target/remail-0.0.1-SNAPSHOT.jar

# Or run with Spring Boot Maven plugin
mvn spring-boot:run
```

The application runs on `http://localhost:8080` by default.

## API Documentation

### Interactive API Docs (Swagger UI)

Once the application is running, access the interactive API documentation at:

```
http://localhost:8080/swagger-ui.html
```

You can test all endpoints directly from the Swagger UI interface.

### OpenAPI Schema

The OpenAPI 3.0 schema is available at:

```
http://localhost:8080/v3/api-docs
```

## API Endpoints

### Tasks API

#### Get Active Task
```http
GET /api/tasks/active
```
Retrieves the currently active task (next in queue).

**Response:**
- `200 OK` - Active task found
- `204 No Content` - No active task available

#### Apply Action to Task
```http
POST /api/tasks/{id}/action?actionType={actionType}
```
Perform an action on a task (SNOOZE, DISMISS, or FILLED).

**Parameters:**
- `id` (path) - Task ID
- `actionType` (query) - Action type: `SNOOZE`, `DISMISS`, or `FILLED`

**Response:**
- `200 OK` - Action applied successfully
- `404 Not Found` - Task not found

### Mail Ingestion API

#### Ingest Email
```http
POST /api/mail/intake
Content-Type: application/json

{
  "messageId": "msg123@gmail.com",
  "subject": "Registration for Senior Developer Position - Deadline Dec 31",
  "from": "recruiter@company.com",
  "body": "Dear Candidate,\nPlease register for our job opening..."
}
```
Process and ingest an email to extract registration deadlines.

**Response:**
- `201 Created` - Email processed and task created
- `204 No Content` - Email processed but no urgent deadline detected

## Configuration

### Environment Variables

#### Database Configuration
```bash
REMAIL_DATASOURCE_URL=jdbc:postgresql://localhost:5432/remail
REMAIL_DATASOURCE_USERNAME=postgres
REMAIL_DATASOURCE_PASSWORD=password
REMAIL_DATASOURCE_DRIVER=org.postgresql.Driver
```

#### Google Calendar Integration
```bash
REMAIL_CALENDAR_ENABLED=true
REMAIL_CALENDAR_ACCESS_TOKEN=your_access_token
REMAIL_CALENDAR_REFRESH_TOKEN=your_refresh_token
REMAIL_CALENDAR_CLIENT_ID=your_client_id
REMAIL_CALENDAR_CLIENT_SECRET=your_client_secret
REMAIL_CALENDAR_CALENDAR_ID=primary  # or specific calendar ID
```

#### Gmail Integration
```bash
REMAIL_GMAIL_ENABLED=false  # Set to true to enable Gmail polling
REMAIL_GMAIL_ACCESS_TOKEN=your_access_token
REMAIL_GMAIL_REFRESH_TOKEN=your_refresh_token
REMAIL_GMAIL_CLIENT_ID=your_client_id
REMAIL_GMAIL_CLIENT_SECRET=your_client_secret
```

## Task Status

Tasks have three possible statuses:

| Status | Description |
|--------|-------------|
| `PENDING` | New task, awaiting action |
| `SNOOZED` | Task snoozed until a future time |
| `COMPLETED` | Task marked as completed (DISMISS or FILLED) |

## Task Actions

| Action | Effect |
|--------|--------|
| `SNOOZE` | Snooze task for 15 minutes |
| `DISMISS` | Mark task as completed/dismissed |
| `FILLED` | Mark task as completed (position filled) |

## Development

### Running Tests
```bash
mvn test
```

### Building Documentation
```bash
mvn clean package
```

The application includes:
- 6 unit tests covering mail processing, Gmail integration, and task management
- H2 in-memory database for local development
- PostgreSQL support for production

## Architecture

### Core Components

- **TaskController** - REST endpoints for task management
- **MailIngestionController** - REST endpoint for email ingestion
- **TaskService** - Business logic for task operations
- **MailProcessingService** - Email parsing and task extraction
- **GoogleCalendarSyncService** - Calendar synchronization
- **RegistrationMailParser** - Email content parsing

### Data Persistence

- **TaskEntity** - JPA entity for tasks
- **TaskRepository** - Spring Data JPA repository
- H2 for local development, PostgreSQL for production

## Features

✅ Gmail email ingestion
✅ Automatic deadline extraction
✅ Google Calendar sync
✅ Task state management (snooze, complete)
✅ Duplicate detection via message ID
✅ Interactive API documentation (Swagger)
✅ Comprehensive test coverage

## Future Enhancements

- [ ] JWT authentication
- [ ] Advanced task filtering and search
- [ ] Email scheduling
- [ ] Multiple calendar support
- [ ] Task templates
- [ ] Webhook notifications

## License

MIT License - See LICENSE file for details

## Support

For issues or questions, please open an issue on GitHub.
