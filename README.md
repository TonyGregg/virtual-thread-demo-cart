# Spring Boot Cart Microservice with Virtual Threads

This is a Spring Boot microservice for managing shopping cart operations, built with Java 21 and MongoDB. It supports CRUD operations for carts, generates fake cart data using DataFaker, and uses virtual threads selectively for specific endpoints to optimize performance for I/O-bound operations. The project is designed to run MongoDB in a Docker container on a Mac and includes instructions for performance testing with Apache JMeter to compare synchronous and asynchronous (virtual thread) implementations.

## Features
- **CRUD Operations**: Create, read, update, and delete carts stored in MongoDB.
- **Selective Virtual Threads**: Two implementations for adding items and generating fake carts:
    - Synchronous (blocking, platform threads).
    - Asynchronous (non-blocking, virtual threads using `ExecutorService`).
- **Dynamic Cart Metrics**:
    - `subTotal`: Sum of `price * quantity` for all items, rounded to two decimal places.
    - `itemCount`: Total quantity of items (sum of `quantity` fields).
- **MongoDB in Docker**: Runs MongoDB Community Edition in a Docker container for persistence.
- **DataFaker Integration**: Generates realistic fake cart data for testing.
- **Performance Testing**: Configured for JMeter to compare sync vs. async endpoints.

## Prerequisites
- **Java 21**: Required for virtual threads (e.g., OpenJDK 21).
- **Docker**: To run MongoDB in a container.
- **Maven**: For building and running the Spring Boot application.
- **MongoDB Compass** (optional): For GUI-based database management.
- **Apache JMeter**: For performance testing.
- **MacOS**: Instructions are tailored for Mac, but adaptable to other platforms.

## Setup Instructions

### 1. Install Java 21
- Download and install JDK 21 from [Oracle](https://www.oracle.com/java/technologies/downloads/#java21) or use SDKMAN:
  ```bash
  curl -s "https://get.sdkman.io" | bash
  sdk install java 21.0.4-tem
  ```
- Verify: `java --version` (should show OpenJDK 21.x).

### 2. Install Docker
- Download and install [Docker Desktop for Mac](https://docs.docker.com/desktop/install/mac-install/).
- Verify: `docker --version`.

### 3. Run MongoDB in Docker
- Pull the MongoDB Community Server image:
  ```bash
  docker pull mongodb/mongodb-community-server:latest
  ```
- Start the container:
  ```bash
  docker run --name mongodb -d -p 27017:27017 -v mongo-data:/data/db mongodb/mongodb-community-server:latest
  ```
- Verify: `docker ps` (should list the `mongodb` container).
- Optional: Use MongoDB Compass to connect to `mongodb://localhost:27017`.

### 4. Clone and Build the Project
- Clone the repository (or create a new Maven project with the provided code):
  ```bash
  git clone <your-repo-url>
  cd cart-microservice
  ```
- Build with Maven:
  ```bash
  ./gradlew clean build
  ```

### 5. Configure the Application
- Ensure `src/main/resources/application.properties` is set:
  ```properties
  spring.data.mongodb.uri=mongodb://localhost:27017/cartdb
  logging.level.org.springframework=INFO
  ```
- Note: Global virtual threads are disabled to allow selective usage via `ExecutorService`.

### 6. Add MongoDB Index
- To optimize `findByUserId` queries, add an index on the `userId` field:
  ```bash
  docker exec -it mongodb mongosh --eval "use cartdb; db.carts.createIndex({ userId: 1 })"
  ```
- Verify:
  ```javascript
  use cartdb
  db.carts.getIndexes()
  ```

### 7. Run the Application
- Start the Spring Boot app:
  ```bash
  ./gradlew bootRun
  ```
- The app runs on `http://localhost:8080`.

## API Endpoints
All endpoints are prefixed with `/api/v1/carts`.

| Method | Endpoint                          | Description                              | Virtual Threads? |
|--------|-----------------------------------|------------------------------------------|------------------|
| POST   | `/`                              | Create a new cart                       | No               |
| GET    | `/{id}`                          | Get cart by ID                          | No               |
| PUT    | `/{id}`                          | Update cart by ID                       | No               |
| DELETE | `/{id}`                          | Delete cart by ID                       | No               |
| GET    | `/user/{userId}`                 | Get cart by user ID                     | No               |
| GET    | `/`                              | Get all carts                           | No               |
| POST   | `/fake/{count}`                  | Generate `count` fake carts (sync)      | No               |
| POST   | `/fake/async/{count}`            | Generate `count` fake carts (async)     | Yes              |
| POST   | `/add-item/sync/{userId}`        | Add item to cart (sync)                | No               |
| POST   | `/add-item/async/{userId}`       | Add item to cart (async)               | Yes              |

### Example Requests
- **Generate Fake Carts (Sync)**:
  ```bash
  curl -X POST http://localhost:8080/api/v1/carts/fake/10
  ```
- **Generate Fake Carts (Async)**:
  ```bash
  curl -X POST http://localhost:8080/api/v1/carts/fake/async/10
  ```
- **Add Item to Cart (Sync)**:
  ```bash
  curl -X POST http://localhost:8080/api/v1/carts/add-item/sync/user123 \
       -H "Content-Type: application/json" \
       -d '{"productId":"PROD123","productName":"Test Item","quantity":3,"price":9.999}'
  ```
- **Add Item to Cart (Async)**:
  ```bash
  curl -X POST http://localhost:8080/api/v1/carts/add-item/async/user123 \
       -H "Content-Type: application/json" \
       -d '{"productId":"PROD123","productName":"Test Item","quantity":3,"price":9.999}'
  ```
- **Get Cart by User ID**:
  ```bash
  curl http://localhost:8080/api/v1/carts/user/user123
  ```
    - Example Response:
      ```json
      {
          "id": "671f3b2c9a1b2c3d4e5f6a7b",
          "userId": "user123",
          "items": [
              {
                  "productId": "PROD123",
                  "productName": "Test Item",
                  "quantity": 3,
                  "price": 9.999
              }
          ],
          "subTotal": 30.00,
          "itemCount": 3
      }
      ```

## Performance Testing with JMeter
To compare synchronous (`/add-item/sync`, `/fake/{count}`) vs. asynchronous (`/add-item/async`, `/fake/async/{count}`) endpoints:

1. **Install JMeter**:
    - Download from [Apache JMeter](https://jmeter.apache.org/download_jmeter.cgi).
    - Extract and run: `cd apache-jmeter-5.6.3/bin; ./jmeter`.

2. **Create Test Plans**:
    - **Sync Test Plan** (`sync_test.jmx`):
        - Thread Group: 1000 threads, 10-second ramp-up, 10 loops.
        - HTTP Request: `POST /api/v1/carts/add-item/sync/user${__threadNum}`.
        - Body:
          ```json
          {
              "productId": "PROD${__threadNum}",
              "productName": "Test Item ${__threadNum}",
              "quantity": 1,
              "price": 9.99
          }
          ```
        - Add Listeners: Summary Report, Aggregate Report.
    - **Async Test Plan** (`async_test.jmx`):
        - Same as above, but use `POST /api/v1/carts/add-item/async/user${__threadNum}`.

3. **Clear Database Before Each Test**:
    - Drop the `cartdb` database:
      ```bash
      docker exec -it mongodb mongosh --eval "use cartdb; db.dropDatabase()"
      ```
    - Recreate the `userId` index:
      ```bash
      docker exec -it mongodb mongosh --eval "use cartdb; db.carts.createIndex({ userId: 1 })"
      ```
    - Generate test data:
      ```bash
      curl -X POST http://localhost:8080/api/v1/carts/fake/100
      ```

4. **Run Tests**:
    - Run `sync_test.jmx` and save results to `sync_results.csv`.
    - Run `async_test.jmx` and save results to `async_results.csv`.
    - Monitor CPU/memory with Activity Monitor or VisualVM.

5. **Compare Results**:
    - **Throughput**: Async endpoints should have higher requests/second due to virtual threads.
    - **Response Time**: Async should be faster, as MongoDB I/O is offloaded.
    - **Resource Usage**: Async uses less memory (virtual threads ~1 KB vs. platform threads ~1 MB).
    - Example:
        - Sync: 300 req/s, 250 ms avg, 5% errors, 1.2 GB memory.
        - Async: 600 req/s, 200 ms avg, 1% errors, 200 MB memory.

## Troubleshooting
- **MongoDB Connection**: Verify the container is running (`docker ps`) and the URI is `mongodb://localhost:27017/cartdb`.
- **Thread Pinning**: Use VisualVM to check for pinning if async performance is similar to sync (e.g., due to `synchronized` blocks).
- **JMeter Errors**: Check View Results Tree for details (e.g., connection issues).
- **Index Missing**: Recreate the `userId` index if queries are slow.

## Notes
- **Virtual Threads**: Enabled selectively via `ExecutorService` for `/add-item/async` and `/fake/async` endpoints, allowing direct comparison with sync versions.
- **MongoDB**: Data persists in the `mongo-data` volume. Drop the database or collection (`db.carts.drop()`) for clean tests.
- **Production**: Add authentication, input validation, and pagination for large datasets.

## License
MIT License. See [LICENSE](LICENSE) for details.