# Docker Setup & Deployment

This guide explains how to build and run REMAIL using Docker.

## Prerequisites

- Docker 20.10+
- Docker Compose 1.29+
- (Optional) Docker Hub account for publishing images

## Quick Start with Docker Compose

The simplest way to run REMAIL locally with a PostgreSQL database:

```bash
# Start all services (app + database)
docker-compose up -d

# View logs
docker-compose logs -f remail-app

# Stop services
docker-compose down

# Clean up volumes (remove data)
docker-compose down -v
```

Once running, access:
- **Application**: http://localhost:8080
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **Health Check**: http://localhost:8080/actuator/health

## Building Docker Image Manually

### Build the image:
```bash
docker build -t remail:latest .
```

### Run with H2 (in-memory database - for testing):
```bash
docker run -p 8080:8080 \
  --name remail-app \
  remail:latest
```

### Run with PostgreSQL:
```bash
# Start PostgreSQL
docker run -d \
  --name remail-postgres \
  -e POSTGRES_DB=remail \
  -e POSTGRES_USER=remail_user \
  -e POSTGRES_PASSWORD=remail_password \
  -p 5432:5432 \
  postgres:16-alpine

# Run REMAIL connected to PostgreSQL
docker run -d \
  --name remail-app \
  -p 8080:8080 \
  -e REMAIL_DATASOURCE_URL=jdbc:postgresql://remail-postgres:5432/remail \
  -e REMAIL_DATASOURCE_USERNAME=remail_user \
  -e REMAIL_DATASOURCE_PASSWORD=remail_password \
  --link remail-postgres \
  remail:latest
```

## Environment Variables

### Database Configuration
```bash
REMAIL_DATASOURCE_URL=jdbc:postgresql://remail-db:5432/remail
REMAIL_DATASOURCE_USERNAME=remail_user
REMAIL_DATASOURCE_PASSWORD=remail_password
REMAIL_DATASOURCE_DRIVER=org.postgresql.Driver
```

### Google Calendar Integration
```bash
REMAIL_CALENDAR_ENABLED=true
REMAIL_CALENDAR_ACCESS_TOKEN=your_token
REMAIL_CALENDAR_REFRESH_TOKEN=your_token
REMAIL_CALENDAR_CLIENT_ID=your_id
REMAIL_CALENDAR_CLIENT_SECRET=your_secret
REMAIL_CALENDAR_CALENDAR_ID=primary
```

### Gmail Integration
```bash
REMAIL_GMAIL_ENABLED=true
REMAIL_GMAIL_ACCESS_TOKEN=your_token
REMAIL_GMAIL_REFRESH_TOKEN=your_token
REMAIL_GMAIL_CLIENT_ID=your_id
REMAIL_GMAIL_CLIENT_SECRET=your_secret
```

### JVM Settings
```bash
JAVA_OPTS=-Xmx512m -Xms256m
```

## Docker Compose Services

### remail-db
- **Image**: postgres:16-alpine
- **Port**: 5432
- **Database**: remail
- **User**: remail_user
- **Volume**: remail_db_data (persistent storage)
- **Health Check**: Enabled

### remail-app
- **Image**: Built from Dockerfile
- **Port**: 8080
- **Depends On**: remail-db (health check)
- **Restart Policy**: unless-stopped
- **Health Check**: HTTP GET /actuator/health

## Publishing to Docker Hub

### Build and tag:
```bash
docker build -t username/remail:1.0.0 .
docker tag username/remail:1.0.0 username/remail:latest
```

### Login and push:
```bash
docker login
docker push username/remail:1.0.0
docker push username/remail:latest
```

## Cloud Deployment Examples

### Google Cloud Run
```bash
gcloud auth login
gcloud builds submit --tag gcr.io/PROJECT_ID/remail
gcloud run deploy remail \
  --image gcr.io/PROJECT_ID/remail \
  --platform managed \
  --region us-central1 \
  --set-env-vars REMAIL_DATASOURCE_URL=...
```

### AWS ECS
```bash
# Create ECR repository
aws ecr create-repository --repository-name remail

# Build and push
docker build -t remail:latest .
docker tag remail:latest ACCOUNT_ID.dkr.ecr.REGION.amazonaws.com/remail:latest
docker push ACCOUNT_ID.dkr.ecr.REGION.amazonaws.com/remail:latest

# Deploy via ECS Fargate (use AWS Console or AWS CLI)
```

### Heroku
```bash
heroku login
heroku container:login
heroku create remail-app
heroku container:push web -a remail-app
heroku container:release web -a remail-app
```

## Troubleshooting

### Container won't start
```bash
# Check logs
docker logs remail-app

# Inspect network
docker network inspect remail_default
```

### Database connection fails
- Ensure PostgreSQL container is running: `docker ps`
- Verify network connectivity: `docker network ls`
- Check environment variables are correctly set

### Health check failing
```bash
# Test health endpoint manually
curl http://localhost:8080/actuator/health

# Check container logs
docker logs remail-app
```

### Rebuild and restart
```bash
docker-compose down
docker-compose build --no-cache
docker-compose up -d
```

## Performance Tuning

### Increase memory for large workloads:
```yaml
# In docker-compose.yml
services:
  remail-app:
    environment:
      JAVA_OPTS: "-Xmx2g -Xms1g"
```

### Scale the application:
```bash
# Not recommended for stateful apps
docker-compose up -d --scale remail-app=3
```

## Security Best Practices

1. **Use secrets for credentials:**
   ```bash
   docker run -e REMAIL_CALENDAR_ACCESS_TOKEN="$(cat token.txt)" remail:latest
   ```

2. **Limit resources:**
   ```yaml
   services:
     remail-app:
       deploy:
         resources:
           limits:
             cpus: '0.5'
             memory: 512M
   ```

3. **Run as non-root:**
   - Dockerfile already uses JRE, add USER directive for extra security

4. **Use environment-specific images:**
   - Build separate images for dev, staging, production

## Monitoring

### View container stats:
```bash
docker stats remail-app
```

### Access health endpoint:
```bash
curl http://localhost:8080/actuator/health
```

### View detailed info:
```bash
curl http://localhost:8080/actuator/info
```

## References

- [Docker Documentation](https://docs.docker.com/)
- [Docker Compose Documentation](https://docs.docker.com/compose/)
- [Spring Boot Docker Documentation](https://spring.io/guides/docker/)
