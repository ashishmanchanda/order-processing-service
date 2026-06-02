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

## 🐳 Docker (optional)

```bash
# Build and run
docker-compose up --build

# Stop
docker-compose down
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


