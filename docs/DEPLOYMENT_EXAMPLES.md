# Deployment Examples - Javalin API Mesh

## Overview

This document provides comprehensive deployment examples for different environments and scenarios using the Javalin API Mesh project's various JAR types.

## Quick Start Examples

### 1. Local Development
```bash
# Build development JARs (fastest)
./scripts/build-executable-jars.sh --dev

# Start all services
./scripts/start-all-services.sh

# Verify deployment
curl http://localhost:8080/api/management/readiness
curl http://localhost:8081/api/health
```

### 2. Production Single Server
```bash
# Build optimized JARs
./scripts/build-executable-jars.sh --optimized-jar --analyze

# Deploy to server
scp generic-api-service/target/*-optimized.jar server:/opt/javalin-api-mesh/
scp metrics-service/target/*-optimized.jar server:/opt/javalin-api-mesh/

# Start services on server
ssh server "cd /opt/javalin-api-mesh && nohup java -jar generic-api-service-1.0-SNAPSHOT-optimized.jar > api.log 2>&1 &"
ssh server "cd /opt/javalin-api-mesh && nohup java -jar metrics-service-1.0-SNAPSHOT-optimized.jar > metrics.log 2>&1 &"

# Health check
curl http://server:8080/api/management/liveness
curl http://server:8081/api/health
```

## Docker Deployment Examples

### 1. Single Service Container

#### Dockerfile for Generic API Service
```dockerfile
FROM openjdk:21-jre-slim

# Create app directory
WORKDIR /app

# Copy optimized JAR
COPY generic-api-service/target/generic-api-service-1.0-SNAPSHOT-optimized.jar app.jar

# Copy configuration files
COPY generic-config/ /app/config/

# Create data directory for H2 databases
RUN mkdir -p /app/data

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/api/management/liveness || exit 1

# Run application
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

#### Build and Run
```bash
# Build optimized JAR
./scripts/build-executable-jars.sh --optimized-jar

# Build Docker image
docker build -t javalin-api-mesh/generic-api:latest .

# Run container
docker run -d \
  --name generic-api-service \
  -p 8080:8080 \
  -v $(pwd)/generic-config:/app/config:ro \
  -v $(pwd)/data:/app/data \
  javalin-api-mesh/generic-api:latest

# Check health
docker exec generic-api-service curl -f http://localhost:8080/api/management/readiness
```

### 2. Multi-Service Docker Compose

#### docker-compose.yml
```yaml
version: '3.8'

services:
  generic-api:
    build:
      context: .
      dockerfile: Dockerfile.generic-api
    ports:
      - "8080:8080"
    volumes:
      - ./generic-config:/app/config:ro
      - ./data:/app/data
    environment:
      - JAVA_OPTS=-Xmx512m -Xms256m
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/api/management/liveness"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s
    restart: unless-stopped

  metrics-service:
    build:
      context: .
      dockerfile: Dockerfile.metrics
    ports:
      - "8081:8081"
    environment:
      - JAVA_OPTS=-Xmx256m -Xms128m
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8081/api/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s
    restart: unless-stopped
    depends_on:
      generic-api:
        condition: service_healthy

  nginx:
    image: nginx:alpine
    ports:
      - "80:80"
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf:ro
    depends_on:
      - generic-api
      - metrics-service
    restart: unless-stopped
```

#### nginx.conf
```nginx
events {
    worker_connections 1024;
}

http {
    upstream api_backend {
        server generic-api:8080;
    }
    
    upstream metrics_backend {
        server metrics-service:8081;
    }
    
    server {
        listen 80;
        
        location /api/ {
            proxy_pass http://api_backend;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
        }
        
        location /metrics/ {
            proxy_pass http://metrics_backend/;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
        }
        
        location /health {
            access_log off;
            return 200 "healthy\n";
            add_header Content-Type text/plain;
        }
    }
}
```

#### Deploy with Docker Compose
```bash
# Build JARs
./scripts/build-executable-jars.sh --optimized-jar

# Start all services
docker-compose up -d

# Check status
docker-compose ps
docker-compose logs -f

# Health checks
curl http://localhost/api/management/health
curl http://localhost/metrics/api/health
```

## Kubernetes Deployment Examples

### 1. Basic Kubernetes Deployment

#### generic-api-deployment.yaml
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: generic-api-service
  labels:
    app: generic-api-service
spec:
  replicas: 2
  selector:
    matchLabels:
      app: generic-api-service
  template:
    metadata:
      labels:
        app: generic-api-service
    spec:
      containers:
      - name: generic-api
        image: javalin-api-mesh/generic-api:latest
        ports:
        - containerPort: 8080
        env:
        - name: JAVA_OPTS
          value: "-Xmx512m -Xms256m"
        resources:
          requests:
            memory: "256Mi"
            cpu: "250m"
          limits:
            memory: "512Mi"
            cpu: "500m"
        livenessProbe:
          httpGet:
            path: /api/management/liveness
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 30
          timeoutSeconds: 10
          failureThreshold: 3
        readinessProbe:
          httpGet:
            path: /api/management/readiness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
          timeoutSeconds: 5
          failureThreshold: 3
        volumeMounts:
        - name: config-volume
          mountPath: /app/config
          readOnly: true
        - name: data-volume
          mountPath: /app/data
      volumes:
      - name: config-volume
        configMap:
          name: javalin-config
      - name: data-volume
        persistentVolumeClaim:
          claimName: javalin-data-pvc

---
apiVersion: v1
kind: Service
metadata:
  name: generic-api-service
spec:
  selector:
    app: generic-api-service
  ports:
  - protocol: TCP
    port: 8080
    targetPort: 8080
  type: ClusterIP

---
apiVersion: v1
kind: ConfigMap
metadata:
  name: javalin-config
data:
  application.yaml: |
    server:
      host: "0.0.0.0"
      port: 8080
    configuration:
      source: "yaml"
      directories:
        - "/app/config"

---
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: javalin-data-pvc
spec:
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 1Gi
```

#### Deploy to Kubernetes
```bash
# Build and push image
./scripts/build-executable-jars.sh --optimized-jar
docker build -t javalin-api-mesh/generic-api:latest .
docker tag javalin-api-mesh/generic-api:latest your-registry/javalin-api-mesh/generic-api:latest
docker push your-registry/javalin-api-mesh/generic-api:latest

# Deploy to Kubernetes
kubectl apply -f generic-api-deployment.yaml

# Check deployment
kubectl get pods -l app=generic-api-service
kubectl logs -l app=generic-api-service

# Test health
kubectl port-forward svc/generic-api-service 8080:8080
curl http://localhost:8080/api/management/readiness
```

### 2. Helm Chart Deployment

#### values.yaml
```yaml
replicaCount: 2

image:
  repository: javalin-api-mesh/generic-api
  tag: latest
  pullPolicy: IfNotPresent

service:
  type: ClusterIP
  port: 8080

ingress:
  enabled: true
  annotations:
    kubernetes.io/ingress.class: nginx
    cert-manager.io/cluster-issuer: letsencrypt-prod
  hosts:
    - host: api.yourdomain.com
      paths:
        - path: /
          pathType: Prefix
  tls:
    - secretName: api-tls
      hosts:
        - api.yourdomain.com

resources:
  limits:
    cpu: 500m
    memory: 512Mi
  requests:
    cpu: 250m
    memory: 256Mi

autoscaling:
  enabled: true
  minReplicas: 2
  maxReplicas: 10
  targetCPUUtilizationPercentage: 80

persistence:
  enabled: true
  size: 1Gi
  storageClass: fast-ssd

config:
  server:
    host: "0.0.0.0"
    port: 8080
  configuration:
    source: "yaml"
    directories:
      - "/app/config"
```

#### Deploy with Helm
```bash
# Install Helm chart
helm install javalin-api-mesh ./helm-chart -f values.yaml

# Upgrade deployment
helm upgrade javalin-api-mesh ./helm-chart -f values.yaml

# Check status
helm status javalin-api-mesh
kubectl get pods -l app.kubernetes.io/name=javalin-api-mesh
```

## Cloud Platform Examples

### 1. AWS ECS Deployment

#### task-definition.json
```json
{
  "family": "javalin-api-mesh",
  "networkMode": "awsvpc",
  "requiresCompatibilities": ["FARGATE"],
  "cpu": "512",
  "memory": "1024",
  "executionRoleArn": "arn:aws:iam::account:role/ecsTaskExecutionRole",
  "containerDefinitions": [
    {
      "name": "generic-api",
      "image": "your-account.dkr.ecr.region.amazonaws.com/javalin-api-mesh:latest",
      "portMappings": [
        {
          "containerPort": 8080,
          "protocol": "tcp"
        }
      ],
      "environment": [
        {
          "name": "JAVA_OPTS",
          "value": "-Xmx512m -Xms256m"
        }
      ],
      "healthCheck": {
        "command": [
          "CMD-SHELL",
          "curl -f http://localhost:8080/api/management/liveness || exit 1"
        ],
        "interval": 30,
        "timeout": 5,
        "retries": 3,
        "startPeriod": 60
      },
      "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
          "awslogs-group": "/ecs/javalin-api-mesh",
          "awslogs-region": "us-west-2",
          "awslogs-stream-prefix": "ecs"
        }
      }
    }
  ]
}
```

### 2. Google Cloud Run Deployment

```bash
# Build and push to Google Container Registry
./scripts/build-executable-jars.sh --optimized-jar
docker build -t gcr.io/your-project/javalin-api-mesh:latest .
docker push gcr.io/your-project/javalin-api-mesh:latest

# Deploy to Cloud Run
gcloud run deploy javalin-api-mesh \
  --image gcr.io/your-project/javalin-api-mesh:latest \
  --platform managed \
  --region us-central1 \
  --allow-unauthenticated \
  --memory 512Mi \
  --cpu 1 \
  --max-instances 10 \
  --port 8080

# Test deployment
curl https://javalin-api-mesh-hash-uc.a.run.app/api/management/health
```

## Monitoring and Observability

### Health Check Integration
```bash
# Kubernetes liveness probe
curl http://service:8080/api/management/liveness

# Kubernetes readiness probe  
curl http://service:8080/api/management/readiness

# Deployment verification
curl http://service:8080/api/management/deployment

# JAR information
curl http://service:8080/api/management/jar
```

### Prometheus Metrics (if enabled)
```bash
# Metrics endpoint
curl http://service:8081/metrics

# Custom dashboard
curl http://service:8081/dashboard
```

## Troubleshooting Deployments

### Common Issues and Solutions

#### 1. Container Won't Start
```bash
# Check logs
docker logs container-name
kubectl logs pod-name

# Check health endpoints
curl http://localhost:8080/api/management/deployment
```

#### 2. Health Checks Failing
```bash
# Test readiness
curl -v http://localhost:8080/api/management/readiness

# Test liveness
curl -v http://localhost:8080/api/management/liveness

# Check specific database health
curl http://localhost:8080/api/management/health/databases/stocktrades
```

#### 3. Performance Issues
```bash
# Check memory usage
curl http://localhost:8080/api/management/health | jq '.service.memoryUsage'

# Check JAR type
curl http://localhost:8080/api/management/jar | jq '.jarType'

# Analyze JAR contents
./scripts/analyze-jars.sh --module generic-api-service --profile optimized
```

## Best Practices

1. **Use optimized JARs for production** - Smaller size, better performance
2. **Implement proper health checks** - Use liveness and readiness probes
3. **Monitor resource usage** - Set appropriate memory and CPU limits
4. **Use persistent volumes** - For H2 database files in containers
5. **Implement graceful shutdown** - Handle SIGTERM signals properly
6. **Use configuration management** - External config files or ConfigMaps
7. **Enable logging** - Structured logging for better observability
8. **Test deployments** - Verify health endpoints after deployment

## Next Steps

- See [JAR_USAGE_GUIDE.md](JAR_USAGE_GUIDE.md) for detailed JAR information
- See [HEALTH_MONITORING.md](HEALTH_MONITORING.md) for health check configuration
- See [DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md) for general deployment information
