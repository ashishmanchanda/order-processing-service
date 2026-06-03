# 📦 Order Processing Service

A production-ready Spring Boot REST API for e-commerce order processing.

---

## 🚀 Quick Start (IntelliJ IDEA)

### Prerequisites
- Java 17 (JDK)
- Maven 3.8+
- IntelliJ IDEA (any edition)

### Steps

1. **Open the project**
   - File → Open → select the `order-processing-service` folder
   - IntelliJ will auto-detect it as a Maven project

2. **Enable annotation processing** *(required for Lombok + MapStruct)*
   - Settings → Build, Execution, Deployment → Compiler → Annotation Processors
   - ✅ Check **"Enable annotation processing"**

3. **Run the application**
   - Open `OrderServiceApplication.java`
   - Click the green ▶ Run button
   - Or use the Maven panel: `spring-boot:run`

4. **Access the app**

   | URL | Description |
      |-----|-------------|
   | http://localhost:8080/swagger-ui.html | **Swagger UI** — try all APIs here |
   | http://localhost:8080/h2-console | **H2 Console** — browse in-memory DB |
   | http://localhost:8080/actuator/health | Health check |

   > **H2 Console login:** JDBC URL: `jdbc:h2:mem:orderdb` · User: `sa` · Password: *(blank)*

---

## 🔧 Configuration Profiles

| Profile | Database | Use Case |
|---------|----------|----------|
| `dev` *(default)* | H2 in-memory | Local development & demos |
| `test` | H2 in-memory | Automated tests |
| `prod` | PostgreSQL | Production deployment |

**Switch profiles in IntelliJ:**
- Edit Run Configuration → Environment variables: `SPRING_PROFILES_ACTIVE=prod`

---

## 🐘 PostgreSQL Setup (Production Profile)

Follow these steps to set up PostgreSQL and run the service with the `prod` profile.

### Step 1 — Install PostgreSQL

**macOS (Homebrew):**
```bash
brew install postgresql@16
brew services start postgresql@16
```

**Ubuntu / Debian:**
```bash
sudo apt update && sudo apt install -y postgresql postgresql-contrib
sudo systemctl start postgresql
sudo systemctl enable postgresql
```

**Windows:** Download the installer from [postgresql.org/download/windows](https://www.postgresql.org/download/windows/) and follow the wizard.

---

### Step 2 — Create the Database (pgAdmin 4)

> **Install pgAdmin 4:** Download from [pgadmin.org/download](https://www.pgadmin.org/download/) — the official GUI for PostgreSQL.

#### 2a. Register a server connection

1. Open pgAdmin 4
2. Right-click **Servers** in the left sidebar → **Create → Server...**
3. **General tab** → Name: `Local PostgreSQL`
4. **Connection tab** → fill in:

   | Field | Value |
      |-------|-------|
   | Host | `localhost` |
   | Port | `5432` |
   | Maintenance database | `postgres` |
   | Username | `postgres` |
   | Password | your postgres password |
   | Save password | ✅ |

5. Click **Save**

#### 2b. Create the `orderdb` database

1. In the sidebar, expand your server → right-click **Databases** → **Create → Database...**
2. **General tab:**
   - Database: `orderdb`
   - Owner: `postgres`
3. **Definition tab:**
   - Template: `template0` *(avoids locale mismatch errors)*
   - Encoding: `UTF8`
4. Click **Save**

> **Locale error?** If you see `HINT: Use the same locale provider as in the template database`, make sure Template is set to `template0` in the Definition tab, not the default `template1`.

#### 2c. Create the tables

1. Click `orderdb` in the sidebar to select it
2. Click the **Query Tool** button in the top toolbar (or right-click `orderdb` → **Query Tool**)
3. Paste the SQL below and press **F5** (or click ▶ Execute):

```sql
-- Orders table
CREATE TABLE IF NOT EXISTS orders (
    id             BIGSERIAL    PRIMARY KEY,
    customer_name  VARCHAR(255) NOT NULL,
    customer_email VARCHAR(255) NOT NULL,
    status         VARCHAR(50)  NOT NULL,
    created_at     TIMESTAMP,
    updated_at     TIMESTAMP
);

-- Order items table
CREATE TABLE IF NOT EXISTS order_items (
    id           BIGSERIAL      PRIMARY KEY,
    order_id     BIGINT         NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_name VARCHAR(255)   NOT NULL,
    product_code VARCHAR(255)   NOT NULL,
    quantity     INT            NOT NULL,
    unit_price   NUMERIC(10, 2) NOT NULL
);
```

4. Verify under **orderdb → Schemas → public → Tables** — you should see `orders` and `order_items`

---

### Step 3 — Run the App with Prod Profile

**One-liner (from the project root):**
```bash
cd order-management-service && SPRING_PROFILES_ACTIVE=prod DB_URL=jdbc:postgresql://localhost:5432/orderdb DB_USERNAME=postgres DB_PASSWORD=changeme java -jar target/order-service-1.0.0.jar
```

**Or with explicit JVM flags:**
```bash
java -jar target/order-service-1.0.0.jar \
  -Dspring.profiles.active=prod \
  -DDB_URL=jdbc:postgresql://localhost:5432/orderdb \
  -DDB_USERNAME=postgres \
  -DDB_PASSWORD=changeme
```

> Replace `changeme` with your actual PostgreSQL password.

**If the jar is missing, build it first:**
```bash
mvn clean package -DskipTests
```

---

### Step 4 — Verify Everything is Working

**Health check:**
```bash
curl http://localhost:8080/actuator/health
```
Expected response:
```json
{ "status": "UP", "components": { "db": { "status": "UP" } } }
```

**Create a test order:**
```bash
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerName": "John Doe",
    "customerEmail": "john@example.com",
    "items": [
      {
        "productName": "Wireless Keyboard",
        "productCode": "KB-001",
        "quantity": 1,
        "unitPrice": 49.99
      }
    ]
  }'
```

**Verify the row in pgAdmin:**
```sql
SELECT * FROM orders;
SELECT * FROM order_items;
```

---

### Troubleshooting

| Error | Cause | Fix |
|-------|-------|-----|
| `Unable to access jarfile` | Wrong directory or jar not built | `cd order-management-service` then `mvn clean package -DskipTests` |
| `missing column [x] in table` | Table schema doesn't match entities | Re-run the CREATE TABLE SQL from Step 2c |
| `Connection refused` on 5432 | PostgreSQL not running | `sudo systemctl start postgresql` (Linux) or `brew services start postgresql@16` (Mac) |
| `password authentication failed` | Wrong credentials | Check `DB_PASSWORD` matches what you set in pgAdmin |
| Locale mismatch on DB creation | `template1` locale conflict | Use `template0` in pgAdmin Definition tab |

---

## 🐳 Docker (optional — includes PostgreSQL)

To run the full stack (app + PostgreSQL) via Docker:

**Step 1:** Edit `docker-compose.yml` — uncomment the `postgres` service block and update the app environment:
```yaml
environment:
  - SPRING_PROFILES_ACTIVE=prod
  - DB_URL=jdbc:postgresql://postgres:5432/orderdb
  - DB_USERNAME=postgres
  - DB_PASSWORD=secret
```

**Step 2:** Run:
```bash
docker-compose up --build
```

**Stop:**
```bash
docker-compose down
```

---

## 📋 API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/v1/orders` | Create a new order |
| `GET` | `/api/v1/orders` | List all orders (optional `?status=PENDING`) |
| `GET` | `/api/v1/orders/{id}` | Get order by ID |
| `PATCH` | `/api/v1/orders/{id}/status` | Update order status |
| `DELETE` | `/api/v1/orders/{id}/cancel` | Cancel a PENDING order |

### Order Statuses
```
PENDING → PROCESSING → SHIPPED → DELIVERED
              ↑
    (auto-promoted by scheduler every 5 min)

Any status except CANCELLED can be manually updated via PATCH.
Only PENDING orders can be cancelled.
```

### Sample Request — Create Order
```json
POST /api/v1/orders
{
  "customerName": "John Doe",
  "customerEmail": "john@example.com",
  "items": [
    {
      "productName": "Wireless Keyboard",
      "productCode": "KB-001",
      "quantity": 1,
      "unitPrice": 49.99
    }
  ]
}
```

---

## ⏰ Background Scheduler

A Spring `@Scheduled` job runs every **5 minutes** (30 seconds in dev profile) and automatically promotes all `PENDING` orders to `PROCESSING`.

You can configure the interval:
```properties
# application-dev.properties
scheduler.order.promote.interval=30000   # 30 seconds (demo)

# application-prod.properties  
scheduler.order.promote.interval=300000  # 5 minutes (production)
```

---

## 🧪 Running Tests

```bash
# All tests
mvn test

# Unit tests only
mvn test -Dtest=OrderServiceTest

# Integration tests only
mvn test -Dtest=OrderControllerIntegrationTest
```

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    HTTP Clients / Swagger                │
└────────────────────────┬────────────────────────────────┘
                         │
               ┌─────────▼──────────┐
               │  OrderController   │  REST layer + validation
               └─────────┬──────────┘
                         │
               ┌─────────▼──────────┐
               │   OrderService     │  Business logic + transactions
               └──────┬──────┬──────┘
                      │      │
          ┌───────────▼─┐  ┌─▼────────────────┐
          │OrderRepository│  │ OrderStatusScheduler│
          │  (JPA/H2/PG) │  │ (@Scheduled job)    │
          └─────────────┘  └──────────────────────┘
```

---

## 📁 Project Structure

```
src/main/java/com/orders/orderservice/
├── OrderServiceApplication.java       # Entry point
├── controller/OrderController.java    # REST endpoints
├── service/OrderService.java          # Business logic
├── scheduler/OrderStatusScheduler.java# Background job
├── repository/OrderRepository.java    # Data access
├── model/                             # JPA entities
│   ├── Order.java
│   ├── OrderItem.java
│   └── OrderStatus.java (enum)
├── dto/                               # Request/Response DTOs
├── exception/                         # Custom exceptions + global handler
└── config/                            # MapStruct mapper
```
