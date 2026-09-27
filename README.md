# Blog Functions

A Java 25 Azure Functions runtime 4.x scaffold with Spring Boot as the dependency-injection container. Native HTTP and batched Event Hub triggers share one Function App. Application ports and domain records have no Azure dependencies; Spring Cloud Function's Java worker SPI creates the Spring-managed adapters using constructor injection.

## Prerequisites

- JDK 25 (the build rejects older JDKs).
- Maven Wrapper (included, Maven 3.9.16); installed Maven must be at least 3.9.9.
- Azure Functions Core Tools 4.x with Java 25 support, and Azure CLI.
- Azurite running for `UseDevelopmentStorage=true`, or a development Azure Storage connection.
- An Event Hubs namespace, the `topic_one` entity, and a consumer group.

Java 25 is GA for Functions 4.x. Use a Java-25-capable Linux plan, such as Flex Consumption or Premium; **Linux Consumption supports Java only through 21**. Configure the existing Azure app's stack to Java 25—packaging alone does not change its runtime. [Azure runtime support](https://learn.microsoft.com/en-us/azure/azure-functions/functions-versions)

## Build and versions

```bash
./mvnw clean verify
./mvnw dependency:tree
```

Verified against Maven Central on 2026-09-28: Boot **4.0.8**, Cloud **2025.1.3**, BOM-managed Function **5.0.4**, Azure Java Library **3.3.0**, Azure Maven Plugin **1.42.0**. Boot manages Jackson 3 and JUnit 6. No Spring Cloud Stream, Event Hub client SDK, binder, MVC/WebFlux server, or Spring Cloud Azure is needed.

Sources: [Boot release](https://spring.io/blog/2026/08/20/spring-boot-4-0-8-available-now/), [Cloud compatibility](https://spring.io/blog/2026/08/20/spring-cloud-2025-1-3-has-been-released/), [Azure integration and packaging](https://docs.spring.io/spring-cloud-function/reference/adapters/azure-intro.html), [library metadata](https://repo.maven.apache.org/maven2/com/microsoft/azure/functions/azure-functions-java-library/maven-metadata.xml), [plugin metadata](https://repo.maven.apache.org/maven2/com/microsoft/azure/azure-functions-maven-plugin/maven-metadata.xml).

The build creates a plain jar and the deployable directory `target/azure-functions/fn-one/`, including both generated `function.json` files and `lib/`. The jar manifest identifies the Spring bootstrap class. Do not enable Boot's executable-jar repackaging. Override the packaging name with `-DfunctionAppName=your-app`.

## Local execution

Start Azurite in a separate terminal and leave it running. With Node.js/npm installed:

```bash
npx --yes azurite --location .azurite
```

Alternatively, use the VS Code Azurite extension's `Azurite: Start` command. The emulator supplies host storage on ports 10000–10002; it does not emulate Event Hubs. `UseDevelopmentStorage=true` requires a running emulator. [Local storage configuration](https://learn.microsoft.com/en-us/azure/azure-functions/functions-develop-local)

```bash
cp .env.example .env
# Edit .env: provide the Event Hub connection, and start Azurite.
./scripts/run-local.sh
```

On Windows, copy the same file and run `./scripts/run-local.ps1`. Scripts load literal `KEY=value` assignments, preserve `$Default`, accept surrounding single/double quotes, and fail if `.env` is missing. Shell expansion, multiline values, `export`, and inline comments are not supported. They create the minimal ignored `local.settings.json` from its example when needed. Neither script installs tools or provisions cloud resources. The launchers export `JAVA_HOME` for the Functions worker, resolving JDK 25 via `/usr/libexec/java_home` on macOS or Java on PATH elsewhere. An explicit `JAVA_HOME` in your environment or `.env` takes precedence and must select JDK 25.

The example selects the `local` Spring profile and disables HMAC for development. To exercise authentication, set `WEBHOOK_SIGNATURE_ENABLED=true` and a real local secret. Keep `.env` and `local.settings.json` out of Git.

### Known HTTP binding limitation

A live smoke test with Core Tools 4.15.1 starts the Java worker after `JAVA_HOME` is exported, but JSON requests fail before entering the HTTP adapter: the worker attempts to deserialize the body into `Optional<byte[]>` and reports `Expected BEGIN_ARRAY but was BEGIN_OBJECT`. The `dataType = "binary"` annotation does not force this runtime to deliver an `application/json` body as bytes.

The HTTP endpoint is therefore **not yet verified as operational on this runtime**, despite passing unit tests. Changing to a String binding would enable normal UTF-8 JSON, but would not guarantee access to the original bytes for every encoding/BOM case. The current byte binding is retained pending a decision on that original HMAC requirement; no authentication guarantee has been silently weakened.

### Startup troubleshooting

- `%JAVA_HOME%/bin/java` / “No such file or directory”: Core Tools did not receive `JAVA_HOME`. Use the updated launcher (`./scripts/run-local.sh`), or export `JAVA_HOME="$(/usr/libexec/java_home -v 25)"` on macOS before launching Core Tools manually.
- `Connection refused (127.0.0.1:10000)`: start Azurite as shown above, or set `AzureWebJobsStorage` to a valid development storage connection in `.env`.
- Event Hub connectivity errors after storage starts: configure a real `EVENT_HUB_CONNECTION`, entity, and consumer group. Azurite provides storage only.
- The Mockito agent warning and the intentionally logged HTTP failure in the unit tests do not indicate startup failure; check the test summary.

Launching `BlogFunctionsApplication` directly from VS Code creates only the Spring context. Start the Functions host through the script to expose the HTTP route and Event Hub trigger.

## HTTP invocation

```bash
curl -i -X POST http://localhost:7071/api/blogs/review \
  -H 'Content-Type: application/json' \
  -d '{
    "title": "My Blog",
    "content": "Content...",
    "author": "Author",
    "source": "website"
  }'
```

`submitBlogForReview` receives binary bytes, authenticates them, parses strict JSON with Jackson 3, validates the DTO, maps it to the domain, and invokes the application port. Unknown fields, duplicate keys, trailing JSON, null bodies, and invalid constraints return 400. Responses use `application/json`.

| Status | Meaning |
| --- | --- |
| 202 | Accepted by the initial logging service |
| 400 | Malformed JSON or validation failure |
| 401 | Missing, malformed, expired, or incorrect signature |
| 415 | Content-Type is not application/json |
| 500 | Unexpected internal error; details withheld |

Errors contain `code`, `message`, `timestamp`, and `fieldErrors`. The initial application services **log outcomes only**; acceptance does not mean durable storage, review scheduling, or publication. Add a durable outbound port before promising those behaviors.

### Signature protocol

Production defaults enable HMAC and timestamp checks, and startup fails if the secret is missing. The function uses anonymous Azure authorization because HMAC authenticates the caller. Use HTTPS and provision a high-entropy shared secret as `WEBHOOK_HMAC_SECRET`.

- `X-Signature`: `sha256=` followed by exactly 64 hexadecimal characters.
- `X-Timestamp`: Unix seconds in decimal. Future timestamps and ages above `WEBHOOK_MAX_AGE_SECONDS` (default 300) are rejected.
- Signed message: ASCII timestamp, a literal `.`, then the **exact request bytes**. Secret encoding is UTF-8. Use HMAC-SHA256 and send the digest as hex.
- With `WEBHOOK_TIMESTAMP_ENABLED=false`, sign just the body bytes and omit the timestamp.
- Header names are configurable and matched case-insensitively; comparison uses `MessageDigest.isEqual`.

For a signed call, create `payload.json` containing the request body, export your secret, then:

```bash
timestamp=$(date +%s)
signature=$(TIMESTAMP="$timestamp" python3 - <<'PY'
import hashlib, hmac, os
from pathlib import Path
message = os.environ['TIMESTAMP'].encode('ascii') + b'.' + Path('payload.json').read_bytes()
print(hmac.new(os.environ['WEBHOOK_HMAC_SECRET'].encode('utf-8'), message, hashlib.sha256).hexdigest())
PY
)
curl -i http://localhost:7071/api/blogs/review \
  -H 'Content-Type: application/json' \
  -H "X-Timestamp: $timestamp" -H "X-Signature: sha256=$signature" \
  --data-binary @payload.json
```

Timestamp checks limit replay age; they do not deduplicate requests within the window. Add durable idempotency if the eventual posting workflow needs it. Payloads, signatures, secrets, and internal exception details are excluded from normal application logs.

## Event Hub

`topic_one` is an **Azure Event Hub entity**, not a Kafka or Spring Cloud Stream topic.

| Host setting | Purpose |
| --- | --- |
| `EVENT_HUB_NAME` | Entity name; use `topic_one` |
| `EVENT_HUB_CONSUMER_GROUP` | Existing consumer group, locally `$Default`; use a dedicated group per deployed consumer |
| `EVENT_HUB_CONNECTION` | Local namespace connection string with Listen access |
| `AzureWebJobsStorage` | Host storage used for checkpoints and coordination |

`topicOneEventProcessor` accepts batches of JSON objects, for example `{"id":"event-123","data":{"example":true}}`. `id` is optional and must be a string when supplied. Extra event fields are permitted and ignored by this initial model. Each valid object reaches `ProcessTopicOneEventUseCase`. Logs include batch size, invocation ID, and outcomes, never the event body.

Malformed events are deliberately skipped with a warning; this initial policy drops those events. Processing failures propagate to the host, which retries the batch up to five times at ten-second intervals. Delivery is at least once: earlier events in a failed batch can repeat. After retries are exhausted the host can advance past the failed batch. Before durable business processing, implement idempotency and a quarantine/recovery policy appropriate to the application. Azure owns partition assignment, polling, checkpoints, lifecycle, and scaling. [Retry and checkpoint behavior](https://learn.microsoft.com/en-us/azure/azure-functions/functions-reliable-event-processing)

To move to managed identity, remove the exact `EVENT_HUB_CONNECTION` connection-string setting and configure `EVENT_HUB_CONNECTION__fullyQualifiedNamespace=<namespace>.servicebus.windows.net`, plus `EVENT_HUB_CONNECTION__credential=managedidentity` in Azure. Grant the identity **Azure Event Hubs Data Receiver** at the appropriate scope. Host storage needs its own connection/identity permissions. No domain/application changes or Spring Cloud Azure dependency are required. [Identity connections](https://learn.microsoft.com/en-us/azure/azure-functions/manage-connections)

## Configuration model

| File or setting | Responsibility |
| --- | --- |
| `application.yaml` | Spring/application defaults; safe production authentication defaults |
| `application-local.yaml` | Local-only Spring overrides |
| `.env` | Ignored developer environment and secrets, loaded by scripts |
| Azure Function App settings | Deployed environment configuration and secrets; prefer Key Vault references/managed identity as appropriate |
| `host.json` | Azure host and extension bundle configuration |
| `local.settings.json` | Minimal Core Tools settings; generated locally, ignored |

`%EVENT_HUB_NAME%`, `%EVENT_HUB_CONSUMER_GROUP%`, and the `EVENT_HUB_CONNECTION` prefix are resolved by the **Functions host**, before Spring runs. Set them in the script-exported environment locally and Function App settings in Azure. Spring YAML cannot supply trigger binding values, so these settings are intentionally not duplicated there.

For Azure, configure `FUNCTIONS_WORKER_RUNTIME=java`, `FUNCTIONS_EXTENSION_VERSION=~4`, host storage, the three Event Hub settings, and `WEBHOOK_HMAC_SECRET`. Keep the `local` profile and local signature-disable flags out of production. The existing GitHub workflow builds with Java 25 and deploys the generated Functions package to `fn-one` using its configured publish-profile secret; infrastructure and app settings must already exist.

## Tests

Focused JUnit 6 tests cover DTO validation, HMAC byte integrity, malformed signatures, authenticated timestamps and expiry boundaries, HTTP status/error handling, application port dispatch, and Event Hub delegation/retry propagation. Pure unit tests construct components directly without a Spring context. A separate bootstrap integration test verifies configuration binding and Spring construction of both entry points. Maven runs headlessly via `.mvn/jvm.config` for compatibility with automated macOS builds.
