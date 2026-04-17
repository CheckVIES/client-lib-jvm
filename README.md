# CheckVIES API Client (Kotlin/JVM)

Production-ready Kotlin/JVM client library for the CheckVIES automated API.

## Features

- **Typed API Client**: Full coverage of CheckVIES API endpoints.
- **Modern Tech Stack**: Ktor Client, Coroutines, and kotlinx.serialization.
- **Strongly Typed**: Models generated from the official OpenAPI spec.
- **Configurable**: Easy customization of base URL, timeouts, logging, and more.
- **Production Ready**: Built with industry-standard best practices.

## Installation

Add the following to your `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.checkvies:client:<ver>")
    // Required Ktor engine (choose one, e.g. CIO)
    implementation("io.ktor:ktor-client-cio:<ktor-version>")
}
```

`ktor-client-core` does not include an HTTP engine, so you must add one Ktor client engine dependency in your application (for example `ktor-client-cio`, `ktor-client-okhttp`, `ktor-client-java`, etc.).

## Usage

### Creating a Client

```kotlin
val client = CheckViesClient(
    CheckViesConfig(
        apiKey = "your-api-key",
        logLevel = LogLevel.INFO // Optional: enable logging
    )
)
```

### Starting a VAT Check

```kotlin
val request = StartCheckRequestDto(
    number = "12345678",
    isoAlpha2 = "DE"
)

val response = client.startCheck(request)
println("Check started with ID: ${response.id}")
```

### Getting Check Details

```kotlin
val details = client.getCheckDetails(UUID.fromString("your-request-id"))
println("Status: ${details.state}")
if (details.state == RequestStateDto.Success) {
    println("Valid: ${details.result?.asVatCheckResultDetailsDto()?.valid}")
}
```

## Configuration

You can customize the underlying Ktor client by providing a `httpClientConfig` block in `CheckViesConfig`:

```kotlin
val client = CheckViesClient(
    CheckViesConfig(
        apiKey = "your-api-key",
        httpClientConfig = {
            install(HttpTimeout) {
                requestTimeoutMillis = 5000
            }
        }
    )
)
```

## Error Handling

The client throws `CheckViesApiException` for server-side errors and `CheckViesClientException` for client-side issues.

```kotlin
try {
    val details = client.getCheckDetails(id)
} catch (e: CheckViesApiException) {
    println("API Error: ${e.errorCode} - ${e.message}")
} catch (e: CheckViesClientException) {
    println("Client Error: ${e.message}")
}
```

## Building and Testing

### Build
```bash
./gradlew build
```

### Run Tests
```bash
./gradlew test
```

### Publish to Maven Local
```bash
./gradlew publishToMavenLocal
```

## License

Apache 2.0 License. See `LICENSE` for details.
