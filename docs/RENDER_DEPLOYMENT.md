# Render Deployment Guide

## Prerequisites
- Render account (render.com)
- GitHub repository with this code pushed
- SMTP configuration (Gmail or other mail service)

## Deployment Steps

### 1. Create Web Service on Render
1. Go to Render Dashboard → Create New → Web Service
2. Connect your GitHub repository
3. Configure:
   - **Name**: ygc-chits-app
   - **Runtime**: Docker
   - **Region**: Singapore (or your preferred region)
   - **Plan**: Standard or higher
   - **Build Command**: (leave empty - uses Dockerfile)
   - **Start Command**: (leave empty - uses Dockerfile)

### 2. Set Environment Variables
Add these environment variables in the Render Web Service settings:

```
SPRING_PROFILES_ACTIVE=prod
JWT_SECRET=<Generate a strong secret>
JWT_EXPIRATION=86400000
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=<your-email@gmail.com>
MAIL_PASSWORD=<your-app-specific-password>
```

**For Gmail:**
- Use App Passwords (not your main password)
- Enable 2-Step Verification
- Generate app-specific password in Google Account settings

### 3. Deploy
1. Push your code to GitHub
2. Render will automatically detect and deploy using the Dockerfile
3. Monitor deployment in Render Dashboard

## Local Development

Use the default `application.properties` which uses H2 in-memory database:

```bash
mvn spring-boot:run
```

## Production Deployment

The `application-prod.properties` will be automatically used when:
- `SPRING_PROFILES_ACTIVE=prod` environment variable is set
- Uses H2 in-memory database (same as local development)

## Database

- **H2 Database**: Used in both local and production environments
- Schema is automatically created via Hibernate (ddl-auto=update)
- Data persists during application runtime
- Note: In-memory data will be lost when the application restarts on Render

### For Persistent Storage (Optional)
If you need data persistence across restarts, consider:
- AWS RDS (PostgreSQL)
- AWS DynamoDB
- Render's PostgreSQL service
- Other cloud database solutions

