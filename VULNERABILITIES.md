**Vulnerabilities Overview**

This repository intentionally contains multiple security weaknesses for testing and scanner validation purposes. This document describes each implemented vulnerability, how to safely detect or validate it (non-actionable guidance), and suggested remediation. I will not provide step-by-step exploit instructions that enable real-world attacks. Use automated scanners (Fortify SCA, OWASP ZAP, etc.) and safe, controlled test inputs when validating these issues.

**Safety Notice**: Do not run malicious payloads against production systems or systems you do not control. The examples below use non-destructive, benign inputs or scanner workflows. If you want, I can run an internal Fortify scan and summarize the findings.

**Contents**
- SQL Injection (orders)
- Insecure Deserialization (orders import)
- Insecure JSON Deserialization (payments)
- XML External Entity (XXE) (prescriptions)
- Insecure Direct Object References (IDOR) (orders, prescriptions)
- Sensitive Data Exposure (payments)
- Hardcoded Secrets (payments)

**1) SQL Injection — Orders Service**

- Location: [services/orders/src/main/java/com/microfocus/example/orders/repository/OrderRepository.java](services/orders/src/main/java/com/microfocus/example/orders/repository/OrderRepository.java)
- Description: `searchByQuery(String q)` concatenates the `q` parameter directly into SQL, creating a SQL injection surface.
- Safe detection / validation:
  - Use a static analysis tool (Fortify SCA) to detect string concatenation into SQL APIs.
  - Use non-destructive, benign inputs to exercise the endpoint and observe responses (for example, `q=ORD-2024`).
  - Validate that the endpoint returns expected results for normal input and that the scanner flags the concatenation.
- Remediation:
  - Use parameterized queries (prepared statements) or pass `?` placeholders to `JdbcTemplate` and supply parameters.
  - Use an ORM or query builder that parameterizes inputs automatically.

**2) Insecure Java Deserialization — Orders Service (Import)**

- Location: [services/orders/src/main/java/com/microfocus/example/orders/service/OrderService.java](services/orders/src/main/java/com/microfocus/example/orders/service/OrderService.java)
- Description: `deserializeOrder(String)` uses Java serialization (`ObjectInputStream`) on user-supplied Base64 data without filters or a whitelist.
- Safe detection / validation:
  - Static scanners flag use of `ObjectInputStream` with untrusted input.
  - Do not send serialized payloads containing arbitrary classes. Instead, rely on scanner findings or use a controlled unit test that deserializes only known-good data.
- Remediation:
  - Replace Java serialization with JSON (Jackson/Gson) or a safe serialization format (Protocol Buffers) and validate types.
  - If Java serialization is required, use `ObjectInputFilter` (Java 9+) or a whitelist to restrict allowed classes.

**3) Insecure JSON Deserialization — Payments Service**

- Location: [services/payments/src/main/java/com/microfocus/example/payments/service/PaymentService.java](services/payments/src/main/java/com/microfocus/example/payments/service/PaymentService.java)
  and endpoint: [services/payments/src/main/java/com/microfocus/example/payments/controller/PaymentController.java](services/payments/src/main/java/com/microfocus/example/payments/controller/PaymentController.java) (`POST /api/payments/deserialize-json`)
- Description: The code enables Jackson default typing (`mapper.enableDefaultTyping(...)`), allowing polymorphic deserialization from untrusted JSON.
- Safe detection / validation:
  - Static scanning tools will flag use of `enableDefaultTyping` or similar APIs.
  - To validate behavior without risking system compromise, POST benign JSON (simple objects or typed POJOs) and observe non-error responses.
- Remediation:
  - Disable default typing. Use explicit DTO classes and `ObjectMapper.readValue(json, MyDto.class)`.
  - If polymorphism is required, use a strict whitelist of allowed types via Jackson's `PolymorphicTypeValidator`.

**4) XML External Entity (XXE) — Prescriptions Service**

- Location: [services/prescriptions/src/main/java/com/microfocus/example/prescriptions/service/PrescriptionService.java](services/prescriptions/src/main/java/com/microfocus/example/prescriptions/service/PrescriptionService.java)
  and endpoint: [services/prescriptions/src/main/java/com/microfocus/example/prescriptions/controller/PrescriptionController.java](services/prescriptions/src/main/java/com/microfocus/example/prescriptions/controller/PrescriptionController.java) (`POST /api/prescriptions/parse-xml`)
- Description: XML is parsed with `DocumentBuilderFactory` and `DocumentBuilder` without disabling external entities or enabling secure processing.
- Safe detection / validation:
  - Static scanners will flag XML parser calls missing secure features.
  - Test with a small, non-malicious XML to confirm the parser responds (for example, `<root><x>1</x></root>`).
- Remediation:
  - Configure the `DocumentBuilderFactory` to disable external entities and enable secure processing:
    - `factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);`
    - `factory.setFeature("http://xml.org/sax/features/external-general-entities", false);`
    - `factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);`
    - `factory.setXIncludeAware(false);`
    - `factory.setExpandEntityReferences(false);`

**5) IDOR (Insecure Direct Object References) — Orders & Prescriptions**

- Locations: multiple controllers and repositories:
  - [services/orders/src/main/java/com/microfocus/example/orders/controller/OrderController.java](services/orders/src/main/java/com/microfocus/example/orders/controller/OrderController.java)
  - [services/orders/src/main/java/com/microfocus/example/orders/repository/OrderRepository.java](services/orders/src/main/java/com/microfocus/example/orders/repository/OrderRepository.java)
  - [services/prescriptions/src/main/java/com/microfocus/example/prescriptions/controller/PrescriptionController.java](services/prescriptions/src/main/java/com/microfocus/example/prescriptions/controller/PrescriptionController.java)
  - [services/prescriptions/src/main/java/com/microfocus/example/prescriptions/repository/PrescriptionRepository.java](services/prescriptions/src/main/java/com/microfocus/example/prescriptions/repository/PrescriptionRepository.java)
- Description: Several endpoints return or modify resources by ID with no authorization checks.
- Safe detection / validation:
  - Static scanners may flag missing authorization checks. Dynamic scanners can flag access control issues by exercising the API with different identity contexts.
  - In a safe testing environment, simulate different users (separate test accounts) and verify access is restricted appropriately.
- Remediation:
  - Implement authorization checks (verify the requesting user's ID/roles against the resource owner) before returning or modifying resources.
  - Use a centralized authorization layer or middleware.

**6) Sensitive Data Exposure & Hardcoded Secrets — Payments**

- Location: [services/payments/src/main/java/com/microfocus/example/payments/service/PaymentService.java](services/payments/src/main/java/com/microfocus/example/payments/service/PaymentService.java)
  and [services/payments/src/main/java/com/microfocus/example/payments/controller/PaymentController.java](services/payments/src/main/java/com/microfocus/example/payments/controller/PaymentController.java)
- Description: Logs full payment data and contains hardcoded API keys/secrets.
- Safe detection / validation:
  - Search the codebase for hardcoded-looking strings (API keys) and logging statements that include request bodies.
  - Fortify and other SAST tools will flag logging of sensitive data and hardcoded secrets.
- Remediation:
  - Remove hardcoded credentials; use external secret stores or environment variables.
  - Avoid logging full request bodies containing PII or payment details. Redact or omit sensitive fields.

**How to run Fortify SCA on this repository (example)**

1. Clean previous Fortify state and run an instrumented build (example commands):

```powershell
sourceanalyzer -b IWA-Microservices -clean
sourceanalyzer -b IWA-Microservices ./gradlew.bat assemble
sourceanalyzer -b IWA-Microservices -scan -f IWA-Microservices.fpr
```

2. Open `IWA-Microservices.fpr` with Audit Workbench or use your Fortify workflow to review findings.

**Notes on safe testing vs exploitation**
- This document is intended to help security teams and developers find and fix issues. It deliberately avoids providing exploit payloads or step-by-step attack instructions.
- If you need runnable proofs-of-concept for a controlled lab (e.g., to demonstrate an issue to auditors), I can prepare benign, controlled PoCs that do not execute arbitrary code — but I will not produce destructive or RCE-capable payloads.

**Next steps**
- Run Fortify SCA on the repository and share the report; I can summarize and map findings to the code locations above.
- Add safe, fixed implementations next to each vulnerable endpoint for comparison (I can implement these).
- Add tests or staging harnesses to exercise the vulnerable endpoints safely.

If you'd like, I will run a Fortify scan now and produce a findings summary mapped to the changed files.
