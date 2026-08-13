# M-Pesa Daraja API Integration Documentation

## Mtugo Hotel – M-Pesa Express Payment System

---

## 1. Overview

This document outlines the complete integration of Safaricom's M-Pesa Daraja API (STK Push) into the Mtugo Hotel ordering system. The integration allows customers to pay for meals via M-Pesa Express directly from the hotel's web application.

### 1.1 Key Features

- **STK Push (Lipa Na M-Pesa Online)**: Sends a payment prompt to the customer's phone.
- **OAuth 2.0 Authentication**: Secure access token generation with auto‑refresh.
- **Callback Handling**: Real‑time payment confirmation via webhook.
- **Transaction & Order Tracking**: Full audit trail with database records.
- **Local Development Tunnel**: Ngrok exposes the local callback URL to Safaricom.

### 1.2 Technologies Used

| Technology | Purpose |
|------------|---------|
| Spring Boot 4.1.0 | Backend framework |
| Java 21 | Programming language |
| H2 Database | Local/in‑memory database for testing |
| Maven | Build and dependency management |
| Ngrok | Public URL tunnel for local callbacks |
| Jackson | JSON parsing |

---

## 2. Prerequisites

### 2.1 Account Setup

- **Safaricom Developer Account**: Registered at [developer.safaricom.co.ke](https://developer.safaricom.co.ke/).
- **App Created**: A sandbox app with the **Lipa Na M-Pesa** API enabled.
- **Credentials Obtained**:
  - Consumer Key
  - Consumer Secret
  - Shortcode (sandbox: `174379`)
  - Passkey (provided via email/portal)

### 2.2 Local Environment

- JDK 17+
- Maven 3.8+
- Git Bash / PowerShell terminal
- Ngrok (downloaded and authenticated)
- Postman (optional, for testing)

---

## 3. Project Setup & Dependencies

### 3.1 Spring Boot Initializr

The project was generated using [Spring Initializr](https://start.spring.io/) with the following selections:

| Field | Selection |
|-------|-----------|
| Project | Maven |
| Language | Java |
| Spring Boot | 4.1.0 |
| Group | com.mtugo |
| Artifact | mtugo_hotel |
| Packaging | Jar |
| Java | 21 |

### 3.2 Dependencies Added

In `pom.xml`:

```xml
<!-- Spring Boot Starters -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-thymeleaf</artifactId>
</dependency>

<!-- JSON parsing -->
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
</dependency>

<!-- Database -->
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>runtime</scope>
</dependency>

<!-- Lombok -->
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>

<!-- Testing -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

---

## 4. Configuration

### 4.1 `application.properties`

The M-Pesa credentials were added directly to `src/main/resources/application.properties` for simplicity during local development:

```properties
# ============================================================
# MTUGO HOTEL - H2 DATABASE CONFIGURATION
# ============================================================

server.port=8080
spring.application.name=mtugo-hotel

spring.datasource.url=jdbc:h2:mem:mtugo_hotel;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=MySQL
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
spring.h2.console.settings.trace=false
spring.h2.console.settings.web-allow-others=false

spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

logging.level.com.mtugo=DEBUG
logging.level.org.hibernate=INFO

# ============================================================
# M-PESA DARAJA API CONFIGURATION
# ============================================================

mpesa.environment=sandbox
mpesa.consumer-key=o6qAtwgxHxmBMTYFQdDsLTLMyMbdQdv59A9cQkymrXEDQJvz
mpesa.consumer-secret=ezuBQSqII246ZaD7Tlx7AMW83g3VjLOWH03B0P790K1NYAszAmGdYAPKtAgQS6xB
mpesa.shortcode=174379
mpesa.passkey=bfb279f9aa9bdbcf158e97dd71a467cd2e0c893059b10f78e6b72ada1ed2c919
mpesa.callback-url=https://showroom-dismay-sanctity.ngrok-free.dev/api/mpesa/callback
```

### 4.2 `.gitignore` Configuration

To prevent credentials from being committed to version control, `src/main/resources/application.properties` was added to `.gitignore`:

```
# Credentials (application.properties contains secrets)
src/main/resources/application.properties
src/main/resources/application-dev.properties

# IDE
.idea/
*.iml
.vscode/
.classpath
.project
.settings/

# Build
target/
*.class
*.jar
*.war

# Logs
logs/
*.log

# System files
.DS_Store
Thumbs.db
```

---

## 5. Code Implementation

### 5.1 `MpesaConfig.java` – Configuration Loader

**Location:** `src/main/java/com/mtugo/mtugo_hotel/config/MpesaConfig.java`

```java
package com.mtugo.mtugo_hotel.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MpesaConfig {

    @Value("${mpesa.environment:sandbox}")
    private String environment;

    @Value("${mpesa.consumer-key}")
    private String consumerKey;

    @Value("${mpesa.consumer-secret}")
    private String consumerSecret;

    @Value("${mpesa.shortcode}")
    private String shortcode;

    @Value("${mpesa.passkey}")
    private String passkey;

    @Value("${mpesa.callback-url}")
    private String callbackUrl;

    public String getBaseUrl() {
        return "sandbox".equalsIgnoreCase(environment)
                ? "https://sandbox.safaricom.co.ke"
                : "https://api.safaricom.co.ke";
    }

    // Getters
    public String getEnvironment() { return environment; }
    public String getConsumerKey() { return consumerKey; }
    public String getConsumerSecret() { return consumerSecret; }
    public String getShortcode() { return shortcode; }
    public String getPasskey() { return passkey; }
    public String getCallbackUrl() { return callbackUrl; }
}
```

---

### 5.2 `MpesaService.java` – Core API Logic

**Location:** `src/main/java/com/mtugo/mtugo_hotel/service/MpesaService.java`

```java
package com.mtugo.mtugo_hotel.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtugo.mtugo_hotel.config.MpesaConfig;
import com.mtugo.mtugo_hotel.dto.MpesaStkPushRequest;
import com.mtugo.mtugo_hotel.dto.MpesaStkPushResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

@Service
public class MpesaService {

    private static final Logger log = LoggerFactory.getLogger(MpesaService.class);

    private final MpesaConfig mpesaConfig;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private String cachedToken;
    private long tokenExpiryTime;

    @Autowired
    public MpesaService(MpesaConfig mpesaConfig) {
        this.mpesaConfig = mpesaConfig;
    }

    /**
     * Get OAuth access token from Daraja API
     */
    public String getAccessToken() throws Exception {
        if (cachedToken != null && System.currentTimeMillis() < tokenExpiryTime) {
            return cachedToken;
        }

        log.info("Fetching new access token from Daraja...");

        String credentials = mpesaConfig.getConsumerKey() + ":" + mpesaConfig.getConsumerSecret();
        String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(mpesaConfig.getBaseUrl() + "/oauth/v1/generate?grant_type=client_credentials"))
                .header("Authorization", "Basic " + encodedCredentials)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            log.error("Failed to get access token. Status: {}, Body: {}", response.statusCode(), response.body());
            throw new RuntimeException("Failed to get access token: " + response.body());
        }

        JsonNode json = objectMapper.readTree(response.body());
        String token = json.get("access_token").asText();
        int expiresIn = json.get("expires_in").asInt();

        cachedToken = token;
        tokenExpiryTime = System.currentTimeMillis() + (expiresIn - 60) * 1000L;

        log.info("Access token obtained successfully. Expires in {} seconds.", expiresIn);
        return token;
    }

    /**
     * Initiate STK Push payment
     */
    public MpesaStkPushResponse initiateStkPush(MpesaStkPushRequest request) throws Exception {
        log.info("Initiating STK Push - orderId: {}, phone: {}, amount: {}",
                request.getOrderId(), request.getPhone(), request.getAmount());

        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

        String passwordStr = mpesaConfig.getShortcode() + mpesaConfig.getPasskey() + timestamp;
        String password = Base64.getEncoder().encodeToString(passwordStr.getBytes());

        String phone = formatPhoneNumber(request.getPhone());

        String requestBody = String.format(
                "{\"BusinessShortCode\":\"%s\",\"Password\":\"%s\",\"Timestamp\":\"%s\"," +
                "\"TransactionType\":\"CustomerPayBillOnline\",\"Amount\":\"%d\"," +
                "\"PartyA\":\"%s\",\"PartyB\":\"%s\",\"PhoneNumber\":\"%s\"," +
                "\"CallBackURL\":\"%s\",\"AccountReference\":\"ORDER-%d\"," +
                "\"TransactionDesc\":\"Mtugo Hotel Meal Payment\"}",
                mpesaConfig.getShortcode(),
                password,
                timestamp,
                request.getAmount().intValue(),
                phone,
                mpesaConfig.getShortcode(),
                phone,
                mpesaConfig.getCallbackUrl(),
                request.getOrderId()
        );

        log.debug("STK Push request: {}", requestBody);

        String accessToken = getAccessToken();

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(mpesaConfig.getBaseUrl() + "/mpesa/stkpush/v1/processrequest"))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        log.info("STK Push response: {}", response.body());

        if (response.statusCode() != 200) {
            log.error("STK Push failed. Status: {}, Body: {}", response.statusCode(), response.body());
            throw new RuntimeException("STK Push failed: " + response.body());
        }

        return objectMapper.readValue(response.body(), MpesaStkPushResponse.class);
    }

    /**
     * Format phone number to international format (2547XXXXXXXX)
     */
    private String formatPhoneNumber(String phone) {
        String cleaned = phone.replaceAll("\\D", "");
        if (cleaned.startsWith("0")) {
            cleaned = "254" + cleaned.substring(1);
        } else if (cleaned.startsWith("7")) {
            cleaned = "254" + cleaned;
        } else if (!cleaned.startsWith("254")) {
            cleaned = "254" + cleaned;
        }
        log.debug("Formatted phone: {} -> {}", phone, cleaned);
        return cleaned;
    }
}
```

---

### 5.3 `MpesaController.java` – REST Endpoints

**Location:** `src/main/java/com/mtugo/mtugo_hotel/controller/MpesaController.java`

```java
package com.mtugo.mtugo_hotel.controller;

import com.mtugo.mtugo_hotel.dto.MpesaStkPushRequest;
import com.mtugo.mtugo_hotel.dto.MpesaStkPushResponse;
import com.mtugo.mtugo_hotel.entity.Transaction;
import com.mtugo.mtugo_hotel.service.MpesaService;
import com.mtugo.mtugo_hotel.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/mpesa")
public class MpesaController {

    private static final Logger log = LoggerFactory.getLogger(MpesaController.class);

    private final MpesaService mpesaService;
    private final OrderService orderService;

    @Autowired
    public MpesaController(MpesaService mpesaService, OrderService orderService) {
        this.mpesaService = mpesaService;
        this.orderService = orderService;
    }

    @PostMapping("/stkpush")
    public ResponseEntity<MpesaStkPushResponse> initiateStkPush(@RequestBody MpesaStkPushRequest request) {
        log.info("STK Push request received - orderId: {}, phone: {}, amount: {}",
                request.getOrderId(), request.getPhone(), request.getAmount());

        try {
            // 1. Create a Transaction record (status: INITIATED)
            Transaction transaction = orderService.createTransaction(
                    request.getOrderId(),
                    request.getPhone(),
                    request.getAmount()
            );
            log.info("Transaction created with id: {}", transaction.getId());

            // 2. Send STK Push request to Safaricom
            MpesaStkPushResponse response = mpesaService.initiateStkPush(request);

            // 3. If STK Push was successful, update transaction with CheckoutRequestID
            if ("0".equals(response.getResponseCode())) {
                orderService.updateTransactionCheckoutRequestId(
                        transaction.getId(),
                        response.getCheckoutRequestID()
                );
                log.info("Transaction {} updated with CheckoutRequestID: {}",
                        transaction.getId(), response.getCheckoutRequestID());

                orderService.updateOrderCheckoutRequestId(
                        request.getOrderId(),
                        response.getCheckoutRequestID()
                );
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to initiate STK Push", e);
            throw new RuntimeException("Failed to initiate payment: " + e.getMessage(), e);
        }
    }

    // ===== TEST ENDPOINT TO VERIFY CREDENTIALS =====
    @GetMapping("/test/token")
    public ResponseEntity<Map<String, String>> testToken() {
        Map<String, String> response = new HashMap<>();
        try {
            String token = mpesaService.getAccessToken();
            response.put("status", "success");
            response.put("message", "Access token obtained successfully");
            response.put("token", token.substring(0, 20) + "...");
            response.put("token_length", String.valueOf(token.length()));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Token test failed", e);
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}
```

---

### 5.4 `CallbackController.java` – Payment Confirmation

**Location:** `src/main/java/com/mtugo/mtugo_hotel/controller/CallbackController.java`

```java
package com.mtugo.mtugo_hotel.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtugo.mtugo_hotel.entity.Transaction;
import com.mtugo.mtugo_hotel.repository.TransactionRepository;
import com.mtugo.mtugo_hotel.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mpesa")
public class CallbackController {

    private static final Logger log = LoggerFactory.getLogger(CallbackController.class);

    private final TransactionRepository transactionRepository;
    private final OrderService orderService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public CallbackController(TransactionRepository transactionRepository, OrderService orderService) {
        this.transactionRepository = transactionRepository;
        this.orderService = orderService;
    }

    @PostMapping("/callback")
    public ResponseEntity<String> handleCallback(@RequestBody String payload) {
        log.info("Received M-Pesa callback: {}", payload);

        try {
            JsonNode root = objectMapper.readTree(payload);
            JsonNode stkCallback = root.path("Body").path("stkCallback");

            String checkoutRequestId = stkCallback.path("CheckoutRequestID").asText();
            int resultCode = stkCallback.path("ResultCode").asInt();
            String resultDesc = stkCallback.path("ResultDesc").asText();

            log.info("Callback: CheckoutRequestID: {}, ResultCode: {}, ResultDesc: {}",
                    checkoutRequestId, resultCode, resultDesc);

            // Find the transaction by CheckoutRequestID
            Transaction transaction = transactionRepository.findByCheckoutRequestId(checkoutRequestId)
                    .orElseThrow(() -> new RuntimeException("Transaction not found for checkoutRequestId: " + checkoutRequestId));

            if (resultCode == 0) {
                // Payment successful
                JsonNode metadata = stkCallback.path("CallbackMetadata");
                String receiptNumber = "";
                if (metadata.has("Item")) {
                    for (JsonNode item : metadata.path("Item")) {
                        if ("MpesaReceiptNumber".equals(item.path("Name").asText())) {
                            receiptNumber = item.path("Value").asText();
                            break;
                        }
                    }
                }
                orderService.completeTransactionAndOrder(transaction.getId(), receiptNumber, resultDesc);
                log.info("Transaction {} and order {} marked as PAID", transaction.getId(), transaction.getOrder().getId());
            } else {
                // Payment failed
                orderService.failTransactionAndOrder(transaction.getId(), resultDesc);
                log.info("Transaction {} and order {} marked as FAILED", transaction.getId(), transaction.getOrder().getId());
            }

            return ResponseEntity.ok("Received");

        } catch (Exception e) {
            log.error("Error processing callback", e);
            return ResponseEntity.status(500).body("Error processing callback: " + e.getMessage());
        }
    }
}
```

---

### 5.5 `OrderService.java` – Transaction & Order Methods (Extract)

**Location:** `src/main/java/com/mtugo/mtugo_hotel/service/OrderService.java`

```java
// Relevant methods only – full file includes all order CRUD

public Transaction createTransaction(Long orderId, String phoneNumber, Double amount) {
    Order order = findOrderById(orderId);
    Transaction transaction = new Transaction();
    transaction.setOrder(order);
    transaction.setAmount(amount);
    transaction.setPhoneNumber(phoneNumber);
    transaction.setStatus(TransactionStatus.INITIATED);
    return transactionRepository.save(transaction);
}

public void updateTransactionCheckoutRequestId(Long transactionId, String checkoutRequestId) {
    Transaction transaction = transactionRepository.findById(transactionId)
            .orElseThrow(() -> new RuntimeException("Transaction not found"));
    transaction.setCheckoutRequestId(checkoutRequestId);
    transaction.setStatus(TransactionStatus.PENDING);
    transactionRepository.save(transaction);
}

public void updateOrderCheckoutRequestId(Long orderId, String checkoutRequestId) {
    Order order = findOrderById(orderId);
    order.setCheckoutRequestId(checkoutRequestId);
    orderRepository.save(order);
}

public void completeTransactionAndOrder(Long transactionId, String mpesaReceiptNumber, String resultDesc) {
    Transaction transaction = transactionRepository.findById(transactionId)
            .orElseThrow(() -> new RuntimeException("Transaction not found"));
    transaction.setStatus(TransactionStatus.COMPLETED);
    transaction.setMpesaReceiptNumber(mpesaReceiptNumber);
    transaction.setResultDescription(resultDesc);
    transactionRepository.save(transaction);

    // Mark order as PAID and calculate ETA
    Order order = transaction.getOrder();
    order.setStatus(OrderStatus.PAID);
    order.setPaidAt(LocalDateTime.now());
    int prepTime = order.getMeal().getPrepTimeMinutes();
    int buffer = (int) orderRepository.countByStatusInAndIdNot(
            List.of(OrderStatus.PENDING, OrderStatus.PAID, OrderStatus.PREPARING),
            order.getId()
    ) * 2;
    order.setExpectedReadyAt(LocalDateTime.now().plusMinutes(prepTime + buffer));
    orderRepository.save(order);
}

public void failTransactionAndOrder(Long transactionId, String resultDesc) {
    Transaction transaction = transactionRepository.findById(transactionId)
            .orElseThrow(() -> new RuntimeException("Transaction not found"));
    transaction.setStatus(TransactionStatus.FAILED);
    transaction.setResultDescription(resultDesc);
    transactionRepository.save(transaction);

    Order order = transaction.getOrder();
    order.setStatus(OrderStatus.FAILED);
    orderRepository.save(order);
}
```

---

### 5.6 DTOs

**`MpesaStkPushRequest.java`**

```java
package com.mtugo.mtugo_hotel.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MpesaStkPushRequest {
    private Long orderId;
    private String phone;
    private Double amount;
}
```

**`MpesaStkPushResponse.java`**

```java
package com.mtugo.mtugo_hotel.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MpesaStkPushResponse {
    private String MerchantRequestID;
    private String CheckoutRequestID;
    private String ResponseCode;
    private String ResponseDescription;
}
```

---

### 5.7 Repository Interfaces

**`TransactionRepository.java`**

```java
package com.mtugo.mtugo_hotel.repository;

import com.mtugo.mtugo_hotel.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Optional<Transaction> findByCheckoutRequestId(String checkoutRequestId);
}
```

---

## 6. Ngrok Setup for Local Callbacks

### 6.1 Installation

1. **Download** ngrok from [ngrok.com/download](https://ngrok.com/download).
2. **Extract** `ngrok.exe` to a folder (e.g., `C:\ngrok`).
3. **Add** the folder to your system PATH.
4. **Sign up** for a free account at [ngrok.com](https://ngrok.com).

### 6.2 Authentication

```bash
ngrok config add-authtoken YOUR_AUTH_TOKEN
```

### 6.3 Start the Tunnel

```bash
ngrok http 8080
```

**Output:**
```
Forwarding  https://showroom-dismay-sanctity.ngrok-free.dev -> http://localhost:8080
```

### 6.4 Update Configuration

The callback URL was set to the ngrok URL in `application.properties`:

```properties
mpesa.callback-url=https://showroom-dismay-sanctity.ngrok-free.dev/api/mpesa/callback
```

### 6.5 Keep ngrok Running

**Important**: The ngrok terminal must remain open while testing. The URL changes each time ngrok restarts, so the configuration must be updated accordingly.

---

## 7. Testing Flow

### 7.1 Test Token Endpoint

```bash
curl https://showroom-dismay-sanctity.ngrok-free.dev/api/mpesa/test/token
```

**Expected Response:**
```json
{
    "status": "success",
    "message": "Access token obtained successfully",
    "token": "abcdef1234567890...",
    "token_length": "100"
}
```

### 7.2 Initiate STK Push (via Frontend)

1. Place an order on the web page.
2. Enter phone number (sandbox test number: `254708374149`).
3. Click **"Pay Now"**.
4. The app sends the STK Push request to Safaricom.
5. The customer receives a prompt on their phone (sandbox PIN: any 4 digits).
6. Upon entering PIN, Safaricom sends a callback to the ngrok URL.

### 7.3 Verify Callback

- Check the ngrok terminal for incoming POST requests:
  ```
  POST /api/mpesa/callback   200 OK
  ```
- Check the application logs for order status updates.
- Verify in H2 console that the order status is `PAID` and `expected_ready_at` is set.

---

## 8. Error Handling & Troubleshooting

| Common Error | Cause | Solution |
|--------------|-------|----------|
| `Failed to get access token` | Consumer Key/Secret invalid or network blocked | Verify credentials; test with Postman; check internet connection. |
| `Could not resolve placeholder 'mpesa.consumer-key'` | Property missing in configuration | Add to `application.properties` or activate profile. |
| `Ngrok 4018` | Ngrok not authenticated | Run `ngrok config add-authtoken`. |
| `Ngrok 8012` (connection refused) | Spring Boot not running on port 8080 | Ensure app is started before ngrok. |
| Callback not received | Ngrok URL incorrect or tunnel down | Update `callback-url` and restart ngrok. |
| `ERR_NGROK_8012` | Ngrok cannot reach localhost:8080 | Verify app is running on port 8080. |
| `PlaceholderResolutionException` | Environment variable missing | Hardcode values in `application.properties` temporarily. |

---

## 9. Security Best Practices

- **Never commit secrets** – `application.properties` containing credentials was added to `.gitignore`.
- **Use HTTPS** – ngrok provides HTTPS automatically.
- **Validate callback payload** – check `ResultCode` and ensure `CheckoutRequestID` matches a pending transaction.
- **Log all requests** for audit and debugging.
- **Use sandbox credentials** for development and testing.

---

## 10. Conclusion

This integration successfully enables M-Pesa payments in the Mtugo Hotel ordering system. By leveraging the Daraja STK Push API and Ngrok for local development, we achieved a fully functional payment flow with real-time confirmation.

### 10.1 Key Steps Summary

| Step | Action |
|------|--------|
| 1 | Created Safaricom Developer account and obtained credentials. |
| 2 | Added M-Pesa dependencies to `pom.xml`. |
| 3 | Configured `application.properties` with credentials. |
| 4 | Added `application.properties` to `.gitignore`. |
| 5 | Implemented `MpesaConfig`, `MpesaService`, `MpesaController`, `CallbackController`. |
| 6 | Set up ngrok for local callback testing. |
| 7 | Tested token endpoint and full payment flow. |

### 10.2 References

- [Safaricom Daraja API Documentation](https://developer.safaricom.co.ke/docs)
- [Ngrok Documentation](https://ngrok.com/docs)
- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)

---

**Document Version:** 1.0  
**Date:** 2026-08-13  
**Author:** PRECIOUS ANYANGU

