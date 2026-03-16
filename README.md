#  BTG Pactual  Investment Fund Orchestrator

<p align="center">
  <img src="https://img.shields.io/badge/Java-17-007396?style=for-the-badge&logo=openjdk&logoColor=white"/>
  <img src="https://img.shields.io/badge/Spring_Boot-3.5-6DB33F?style=for-the-badge&logo=springboot&logoColor=white"/>
  <img src="https://img.shields.io/badge/DynamoDB-Single_Table-4053D6?style=for-the-badge&logo=amazondynamodb&logoColor=white"/>
  <img src="https://img.shields.io/badge/Docker-Multi--Stage-2496ED?style=for-the-badge&logo=docker&logoColor=white"/>
  <img src="https://img.shields.io/badge/AWS-App_Runner-FF9900?style=for-the-badge&logo=amazonaws&logoColor=white"/>
  <img src="https://img.shields.io/badge/Coverage-Unit_%26_Integration-4CAF50?style=for-the-badge"/>
</p>

> Self-service REST API for managing BTG Pactual investment fund subscriptions  no advisor contact required.

---

##  Table of Contents

- [Overview](#-overview)
- [Architecture](#-architecture)
- [Design Patterns](#-design-patterns)
- [NoSQL Data Model](#-nosql-data-model--single-table-design)
- [API Endpoints](#-api-endpoints)
- [Quick Start (Docker)](#-quick-start-docker)
- [Running Tests](#-running-tests)
- [Security Practices](#-security-practices)
- [AWS Deployment](#-aws-deployment-cloudformation)
- [BTG Pactual Business Rules](#-btg-pactual-business-rules)

---

## 🎯 Overview

| Feature | Detail |
|---|---|
| **Subscribe to fund** | `POST /api/v1/clients/{clientId}/funds` |
| **Cancel subscription** | `DELETE /api/v1/clients/{clientId}/funds/{fundId}` |
| **Transaction history** | `GET /api/v1/clients/{clientId}/transactions` |
| **Email / SMS notification** | Strategy pattern, async via Spring Events |
| **Idempotency** | `X-Idempotency-Key` header prevents duplicate charges |
| **Database** | Amazon DynamoDB  Single Table Design |
| **Deployment** | Docker Compose (local)  AWS App Runner (production) |

---

## 🏛 Architecture

```
┌─────────────────────────────────────────────────────────┐
│                  HTTP Client / Swagger UI                │
└───────────────────────────┬─────────────────────────────┘
                            │ REST
┌───────────────────────────▼─────────────────────────────┐
│               Web Layer  (Controller + DTOs)             │
│         GlobalExceptionHandler (@RestControllerAdvice)   │
└───────────────────────────┬─────────────────────────────┘
                            │ Use-Case interfaces (DIP)
┌───────────────────────────▼─────────────────────────────┐
│              Application Layer  (Use Cases)              │
│   SubscribeFundService · CancelSubscriptionService       │
│   GetTransactionHistoryService · ListFundsService        │
└──────────┬─────────────────────────────┬────────────────┘
           │ Port interfaces             │ Spring Events
┌──────────▼──────────┐      ┌───────────▼────────────────┐
│  Infrastructure –   │      │  Notification Layer         │
│  DynamoDB Adapters  │      │  Strategy (Email / SMS)     │
│  Single Table       │      │  Factory + Observer         │
└──────────┬──────────┘      └────────────────────────────┘
           │
┌──────────▼──────────┐
│   Amazon DynamoDB   │  (Local via docker-compose / AWS in production)
└─────────────────────┘

```

The project follows **Hexagonal Architecture** (Ports & Adapters):

- **Domain**  pure business entities and rules (zero framework dependencies).
- **Application**  use-case services that orchestrate the domain.
- **Infrastructure**  DynamoDB adapters, notification strategies.
- **Web**  Spring MVC controllers, DTOs, exception handler.

---

## 🧩 Design Patterns

| Pattern | Where | Why |
|---|---|---|
| **Strategy** | `NotificationStrategy` + `Email/SmsNotificationStrategy` | Swap notification channel at runtime without `if/else` |
| **Factory** | `NotificationStrategyFactory`  `TransactionFactory` | Centralise object creation; Open/Closed for new channels |
| **Observer** | `FundTransactionEvent` + `FundTransactionEventListener` | Decouple subscription logic from notification side-effect |
| **Repository** | `ClientRepository`, `FundRepository`, etc. | Abstract persistence behind interfaces (DIP) |
| **Use-Case** | `SubscribeFundUseCase`, `CancelSubscriptionUseCase`, etc. | Single Responsibility  one class, one business flow |

---

## 🗄 NoSQL Data Model — Single Table Design

All entities live in **one DynamoDB table** (`FondosBTG`) with a composite key `PK + SK`.  
A single `Query` by `PK = CLIENT#<id>` returns the client's subscriptions and full history.

| Entity | PK | SK | Key Attributes |
|---|---|---|---|
| **Client** | `CLIENT#001` | `METADATA` | name, email, phone, balance, notificationPreference |
| **Subscription** | `CLIENT#001` | `FUND#1` | fundId, fundName, amount, subscribedAt |
| **Transaction** | `CLIENT#001` | `TX#2025-06-20T10:00:00Z#<uuid>` | txId, type (APERTURA/CANCELACION), amount |
| **Fund** | `FUND#1` | `METADATA` | name, minAmount, category (FPV/FIC) |
| **Idempotency** | `IDEMPOTENCY#<key>` | `METADATA` | txId |

### Why Single Table?

- **One network round-trip** to get all client data.
- **No JOINs**, no N+1 problems.
- **Pay-per-request** billing  cost scales to zero when idle.

---

## 📡 API Endpoints

| Method | Path | Description | Status |
|---|---|---|---|
| `GET` | `/api/v1/clients/{id}` | Get client + balance | 200 |
| `GET` | `/api/v1/clients/{id}/funds` | List all funds | 200 |
| `POST` | `/api/v1/clients/{id}/funds` | Subscribe (apertura) | **201** |
| `DELETE` | `/api/v1/clients/{id}/funds/{fundId}` | Cancel subscription | 200 |
| `GET` | `/api/v1/clients/{id}/transactions` | Transaction history | 200 |
| `GET` | `/actuator/health` | Health check (AWS probe) | 200 |
| `GET` | `/swagger-ui.html` | Interactive API docs |  |

### Idempotency Header

```http
POST /api/v1/clients/CLIENT-001/funds
X-Idempotency-Key: 550e8400-e29b-41d4-a716-446655440000
Content-Type: application/json

{ "fundId": "1" }
```

Send the same `X-Idempotency-Key` on retries  the **original transaction** is returned, the client is **never charged twice**.

---

## 🚀 Quick Start (Docker)

### Prerequisites

- Docker  24 and Docker Compose v2
- Ports **8080** and **8000** available

### 1. Clone & configure

```bash
git clone https://github.com/adrianland/java-serverless-funds-orchestrator.git
cd java-serverless-funds-orchestrator

# .env is pre-configured for local development  no changes needed
cat .env
```

### 2. Build & run

```bash
docker compose up --build
```

That's it. Docker will:

1. **Stage 1**  compile the Maven project inside a JDK container.
2. **Stage 2**  copy the JAR into a lightweight Alpine JRE image (~90 MB).
3. Start **DynamoDB Local** with a persistent volume.
4. Start the **Spring Boot app** (waits for DynamoDB to be healthy).
5. **Auto-create** the `FondosBTG` table and **seed** the 5 BTG funds + 1 demo client.

### 3. Explore

| Resource | URL |
|---|---|
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Health check | http://localhost:8080/actuator/health |
| API docs (JSON) | http://localhost:8080/api-docs |
| DynamoDB Local | http://localhost:8000 |

### 4. Quick smoke test

```bash
# Get demo client (starts with COP $500 000)
curl http://localhost:8080/api/v1/clients/CLIENT-001

# Subscribe to fund 1 (FPV_BTG_PACTUAL_RECAUDADORA  COP $75 000)
curl -X POST http://localhost:8080/api/v1/clients/CLIENT-001/funds \
  -H "Content-Type: application/json" \
  -H "X-Idempotency-Key: $(uuidgen)" \
  -d '{"fundId":"1"}'

# View transaction history
curl http://localhost:8080/api/v1/clients/CLIENT-001/transactions

# Cancel subscription
curl -X DELETE http://localhost:8080/api/v1/clients/CLIENT-001/funds/1
```

### 5. Stop & clean up

```bash
docker compose down          # stop containers, keep volumes
docker compose down -v       # stop containers AND delete DynamoDB data
```

---

##  Quick Start (Local  No Docker)

Run the application directly from your IDE or terminal, using only DynamoDB Local in Docker.

### Prerequisites

- Java 17+
- Maven 3.9+
- Docker (only for DynamoDB Local)

### 1. Start only the database

```bash
# Spin up DynamoDB Local only  no need to build the app image
docker compose up dynamodb-local
```

DynamoDB Local will be available at `http://localhost:8000`.

### 2. Run from IntelliJ IDEA

1. Open **Run  Edit Configurations**
2. Select your Spring Boot run configuration
3. Add the following under **Environment variables**:

```
SPRING_PROFILES_ACTIVE=local
```

4. Click **Run** 

### 3. Run from terminal

```bash
SPRING_PROFILES_ACTIVE=local mvn spring-boot:run
```

### 4. What the `local` profile does

With `SPRING_PROFILES_ACTIVE=local` Spring Boot loads `application-local.yml`,
which points DynamoDB to `http://localhost:8000` and uses fake credentials 
no real AWS account required.

```yaml
# application-local.yml
aws:
  dynamodb:
    endpoint: http://localhost:8000
    region: us-east-1
    access-key: local
    secret-key: local
    table-name: FondosBTG
```

On startup the app **auto-creates** the `FondosBTG` table and seeds the 5 BTG funds
and the demo client  same behaviour as the full Docker setup.

### 5. Verify it is running

```bash
curl http://localhost:8080/actuator/health
#  {"status":"UP", ...}

curl http://localhost:8080/api/v1/clients/CLIENT-001
#  {"clientId":"CLIENT-001","balance":500000, ...}
```

Swagger UI is also available at: http://localhost:8080/swagger-ui.html

---

## 🧪 Running Tests

```bash
# Unit tests only (no Docker required)
mvn test

# Integration tests (requires Docker  Testcontainers pulls LocalStack automatically)
mvn verify

# Both
mvn verify -Dsurefire.failIfNoSpecifiedTests=false
```

### Test Coverage

| Layer | Tests | Type |
|---|---|---|
| `SubscribeFundService` | 7 scenarios (happy path, all business rules, edge cases) | Unit |
| `CancelSubscriptionService` | 3 scenarios | Unit |
| `TransactionFactory` | 3 scenarios (immutability, uniqueness) | Unit |
| `NotificationStrategyFactory` | 4 scenarios | Unit |
| `Client` domain model | 4 scenarios (balance immutability) | Unit |
| `FundApiIntegrationTest` | 8 end-to-end scenarios against real DynamoDB | Integration |

---

## 🔐 Security Practices

| Practice | Implementation |
|---|---|
| **Expression Attribute Values** | All DynamoDB SDK calls use `:param` bindings  prevents NoSQL injection |
| **Non-root Docker user** | `adduser appuser` in Dockerfile Stage 2 |
| **Minimal attack surface** | Alpine JRE  no JDK, no Maven, no shell tools in the runtime image |
| **Container memory limits** | `-XX:MaxRAMPercentage=75.0` respects cgroup limits |
| **HTTP client timeouts** | `connectionTimeout=5s`, `socketTimeout=30s`, `maxConnections=50` |
| **IAM Least Privilege** | CloudFormation role grants only `dynamodb:GetItem|PutItem|...` on the specific table ARN |
| **Encryption at rest** | DynamoDB `SSEEnabled: true` in CloudFormation |
| **Point-in-time recovery** | `PointInTimeRecoveryEnabled: true`  35-day recovery window |
| **Structured JSON logs** | Logstash encoder  no sensitive data in log messages |
| **Graceful shutdown** | `server.shutdown: graceful`  drains in-flight requests |

---

## ☁️ AWS Deployment (CloudFormation)

### What the template provisions

```
VPC + Public Subnet + Internet Gateway
Security Group       (inbound: port 8080 only)

DynamoDB             FondosBTG table (PAY_PER_REQUEST, SSE, PITR)

IAM                  Ec2InstanceRole  (least-privilege DynamoDB + ECR access)
                     Ec2InstanceProfile

EC2 t3.micro         Runs Docker + Spring Boot container (FREE TIER eligible)
                     Auto-pulls image from ECR on startup
```

> **Free Tier:** EC2 t3.micro (750 hours/month), DynamoDB (25 GB + 200M requests/month),
> ECR (500 MB/month) and VPC/IAM are always free.

---

### Prerequisites

- AWS CLI v2 installed and configured (`aws configure`)
- Docker installed and running
- An active AWS account

---

### Step 1 — Configure AWS credentials

```bash
aws configure
```

Enter your Access Key ID, Secret Access Key, region (`us-east-1`) and output format (`json`).

Verify it works:
```bash
aws sts get-caller-identity
```

---

### Step 2 — Get your AWS Account ID

You will need it in every command below. Save it in a variable:

```bash
# Run this and copy the value
aws sts get-caller-identity --query "Account" --output text
```

From here on replace `<ACCOUNT_ID>` with your real account ID (e.g. `123456789012`).

---

### Step 3 — Create the ECR repository

```bash
aws ecr create-repository \
  --repository-name btg-funds-orchestrator \
  --region us-east-1
```

---

### Step 4 — Build and push the Docker image

```bash
# Authenticate with ECR (token expires every 12 hours, re-run if push fails)
aws ecr get-login-password --region us-east-1 | \
  docker login --username AWS --password-stdin \
  <ACCOUNT_ID>.dkr.ecr.us-east-1.amazonaws.com

# Build the image (from the project root)
docker build -t btg-funds-orchestrator .

# Tag
docker tag btg-funds-orchestrator:latest \
  <ACCOUNT_ID>.dkr.ecr.us-east-1.amazonaws.com/btg-funds-orchestrator:latest

# Push
docker push \
  <ACCOUNT_ID>.dkr.ecr.us-east-1.amazonaws.com/btg-funds-orchestrator:latest
```

Verify the image was uploaded:
```bash
aws ecr describe-images \
  --repository-name btg-funds-orchestrator \
  --region us-east-1 \
  --query "imageDetails[*].{Tag:imageTags[0],Date:imagePushedAt}" \
  --output table
```

---

### Step 5 — Get the latest Amazon Linux 2023 AMI ID for your region

The AMI ID changes per region and over time, so always fetch the latest one:

```bash
aws ec2 describe-images \
  --owners amazon \
  --filters "Name=name,Values=al2023-ami-*-x86_64" \
            "Name=state,Values=available" \
  --query "sort_by(Images, &CreationDate)[-1].ImageId" \
  --output text \
  --region us-east-1
```

Copy the result (e.g. `ami-0abcdef1234567890`). You will need it in the next step.

---

### Step 6 — Deploy the CloudFormation stack

```bash
aws cloudformation create-stack \
  --stack-name btg-funds-orchestrator-production \
  --template-body file://infrastructure/cloudformation/template.yaml \
  --capabilities CAPABILITY_NAMED_IAM \
  --region us-east-1 \
  --parameters \
      ParameterKey=Environment,ParameterValue=production \
      ParameterKey=AmiId,ParameterValue=<YOUR_AMI_ID> \
      ParameterKey=ImageUri,ParameterValue=<ACCOUNT_ID>.dkr.ecr.us-east-1.amazonaws.com/btg-funds-orchestrator:latest
```

Wait for the stack to finish (5-10 minutes):
```bash
aws cloudformation wait stack-create-complete \
  --stack-name btg-funds-orchestrator-production \
  --region us-east-1
```

---

### Step 7 — Get the application URL

```bash
aws cloudformation describe-stacks \
  --stack-name btg-funds-orchestrator-production \
  --region us-east-1 \
  --query "Stacks[0].Outputs" \
  --output table
```

You will see:

```
AppUrl         -> http://<EC2_PUBLIC_IP>:8080
SwaggerUiUrl   -> http://<EC2_PUBLIC_IP>:8080/swagger-ui.html
HealthCheckUrl -> http://<EC2_PUBLIC_IP>:8080/actuator/health
```

Wait 3-5 minutes after `CREATE_COMPLETE` for the EC2 instance to install Docker,
pull the image and start Spring Boot.

---

### Step 8 — Verify everything works

```bash
BASE=http://<EC2_PUBLIC_IP>:8080

# Health check
curl $BASE/actuator/health

# Get demo client (COP $500.000 balance)
curl $BASE/api/v1/clients/CLIENT-001

# List all funds
curl $BASE/api/v1/clients/CLIENT-001/funds

# Subscribe to a fund
curl -X POST $BASE/api/v1/clients/CLIENT-001/funds \
  -H "Content-Type: application/json" \
  -H "X-Idempotency-Key: test-key-001" \
  -d '{"fundId":"1"}'

# View transaction history
curl $BASE/api/v1/clients/CLIENT-001/transactions

# Cancel subscription
curl -X DELETE $BASE/api/v1/clients/CLIENT-001/funds/1
```

Open Swagger UI in the browser:
```
http://<EC2_PUBLIC_IP>:8080/swagger-ui.html
```

---

### Updating the application (after code changes)

When you make changes to the code, you only need to rebuild and push the image.
No need to redeploy CloudFormation:

```bash
# 1. Rebuild and push from your local machine
aws ecr get-login-password --region us-east-1 | \
  docker login --username AWS --password-stdin \
  <ACCOUNT_ID>.dkr.ecr.us-east-1.amazonaws.com

docker build -t btg-funds-orchestrator .
docker tag btg-funds-orchestrator:latest \
  <ACCOUNT_ID>.dkr.ecr.us-east-1.amazonaws.com/btg-funds-orchestrator:latest
docker push \
  <ACCOUNT_ID>.dkr.ecr.us-east-1.amazonaws.com/btg-funds-orchestrator:latest

# 2. Connect to the EC2 via AWS Console -> EC2 -> Connect -> Session Manager
# Then run these commands inside the terminal:
sudo aws ecr get-login-password --region us-east-1 | \
  sudo docker login --username AWS --password-stdin \
  <ACCOUNT_ID>.dkr.ecr.us-east-1.amazonaws.com

sudo docker pull \
  <ACCOUNT_ID>.dkr.ecr.us-east-1.amazonaws.com/btg-funds-orchestrator:latest

sudo docker stop btg-funds-app && sudo docker rm btg-funds-app

sudo docker run -d \
  --name btg-funds-app \
  --restart unless-stopped \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=production \
  -e AWS_REGION=us-east-1 \
  -e AWS_DEFAULT_REGION=us-east-1 \
  -e DYNAMODB_TABLE_NAME=FondosBTG \
  -e DYNAMODB_ENDPOINT="" \
  <ACCOUNT_ID>.dkr.ecr.us-east-1.amazonaws.com/btg-funds-orchestrator:latest
```

---

### Viewing logs on the EC2 instance

Connect via **AWS Console -> EC2 -> your instance -> Connect -> Session Manager**, then:

```bash
# Check container is running
sudo docker ps

# View application logs
sudo docker logs btg-funds-app --tail 50

# Follow logs in real time
sudo docker logs btg-funds-app -f
```

---

### Tear down (avoid charges)

```bash
# Delete the CloudFormation stack (removes EC2, VPC, IAM, Security Group)
aws cloudformation delete-stack \
  --stack-name btg-funds-orchestrator-production \
  --region us-east-1

# DynamoDB has DeletionPolicy: Retain — delete it manually if needed
aws dynamodb delete-table --table-name FondosBTG --region us-east-1

# Delete the ECR repository and images
aws ecr delete-repository \
  --repository-name btg-funds-orchestrator \
  --force \
  --region us-east-1
```

---

## 📋 BTG Pactual Business Rules

| Rule | Implementation |
|---|---|
| Client starts with **COP $500 000** | Seeded in `DynamoDbTableInitializer` |
| Each transaction has a **unique ID** | `UUID.randomUUID()` in `TransactionFactory` |
| Each fund has a **minimum subscription amount** | Validated in `SubscribeFundService` before debit |
| Insufficient balance  `"No tiene saldo disponible para vincularse al fondo <Name>"` | `InsufficientBalanceException`  HTTP 400 |
| Cancel subscription  **amount returned** to client | `CancelSubscriptionService.execute()` credits balance atomically |
| **Email or SMS** notification on subscription | Strategy pattern selects channel based on `notificationPreference` |
| All amounts in **COP** | `BigDecimal` throughout  no floating-point errors |

### Fund Catalog

| ID | Name | Min. Amount | Category |
|---|---|---|---|
| 1 | FPV_BTG_PACTUAL_RECAUDADORA | COP $75,000 | FPV |
| 2 | FPV_BTG_PACTUAL_ECOPETROL | COP $125,000 | FPV |
| 3 | DEUDAPRIVADA | COP $50,000 | FIC |
| 4 | FDO-ACCIONES | COP $250,000 | FIC |
| 5 | FPV_BTG_PACTUAL_DINAMICA | COP $100,000 | FPV |

---

##  Project Structure

```
java-serverless-funds-orchestrator/
 src/
    main/java/com/adrianland/fundsorchestrator/
       config/               # DynamoDB, OpenAPI, Async config
       domain/
          model/            # Client, Fund, Subscription, Transaction (+ enums)
          event/            # FundTransactionEvent (Spring ApplicationEvent)
          exception/        # Domain exceptions
          port/             # Repository interfaces (output ports)
       application/
          usecase/          # Use-case interfaces (input ports)
          service/          # Use-case implementations + TransactionFactory
       infrastructure/
          dynamodb/         # DynamoDB adapters + table initializer
          notification/
              strategy/     # EmailNotificationStrategy, SmsNotificationStrategy
              factory/      # NotificationStrategyFactory
              observer/     # FundTransactionEventListener
       web/
           controller/       # FundController, HealthController
           dto/              # Request / Response records
           handler/          # GlobalExceptionHandler
    test/java/com/adrianland/fundsorchestrator/
        unit/
           service/          # SubscribeFundServiceTest, CancelSubscriptionServiceTest, ClientTest
           factory/          # TransactionFactoryTest
           notification/     # NotificationStrategyFactoryTest
        integration/          # FundApiIntegrationTest (Testcontainers + LocalStack)
 infrastructure/
    cloudformation/
        template.yaml         # VPC  DynamoDB  App Runner  IAM
 Dockerfile                    # Multi-stage: Maven builder + Alpine JRE runtime
 docker-compose.yml            # app + dynamodb-local + persistent volume
 .env                          # Local environment variables
 README.md
```

---

##  Tech Stack

| Technology | Version | Role |
|---|---------|---|
| Java | 17      | Language |
| Spring Boot | 3.5.11  | Framework |
| AWS SDK v2 | 2.25.40 | DynamoDB client |
| DynamoDB Local | 2.4.0   | Local database |
| SpringDoc OpenAPI | 2.5.0   | Swagger UI |
| Logstash Logback | 7.4     | Structured JSON logging |
| Testcontainers + LocalStack | 1.19.7  | Integration testing |
| Docker (Multi-stage + Alpine) |         | Containerisation |
| AWS App Runner |         | Production compute |
| AWS CloudFormation |         | Infrastructure as Code |

---

<p align="center">Made with  for BTG Pactual Technical Assessment</p>
