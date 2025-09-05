[![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-green?logo=spring&logoColor=white)](https://spring.io/projects/spring-boot)
[![SOLID Principles](https://img.shields.io/badge/SOLID-Principles-blue?logo=code&logoColor=white)](https://en.wikipedia.org/wiki/SOLID)
[![Maven](https://img.shields.io/badge/Maven-3.x-red?logo=apache-maven&logoColor=white)](https://maven.apache.org/)

# chaos-solid-missing-example-java
> Intentionally Anti-SOLID Spring Boot App

A tiny Spring Boot (Java 21) project that **intentionally violates every SOLID principle** so you can practice refactoring.

> ⚠️ This code is bad on purpose. Don't copy it to production.

---

## Quickstart

```bash
# Java 21 + Maven required
mvn spring-boot:run
```

Endpoints:

* `POST /checkout?userId=alice&amount=10&paymentType=stripe&sendMarketingEmail=true`
* `GET  /users/{id}`
* `GET  /report`

---

## Project Layout

```
solid-chaos/
├─ pom.xml
└─ src/main/java/com/example/chaos/
   ├─ ChaosApplication.java
   ├─ MegaController.java
   ├─ model/User.java
   ├─ infra/
   │  ├─ StaticUtil.java
   │  ├─ EmailSender.java
   │  ├─ JdbcUserRepository.java
   │  ├─ BaseUserRepository.java
   │  └─ ReadOnlyUserRepository.java
   └─ payments/
      ├─ PaymentOperations.java
      ├─ StripePaymentProcessor.java
      └─ FreeTrialPaymentProcessor.java
```

---

## What’s Wrong (by SOLID principle)

### 1) S — Single Responsibility

**Violations**

* `MegaController` does everything: validation, pricing, DB writes, file I/O, email, logging, config changes.
* `StripePaymentProcessor` also generates reports, backs up the DB, and sends marketing emails.

**Refactor plan**

* Extract responsibilities into services:

  * `PricingService`, `PaymentService`, `UserService`, `ReportingService`, `EmailService`.
* Move persistence to repositories and inject them.
* Replace `StaticUtil.log` with Spring’s `Logger`.

```java
@RestController
class CheckoutController {
  private final CheckoutService checkout;
  @PostMapping("/checkout") Map<String,Object> checkout(@Valid CheckoutRequest req) {
    return checkout.handle(req);
  }
}
```

---

### 2) O — Open/Closed

**Violations**

* `MegaController.checkout` uses a big `if/else` on `paymentType` to pick a processor.

**Refactor plan**

* Use strategy + Spring DI to register processors by key.

```java
public interface PaymentProcessor { String key(); void pay(String userId, double amount); }

@Component class StripeProcessor implements PaymentProcessor { public String key(){return "stripe";} ... }

@Service
class PaymentRouter {
  private final Map<String, PaymentProcessor> byKey;
  PaymentRouter(List<PaymentProcessor> processors) {
    this.byKey = processors.stream().collect(Collectors.toMap(PaymentProcessor::key, p->p));
  }
  void pay(String key, String userId, double amount) { byKey.get(key).pay(userId, amount); }
}
```

Adding a new processor no longer changes controller code.

---

### 3) L — Liskov Substitution

**Violations**

* `ReadOnlyUserRepository` extends `BaseUserRepository` but throws on `save`.
* `FreeTrialPaymentProcessor` implements `PaymentOperations` but throws on `processPayment`/`refund`.

**Refactor plan**

* Split hierarchies so contracts match behavior.

  * Replace `BaseUserRepository` inheritance with interfaces: `UserReader`, `UserWriter`.
  * For “free trial” use a **different** interface (e.g., `EntitlementActivator`) or implement `PaymentProcessor` in a way that still respects the contract (e.g., \$0 charge) — but **don’t** throw for expected methods.

```java
interface UserReader { User getById(String id); }
interface UserWriter { void save(User u); }
// Implement ReadOnlyUserRepository implements UserReader only
```

---

### 4) I — Interface Segregation

**Violations**

* `PaymentOperations` forces unrelated methods: `generateMonthlyReport`, `backupDatabase`, `sendMarketingEmails` on *all* processors.

**Refactor plan**

* Split into small, focused interfaces:

```java
interface PaymentProcessor { void pay(String userId, double amount); }
interface Refunds { void refund(String paymentId); }
interface Reporting { void writeMonthlyReport(Path out); }
interface Marketing { void sendCampaign(); }
```

Classes implement only what they actually provide.

---

### 5) D — Dependency Inversion

**Violations**

* Controller `new`’s concretes (`JdbcUserRepository`, `EmailSender`, `StripePaymentProcessor`).
* Uses static global state `StaticUtil.GLOBAL_DISCOUNT_PERCENT` and `StaticUtil.log`.

**Refactor plan**

* Depend on **abstractions** and inject via constructor.
* Replace static global config with configuration properties.
* Replace `DriverManager` and hand-rolled JDBC in controllers with repositories managed by Spring.

```java
@Service
class CheckoutService {
  private final PaymentRouter payments;
  private final UserWriter users;
  private final EmailService email;
  CheckoutService(PaymentRouter payments, UserWriter users, EmailService email) { ... }
}
```

---

## Detailed Refactor Checklist (one pass per principle)

1. **SRP Pass**

   * [ ] Create `domain` package with `CheckoutRequest` and domain services.
   * [ ] Extract `PricingService` (applies discounts) and `CheckoutService` (orchestrates).
   * [ ] Move DB calls out of controller into `UserRepository` interface + `JdbcUserRepository` implementation.
   * [ ] Introduce `EmailService` wrapper around `EmailSender`.
   * [ ] Replace `StaticUtil.log` with `Logger` (`private static final Logger log = LoggerFactory.getLogger(...);`).

2. **OCP Pass**

   * [ ] Introduce `PaymentProcessor` interface (only `pay`).
   * [ ] Implement `StripeProcessor`, `FreeTrialProcessor` (if needed).
   * [ ] Add `PaymentRouter` that maps `paymentType` → bean.
   * [ ] Controller delegates to `PaymentRouter`.

3. **LSP Pass**

   * [ ] Replace `BaseUserRepository` with `UserReader`/`UserWriter`.
   * [ ] `ReadOnlyUserRepository` implements `UserReader` only.
   * [ ] Ensure `FreeTrialProcessor` doesn’t throw for `pay`; either implement “no-charge” path or use a separate flow.

4. **ISP Pass**

   * [ ] Split `PaymentOperations` into `PaymentProcessor`, `Refunds`, `Reporting`, `Marketing`.
   * [ ] Remove reporting/backup/marketing from payment classes; create `ReportingService`, `BackupService`, `MarketingService`.

5. **DIP Pass**

   * [ ] Convert all `new` in controller to constructor-injected interfaces.
   * [ ] Introduce `@ConfigurationProperties` for discounts instead of `StaticUtil.GLOBAL_DISCOUNT_PERCENT`.
   * [ ] Replace raw SQL string concatenation with `JdbcTemplate` or JPA (still fine to keep H2).

6. **Cleanup**

   * [ ] Add `@Validated` + request DTOs.
   * [ ] Add tests around `CheckoutService` and processors.
   * [ ] Remove `StaticUtil` entirely.

---

## Example Target Structure (after refactor)

```
src/main/java/com/example/chaos/
├─ app/
│  ├─ CheckoutController.java
│  └─ dto/CheckoutRequest.java
├─ domain/
│  ├─ PricingService.java
│  ├─ CheckoutService.java
│  ├─ payments/
│  │  ├─ PaymentProcessor.java
│  │  ├─ PaymentRouter.java
│  │  ├─ StripeProcessor.java
│  │  └─ FreeTrialProcessor.java
│  └─ users/
│     ├─ UserReader.java
│     ├─ UserWriter.java
│     └─ UserService.java
├─ infra/
│  ├─ JdbcUserRepository.java  // implements UserReader, UserWriter
│  ├─ EmailService.java
│  └─ ReportingService.java
└─ ChaosApplication.java
```

---

## Suggested Commits (learning path)

1. `feat: extract PricingService and CheckoutService (SRP)`
2. `feat: introduce PaymentProcessor & PaymentRouter; remove if/else (OCP)`
3. `refactor: split user repository interfaces; remove LSP violations`
4. `refactor: split PaymentOperations into small interfaces (ISP)`
5. `refactor: constructor DI; remove StaticUtil & new(); add properties (DIP)`
6. `chore: tests + cleanup`

---

## Known Anti-Patterns Left for Practice

* Raw JDBC and hand-made SQL concatenation (SQL injection risk).
* Controller returns a generic `Map`; switch to response DTOs.
* No validation or error handling strategy.

---

## License

Public domain / do-whatever for educational use. Attribution appreciated.
