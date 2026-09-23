# Techliex Management - Backend REST API Service

Production-ready backend API service for **Techliex Management** built with **Ktor 3.x**, **Kotlin**, **Exposed ORM**, **PostgreSQL**, **JWT Authentication**, **Clean Architecture**, and **Docker**.

---

## Table of Contents
1. [Architecture Overview](#architecture-overview)
2. [Local Development & Building](#local-development--building)
3. [Initial Admin Credentials](#initial-admin-credentials)
4. [Interactive Swagger Documentation](#interactive-swagger-documentation)
5. [Step-by-Step Linux VPS Deployment Guide](#step-by-step-linux-vps-deployment-guide)
   - [Step 1: Prepare VPS & Firewall](#step-1-prepare-vps--firewall)
   - [Step 2: Install Docker & Docker Compose](#step-2-install-docker--docker-compose)
   - [Step 3: Clone Repository & Configure Environment](#step-3-clone-repository--configure-environment)
   - [Step 4: Launch Application Containers](#step-4-launch-application-containers)
   - [Step 5: Configure Nginx Reverse Proxy](#step-5-configure-nginx-reverse-proxy)
   - [Step 6: Secure with SSL (Certbot / Let's Encrypt)](#step-6-secure-with-ssl-certbot--lets-encrypt)
6. [Database Backup & Automated Crontab Strategy](#database-backup--automated-crontab-strategy)
7. [Step-by-Step Zero-Data-Loss VPS Migration Guide](#step-by-step-zero-data-loss-vps-migration-guide)
8. [API Route Specifications](#api-route-specifications)

---

## Architecture Overview

The application strictly follows **Clean Architecture** principles:

```
src/main/kotlin/com/techliex/
├── domain/                  # Domain Layer (Entities, Repositories Interfaces, Use Cases)
│   ├── model/
│   ├── repository/
│   └── usecase/
├── data/                    # Data Layer (PostgreSQL Schema, Exposed ORM, DB Factory, Repositories Impl)
│   ├── db/
│   └── repository/
├── presentation/            # Presentation Layer (DTOs, Mappers, Ktor Route Controllers)
│   ├── dto/
│   └── routes/
├── security/                # JWT Token Generation & BCrypt Password Hashing
├── plugins/                 # Ktor Server Plugins (CORS, Serialization, StatusPages, Swagger)
└── Application.kt           # Main Entry Point
```

---

## Local Development & Building

### Prerequisites
* JDK 21+
* Docker & Docker Compose

### Run Server Locally via Gradle
```bash
# Build and run local server
./gradlew run
```

### Run with Docker Compose Locally
```bash
# Start PostgreSQL container and Ktor API server
docker-compose up --build
```

---

## Initial Admin Credentials

Upon initial database startup, if no users exist in the database, the system automatically seeds the default Administrator account:

* **Name**: `Umair`
* **Username**: `umair`
* **Password**: `umair123`
* **Role**: `Admin`

---

## Interactive Swagger Documentation

Interactive OpenAPI 3.0 documentation is served directly by the application at:

```
http://localhost:8080/swagger
```

You can test all endpoints, authenticate via JWT Bearer token, and inspect schemas directly from the Swagger UI.

---

## Step-by-Step Linux VPS Deployment Guide

This guide walks through deploying the Techliex Management backend to an **Ubuntu 22.04 / 24.04 LTS or Debian Linux VPS** (e.g., DigitalOcean, AWS EC2, Hetzner, Linode, Vultr).

### Step 1: Prepare VPS & Firewall

1. Connect to your VPS via SSH:
   ```bash
   ssh root@YOUR_VPS_IP
   ```

2. Update system packages:
   ```bash
   sudo apt update && sudo apt upgrade -y
   ```

3. Configure UFW Firewall:
   ```bash
   sudo ufw allow OpenSSH
   sudo ufw allow 80/tcp
   sudo ufw allow 443/tcp
   sudo ufw enable
   ```

---

### Step 2: Install Docker & Docker Compose

1. Install required packages and official Docker repository:
   ```bash
   sudo apt install -y ca-certificates curl gnupg lsb-release git nginx
   
   sudo mkdir -p /etc/apt/keyrings
   curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg

   echo \
     "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
     $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

   sudo apt update
   sudo apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
   ```

2. Verify Docker installation:
   ```bash
   docker --version
   docker compose version
   ```

---

### Step 3: Clone Repository & Configure Environment

1. Clone your project repository into `/var/www/techliex-management`:
   ```bash
   sudo mkdir -p /var/www
   cd /var/www
   git clone <YOUR_GIT_REPOSITORY_URL> techliex-management
   cd /var/www/techliex-management
   ```

2. Review `docker-compose.yml` environment settings:
   - Ensure database credentials (`DB_USER`, `DB_PASSWORD`, `POSTGRES_PASSWORD`) match.
   - Update `JWT_SECRET` to a strong random key.

---

### Step 4: Launch Application Containers

1. Build and run the services in background (detached mode):
   ```bash
   docker compose up -d --build
   ```

2. Check container status:
   ```bash
   docker compose ps
   ```

3. View live application logs:
   ```bash
   docker compose logs -f app
   ```

---

### Step 5: Configure Nginx Reverse Proxy

1. Create a new Nginx server block configuration for your domain:
   ```bash
   sudo nano /etc/nginx/sites-available/techliex
   ```

2. Paste the following configuration (replace `api.yourdomain.com` with your domain or IP):
   ```nginx
   server {
       listen 80;
       server_name api.yourdomain.com;

       # Max upload size for product and payment proof images
       client_max_body_size 50M;

       location / {
           proxy_pass http://127.0.0.1:8080;
           proxy_http_version 1.1;
           proxy_set_header Upgrade $http_upgrade;
           proxy_set_header Connection 'upgrade';
           proxy_set_header Host $host;
           proxy_set_header X-Real-IP $remote_addr;
           proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
           proxy_set_header X-Forwarded-Proto $scheme;
           proxy_cache_bypass $http_upgrade;
       }
   }
   ```

3. Enable configuration and test Nginx:
   ```bash
   sudo ln -s /etc/nginx/sites-available/techliex /etc/nginx/sites-enabled/
   sudo nginx -t
   sudo systemctl restart nginx
   ```

---

### Step 6: Secure with SSL (Certbot / Let's Encrypt)

1. Install Certbot for Nginx:
   ```bash
   sudo apt install -y certbot python3-certbot-nginx
   ```

2. Obtain SSL certificate and enable automatic HTTPS redirect:
   ```bash
   sudo certbot --nginx -d api.yourdomain.com
   ```

3. Verify SSL auto-renewal:
   ```bash
   sudo certbot renew --dry-run
   ```

Your backend API is now securely live at `https://api.yourdomain.com/` and Swagger UI is accessible at `https://api.yourdomain.com/swagger`.

---

## Database Backup & Automated Crontab Strategy

### Manual Backup (Instant Snapshot)
To take an immediate full database backup on your Linux server:

```bash
# Export compressed PostgreSQL SQL dump
docker exec -t techliex_postgres pg_dump -U techliex techliex_db | gzip > ~/techliex_backup_$(date +%F_%H%M%S).sql.gz
```

### Manual Database Restore
To restore database from a backup file:

```bash
# Restore compressed database dump
gunzip < ~/techliex_backup_2025-01-01.sql.gz | docker exec -i techliex_postgres psql -U techliex -d techliex_db
```

### Setting Up Automated Daily Backups via Cron
1. Create a backup script directory and backup script:
   ```bash
   mkdir -p ~/scripts /var/backups/techliex
   nano ~/scripts/backup_db.sh
   ```

2. Paste the script contents:
   ```bash
   #!/bin/bash
   BACKUP_DIR="/var/backups/techliex"
   DATE=$(date +%Y-%m-%d_%H%M%S)
   FILENAME="${BACKUP_DIR}/techliex_db_${DATE}.sql.gz"

   # Create database backup dump
   docker exec -t techliex_postgres pg_dump -U techliex techliex_db | gzip > "${FILENAME}"

   # Keep only backups from the last 14 days
   find ${BACKUP_DIR} -type f -name "*.sql.gz" -mtime +14 -delete
   ```

3. Grant execution permissions:
   ```bash
   chmod +x ~/scripts/backup_db.sh
   ```

4. Schedule daily execution at 2:00 AM via crontab:
   ```bash
   crontab -e
   ```
   Add the following line at the bottom:
   ```cron
   0 2 * * * /bin/bash /root/scripts/backup_db.sh > /dev/null 2>&1
   ```

---

## Step-by-Step Zero-Data-Loss VPS Migration Guide

Follow these exact steps when changing to a new VPS or cloud provider without losing any database records or uploaded image files.

### Phase 1: Prepare Backup on Old VPS

1. Stop application traffic on OLD VPS to prevent new writes during transfer:
   ```bash
   cd /var/www/techliex-management
   docker compose stop app
   ```

2. Generate final database dump and archive uploaded media files:
   ```bash
   # Create database snapshot
   docker exec -t techliex_postgres pg_dump -U techliex techliex_db | gzip > ~/migration_db.sql.gz

   # Archive uploaded image files
   docker run --rm --volumes-from techliex_postgres -v ~/:/backup alpine tar -czf /backup/migration_uploads.tar.gz -C /var/lib/docker/volumes/techliex-management_uploads_data/_data .
   ```

3. Copy backup files from OLD VPS directly to NEW VPS:
   ```bash
   scp ~/migration_db.sql.gz ~/migration_uploads.tar.gz root@NEW_VPS_IP:~/
   ```

---

### Phase 2: Setup New VPS

1. Log into NEW VPS:
   ```bash
   ssh root@NEW_VPS_IP
   ```

2. Repeat Steps 1 & 2 from Deployment Guide (Install Docker, Git, Nginx, UFW).

3. Clone repository on NEW VPS:
   ```bash
   sudo mkdir -p /var/www
   cd /var/www
   git clone <YOUR_GIT_REPOSITORY_URL> techliex-management
   cd /var/www/techliex-management
   ```

4. Start database service on NEW VPS:
   ```bash
   docker compose up -d db
   ```

---

### Phase 3: Restore Database & Uploads on New VPS

1. Restore database data onto NEW VPS:
   ```bash
   gunzip < ~/migration_db.sql.gz | docker exec -i techliex_postgres psql -U techliex -d techliex_db
   ```

2. Restore uploaded image files:
   ```bash
   # Start app container once to initialize docker volume
   docker compose up -d app
   docker compose stop app

   # Extract media files into new container volume
   docker run --rm --volumes-from techliex_postgres -v ~/:/backup alpine sh -c "cd /var/lib/docker/volumes/techliex-management_uploads_data/_data && tar -xzf /backup/migration_uploads.tar.gz"
   ```

3. Launch all services on NEW VPS:
   ```bash
   docker compose up -d --build
   ```

4. Verify containers are healthy:
   ```bash
   docker compose ps
   docker compose logs -f
   ```

---

### Phase 4: DNS Switch & SSL Setup

1. Update your domain DNS A Record to point to `NEW_VPS_IP`.
2. Configure Nginx and SSL Certbot on NEW VPS (Steps 5 & 6 from Deployment Guide).
3. Test your domain `https://api.yourdomain.com/swagger` and verify all product hunts, users, and orders are intact.
4. Once verified, turn off OLD VPS.

---

## API Route Specifications

| Method | Endpoint | Authorization | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/auth/login` | Public | Authenticate user & return JWT token |
| `GET` | `/api/v1/auth/me` | Bearer JWT | Retrieve current user profile |
| `POST` | `/api/v1/users` | Admin Only | Create new user account |
| `GET` | `/api/v1/users` | Bearer JWT | List all active users |
| `GET` | `/api/v1/users/{username}` | Bearer JWT | Get user details by username |
| `DELETE` | `/api/v1/users/{username}` | Admin Only | Deactivate user account |
| `POST` | `/api/v1/products` | Bearer JWT | Create new product hunt |
| `GET` | `/api/v1/products` | Bearer JWT | Get visible products (role scoped) |
| `GET` | `/api/v1/products/{id}` | Bearer JWT | Get product hunt details |
| `PATCH` | `/api/v1/products/{id}/warehouse` | Admin / Warehouse | Update warehouse price & note |
| `DELETE` | `/api/v1/products/{id}` | Admin Only | Delete product hunt |
| `POST` | `/api/v1/orders` | Bearer JWT | Place new order |
| `GET` | `/api/v1/orders` | Bearer JWT | Get orders (role scoped & status sorted) |
| `GET` | `/api/v1/orders/{id}` | Bearer JWT | Get order details |
| `PATCH` | `/api/v1/orders/{id}/logistics` | Admin Only | Set tracking info & status -> `Shipped` |
| `PATCH` | `/api/v1/orders/{id}/complete` | Bearer JWT | Set order status -> `Completed` |
| `GET` | `/api/v1/dashboard/stats` | Bearer JWT | Get dashboard metrics |
| `POST` | `/api/v1/media/upload` | Bearer JWT | Upload multipart image file |
