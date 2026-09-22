# 🛒 E-Commerce Microservices Platform

A production-inspired **E-Commerce Backend built with Java, Spring Boot, Spring Cloud, PostgreSQL, Apache Kafka, Docker and JWT-based security**.

The project follows a **distributed microservices architecture** where authentication, products, orders, payments and notifications are separated into independently deployable services.

The primary goal of this project is not just to implement CRUD operations, but to demonstrate practical backend engineering concepts such as:

* Microservices architecture
* API Gateway
* JWT authentication and authorization
* Inter-service communication
* Event-driven architecture
* Apache Kafka
* Optimistic locking
* Inventory concurrency control
* Idempotency
* Transaction compensation
* Order lifecycle management
* Dockerized deployment
* Centralized error handling
* Service-level separation
* Production-oriented backend design

---

## 📌 Project Overview

Traditional monolithic e-commerce applications keep users, products, orders, payments and notifications inside a single application.

This project separates those responsibilities into multiple services.

```text
                         ┌──────────────────────┐
                         │       Client         │
                         │ Web / Mobile / cURL  │
                         └──────────┬───────────┘
                                    │
                                    ▼
                         ┌──────────────────────┐
                         │     API Gateway      │
                         │      :8080           │
                         └──────────┬───────────┘
                                    │
                 ┌──────────────────┼──────────────────┐
                 │                  │                  │
                 ▼                  ▼                  ▼
        ┌────────────────┐ ┌────────────────┐ ┌────────────────┐
        │  User Service  │ │Product Service │ │ Order Service  │
        │     :8081      │ │     :8082      │ │     :8083      │
        └───────┬────────┘ └───────┬────────┘ └───────┬────────┘
                │                  │                  │
                ▼                  ▼                  ▼
        ┌───────────────┐ ┌────────────────┐ ┌────────────────┐
        │ PostgreSQL DB │ │ PostgreSQL DB  │ │ PostgreSQL DB  │
        │   User DB     │ │  Product DB    │ │   Order DB     │
        └───────────────┘ └────────────────┘ └────────────────┘
                                                     │
                                                     │ Kafka
                                                     ▼
                                           ┌──────────────────┐
                                           │ Payment Service  │
                                           │      :8084       │
                                           └────────┬─────────┘
                                                    │
                                                    ▼
                                           ┌──────────────────┐
                                           │Notification       │
                                           │Service :8085     │
                                           └──────────────────┘
```

---

# 🧩 Microservices

## 1. API Gateway

**Port:** `8080`

The API Gateway acts as the single entry point for external clients.

Responsibilities:

* Route incoming requests
* Hide internal service topology
* Provide a single public API entry point
* Forward requests to appropriate services
* Centralize cross-cutting concerns

Example:

```text
Client
   |
   v
/api/users/**     -> User Service
/api/products/**  -> Product Service
/api/orders/**    -> Order Service
/api/payments/**  -> Payment Service
```

---

## 2. User Service

**Port:** `8081`

Responsible for user-related functionality.

Responsibilities:

* User registration
* User authentication
* Password handling
* JWT generation
* Role-based authorization
* User information
* Address management

Supported roles can be used to distinguish between:

```text
CUSTOMER
SELLER
ADMIN
```

Authentication flow:

```text
Client
   |
   | Login
   v
User Service
   |
   | Validate credentials
   |
   | Generate JWT
   v
Client
   |
   | JWT
   v
API Gateway
```

---

# 3. Product Service

**Port:** `8082`

Responsible for product and inventory management.

Responsibilities:

* Create products
* Fetch products
* Fetch product by ID
* Filter products by category
* Update products
* Delete/deactivate products
* Maintain stock quantity
* Validate stock availability

Each product contains information such as:

```text
Product
├── id
├── name
├── description
├── SKU
├── price
├── stockQuantity
├── category
├── sellerEmail
├── active
├── version
├── createdAt
└── updatedAt
```

---

# 🔐 Optimistic Locking

Inventory updates use JPA optimistic locking.

```java
@Version
private Long version;
```

This prevents lost updates when multiple requests attempt to modify the same entity concurrently.

Example scenario:

```text
Initial Stock = 1
```

Two customers attempt to buy the last item simultaneously.

```text
Customer A ---> Product
Customer B ---> Product
```

Both transactions may initially read:

```text
stock = 1
```

However, the version field ensures that only the transaction operating on the valid entity version can successfully commit.

Conceptually:

```text
Version 5

Request A:
Version 5 -> Update -> Version 6 ✅

Request B:
Version 5 -> Update
             ↓
       Optimistic locking failure ❌
```

This prevents inventory from incorrectly becoming negative or being updated twice.

---

# 4. Order Service

**Port:** `8083`

This is the core business service responsible for order management.

Responsibilities:

* Create orders
* Read customer orders
* Fetch order details
* Cancel orders
* Update order status
* Inventory coordination
* Idempotent order creation
* Publish order events

---

# 🛒 Order Placement Flow

The order placement flow works approximately like this:

```text
Customer
   |
   | POST /api/orders
   v
Order Service
   |
   | Load cart
   v
Cart validation
   |
   | For each item
   v
Product Service
   |
   | reduceStock()
   v
Inventory updated
   |
   v
Order created
   |
   v
Cart cleared
   |
   v
Kafka
   |
   └── order-placed-topic
```

---

# 🔄 Failed Order & Stock Rollback

One of the important distributed-system scenarios handled by the project is partial failure.

Suppose an order contains multiple products:

```text
Product A -> stock successfully reduced ✅
Product B -> stock successfully reduced ✅
Product C -> stock reduction fails ❌
```

Without compensation:

```text
Product A stock = reduced
Product B stock = reduced
Order = failed
```

This would leave the inventory inconsistent.

The project therefore tracks already processed cart items and restores their inventory when the order placement flow fails.

```text
Product A -> reduce ✅
Product B -> reduce ✅
Product C -> fail ❌
                    |
                    v
              Compensation
                    |
             ┌──────┴──────┐
             ▼             ▼
          Product A     Product B
          increase      increase
```

This is a practical example of a **compensating transaction pattern** in a distributed system.

---

# ♻️ Order Cancellation

Customers can cancel their own orders.

Endpoint:

```http
PUT /api/orders/{orderId}/cancel
```

The service validates:

1. Order exists
2. Order belongs to the authenticated user
3. Order has not already reached a non-cancellable lifecycle stage

Orders that are already:

```text
SHIPPED
DELIVERED
```

cannot be cancelled.

When cancellation occurs:

```text
Order Service
     |
     | status = CANCELLED
     |
     v
Database
     |
     v
Kafka
     |
     | order-cancelled-topic
     v
Inventory restoration
```

The cancellation event contains the cancelled order and the affected product quantities so downstream processing can restore inventory.

---

# 📦 Order Lifecycle

Current order status model:

```text
PLACED
   |
   v
CONFIRMED
   |
   v
SHIPPED
   |
   v
DELIVERED
```

Cancellation is allowed before shipment/delivery according to the implemented state rules.

Invalid transitions are rejected.

For example:

```text
PLACED -> CONFIRMED      ✅
CONFIRMED -> SHIPPED     ✅
SHIPPED -> DELIVERED     ✅

DELIVERED -> SHIPPED     ❌
PLACED -> SHIPPED        ❌
DELIVERED -> CANCELLED   ❌
```

This keeps the domain state consistent instead of allowing arbitrary status changes.

---

# 🔁 Idempotency

Order creation supports an idempotency key.

Example:

```json
{
  "addressId": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
  "idempotencyKey": "checkout-12345"
}
```

If the same idempotency key is submitted again, the service checks whether an order already exists for that key.

This is useful when clients retry requests because of:

* Network timeout
* Client retry
* Gateway timeout
* Temporary connection failure

Without idempotency:

```text
Request
   |
   | timeout
   v
Client retries
   |
   v
Second order created ❌
```

With idempotency:

```text
Request #1
   |
   v
Order created ✅
   |
Request #2 with same key
   |
   v
Existing order returned ✅
```

This prevents duplicate order creation caused by retries.

---

# 💳 5. Payment Service

**Port:** `8084`

The payment service is responsible for payment processing and payment persistence.

Responsibilities:

* Process payments
* Store payment details
* Generate transaction identifiers
* Maintain payment status
* Publish payment events
* Expose payment information

The current project uses simulated payment processing for development/testing.

Payment success is intentionally simulated rather than connected to a real payment provider.

This keeps the project focused on distributed backend architecture without requiring an external payment gateway.

---

# 📧 6. Notification Service

**Port:** `8085`

The notification service consumes asynchronous events and is responsible for notification-related processing.

Instead of tightly coupling notification logic to order/payment APIs, events can be published through Kafka.

Example:

```text
Order Service
     |
     | OrderPlacedEvent
     v
    Kafka
     |
     v
Notification Service
```

This decouples notification processing from the main order flow.

---

# 📨 Event-Driven Architecture

Apache Kafka is used for asynchronous communication.

Current event-driven flows include concepts such as:

```text
Order Placed
     |
     v
order-placed-topic
```

and

```text
Order Cancelled
     |
     v
order-cancelled-topic
```

Payment events are also published through Kafka.

---

# 🔥 Why Kafka?

Kafka is useful here because some operations do not need to happen synchronously.

For example:

```text
Place Order
    |
    +---- Save Order
    |
    +---- Publish Event
              |
              +---- Notification
              |
              +---- Other future consumers
```

Instead of making the order service directly wait for every downstream consumer, Kafka allows asynchronous processing.

Benefits:

* Loose coupling
* Asynchronous processing
* Independent consumers
* Event-driven architecture
* Better scalability
* Replayable event streams

---

# 🔗 Synchronous vs Asynchronous Communication

The project demonstrates both communication models.

## Synchronous

Order Service communicates with Product Service using OpenFeign.

```text
Order Service
     |
     | HTTP / REST
     v
Product Service
```

Used for operations where the response is immediately required.

Example:

```text
reduceStock()
increaseStock()
```

## Asynchronous

Kafka is used for event-driven operations.

```text
Order Service
      |
      | Kafka event
      v
    Kafka
      |
      v
Notification / Payment / Other consumers
```

This distinction demonstrates practical distributed-system design.

---

# 🗄️ Database Architecture

The project uses PostgreSQL.

Services are logically separated into their own databases:

```text
PostgreSQL
│
├── ecommerce_user_db
├── ecommerce_product_db
├── ecommerce_order_db
└── ecommerce_payment_db
```

This follows the microservices principle of **service-owned data** instead of sharing one database across all services.

---

# 🐳 Docker Architecture

The complete environment can be started using Docker Compose.

Infrastructure includes:

```text
PostgreSQL
Kafka

User Service
Product Service
Order Service
Payment Service
Notification Service
API Gateway
```

Run:

```bash
docker compose up --build
```

Stop:

```bash
docker compose down
```

Stop and remove persisted database volume:

```bash
docker compose down -v
```

---

# ⚙️ Environment Variables

Sensitive configuration should be supplied using environment variables.

Example:

```env
DB_USERNAME=postgres
DB_PASSWORD=your_password
JWT_SECRET=your_jwt_secret
```

Do not commit real secrets to Git.

Recommended local setup:

```text
.env
.env.example
```

`.env.example` should contain placeholders only.

---

# 🚀 Running the Project Locally

## Prerequisites

Install:

* Java
* Maven
* Docker
* Docker Compose
* Git

Optional:

* IntelliJ IDEA
* Postman
* cURL

---

## Clone Repository

```bash
git clone https://github.com/raahulllkushwaha/E-Commerce-Microservices.git
```

```bash
cd E-Commerce-Microservices
```

---

## Configure Environment

Create a `.env` file:

```env
DB_USERNAME=postgres
DB_PASSWORD=your_password
JWT_SECRET=your_secret
```

Use strong values for real environments.

---

## Start Infrastructure and Services

```bash
docker compose up --build
```

The gateway will be available at:

```text
http://localhost:8080
```

---

# 🌐 Service Ports

| Service              | Port |
| -------------------- | ---: |
| API Gateway          | 8080 |
| User Service         | 8081 |
| Product Service      | 8082 |
| Order Service        | 8083 |
| Payment Service      | 8084 |
| Notification Service | 8085 |
| PostgreSQL           | 5433 |
| Kafka                | 9092 |

---

# 🧪 Testing with cURL

## Register User

```bash
curl -X POST http://localhost:8080/api/users/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Rahul",
    "email": "rahul@example.com",
    "password": "password"
  }'
```

---

## Login

```bash
curl -X POST http://localhost:8080/api/users/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "rahul@example.com",
    "password": "password"
  }'
```

Store the returned JWT.

```text
JWT_TOKEN=...
```

Use the token for authenticated requests:

```bash
-H "Authorization: Bearer $JWT_TOKEN"
```

---

# 📦 Product APIs

Example:

```http
GET /api/products
```

Fetch a product:

```http
GET /api/products/{id}
```

Create a product:

```http
POST /api/products
```

Update:

```http
PUT /api/products/{id}
```

Delete/deactivate:

```http
DELETE /api/products/{id}
```

---

# 🛒 Order APIs

Create order:

```http
POST /api/orders
```

Get logged-in user's orders:

```http
GET /api/orders
```

Get order by ID:

```http
GET /api/orders/{id}
```

Cancel order:

```http
PUT /api/orders/{id}/cancel
```

Update order status:

```http
PUT /api/orders/{id}/status?status=CONFIRMED
```

---

# 🔐 Authorization Example

Authenticated requests use:

```http
Authorization: Bearer <JWT>
```

The system uses authenticated identity information when performing user-specific operations.

For example, order cancellation checks that the authenticated user's email matches the order owner.

This prevents one customer from cancelling another customer's order.

---

# 🧠 Concurrency Test

A key test scenario is buying the final available product using concurrent requests.

Suppose:

```text
Stock = 1
```

Two requests are executed simultaneously:

```text
Request A -> Buy quantity 1
Request B -> Buy quantity 1
```

Expected result:

```text
Request A -> Success ✅
Request B -> Failure ❌
```

The product entity uses:

```java
@Version
private Long version;
```

to provide optimistic locking at the persistence layer.

This is particularly important for e-commerce inventory because two customers should not both successfully purchase the same final item.

---

# 🧱 Project Structure

```text
E-Commerce-Microservices/
│
├── ApiGateway/
│   ├── src/
│   ├── pom.xml
│   └── Dockerfile
│
├── UserService/
│   ├── src/
│   ├── pom.xml
│   └── Dockerfile
│
├── ProductService/
│   ├── src/
│   ├── pom.xml
│   └── Dockerfile
│
├── Order-Service/
│   ├── src/
│   ├── pom.xml
│   └── Dockerfile
│
├── Payment-Service/
│   ├── src/
│   ├── pom.xml
│   └── Dockerfile
│
├── Notification-Service/
│   ├── src/
│   ├── pom.xml
│   └── Dockerfile
│
├── docker-compose.yml
├── init-db.sql
└── README.md
```

---

# 🏗️ Architectural Patterns Demonstrated

## Microservices Architecture

Business capabilities are separated into independent services.

```text
Users
Products
Orders
Payments
Notifications
```

## API Gateway Pattern

External clients communicate through one gateway.

## Event-Driven Architecture

Kafka events decouple asynchronous consumers.

## Saga / Compensation Concept

Inventory changes can be compensated when an order flow fails.

## Optimistic Locking

`@Version` protects concurrent entity modifications.

## Idempotency

Idempotency keys prevent duplicate order creation during retries.

## Role-Based Authorization

Different application roles can receive different permissions.

## DTO Pattern

Requests and responses are separated from persistence entities.

---

# 🛡️ Error Handling

The services use custom exceptions for domain-specific failures such as:

* Resource not found
* Invalid credentials
* Insufficient stock
* Invalid order state
* Payment failure
* Duplicate resources

This allows business errors to be represented explicitly rather than exposing generic exceptions everywhere.

---

# 📈 Production-Oriented Improvements Planned

Although this project already demonstrates several production-inspired patterns, the following areas can be extended further:

### Kafka Reliability

* Retry policies
* Dead Letter Topics
* Consumer idempotency
* Event identifiers
* Better delivery guarantees

### Distributed Resilience

* Resilience4j
* Circuit breakers
* Timeouts
* Retries
* Fallback strategies

### Database Management

* Flyway migrations
* Versioned schema changes
* Safer production configuration

### Observability

* Spring Boot Actuator
* Prometheus
* Grafana
* Distributed tracing
* Correlation IDs

### Testing

* Unit tests
* Integration tests
* Testcontainers
* Kafka integration testing
* Concurrency testing

### Documentation

* OpenAPI / Swagger
* Architecture diagrams
* API examples
* Failure scenarios

---

# 📊 Technology Stack

| Category                    | Technology                  |
| --------------------------- | --------------------------- |
| Language                    | Java                        |
| Framework                   | Spring Boot                 |
| Microservices               | Spring Cloud                |
| Gateway                     | Spring Cloud Gateway        |
| Security                    | Spring Security + JWT       |
| REST                        | Spring Web                  |
| Inter-service Communication | OpenFeign                   |
| Messaging                   | Apache Kafka                |
| Database                    | PostgreSQL                  |
| ORM                         | Spring Data JPA / Hibernate |
| Build Tool                  | Maven                       |
| Containerization            | Docker                      |
| API Testing                 | cURL / Postman              |
| Version Control             | Git / GitHub                |

---

# 💡 Engineering Challenges Solved

This project intentionally focuses on backend problems that appear in real distributed systems.

### Problem 1 — Duplicate Orders

Solution:

```text
Idempotency Key
```

### Problem 2 — Concurrent Inventory Updates

Solution:

```text
Optimistic Locking
@Version
```

### Problem 3 — Partial Inventory Update

Solution:

```text
Compensating Stock Rollback
```

### Problem 4 — Tight Coupling

Solution:

```text
Kafka Event-Driven Communication
```

### Problem 5 — Invalid Order States

Solution:

```text
Explicit State Transition Rules
```

### Problem 6 — Unauthorized Cancellation

Solution:

```text
Authenticated User Ownership Validation
```

---

# 🎯 Example End-to-End Flow

A complete customer checkout can conceptually look like this:

```text
                CUSTOMER
                    |
                    v
              API GATEWAY
                    |
                    v
              ORDER SERVICE
                    |
              Load Cart
                    |
                    v
             PRODUCT SERVICE
                    |
              Reduce Stock
                    |
                    v
              Create Order
                    |
                    v
               PostgreSQL
                    |
                    v
                  Kafka
                    |
          ┌─────────┴─────────┐
          |                   |
          v                   v
   PAYMENT / EVENTS      NOTIFICATION
```

Cancellation:

```text
CUSTOMER
   |
   v
API GATEWAY
   |
   v
ORDER SERVICE
   |
   | Validate ownership
   |
   | Validate order state
   |
   v
CANCELLED
   |
   v
Kafka
   |
   v
order-cancelled-topic
   |
   v
Inventory Restoration
```

---

# 🔭 Future Roadmap

The project can evolve toward a more complete production-grade platform with:

* Payment gateway integration
* Redis caching
* Kafka retry + DLT
* Resilience4j
* Flyway
* Prometheus + Grafana
* OpenTelemetry
* Distributed tracing
* Elasticsearch
* Centralized logging
* Refresh tokens
* API rate limiting
* Kubernetes deployment
* CI/CD pipelines
* Testcontainers
* Contract testing
* React frontend
* Search and filtering
* Product reviews
* Coupon system
* Order history
* Email notifications
* Inventory reservation

---

# 👨‍💻 Author

**Rahul Kushwaha**

Java Backend / Spring Boot Developer

### Interests

* Java
* Spring Boot
* Microservices
* Kafka
* Distributed Systems
* Docker
* Backend Engineering

---

# ⭐ Why This Project?

This project is designed to demonstrate more than framework knowledge.

It focuses on understanding:

```text
How services communicate
How distributed failures are handled
How inventory consistency is maintained
How duplicate requests are prevented
How asynchronous events are processed
How authentication works across services
How business state transitions are enforced
```

The goal is to build backend systems that are not only functional, but also **reliable, maintainable and scalable**.

---

# 📜 License

This project is intended for educational and portfolio purposes.
