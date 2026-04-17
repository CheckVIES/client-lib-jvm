package com.checkvies.client

import com.checkvies.client.generated.model.CheckRequestCreatedDto
import com.checkvies.client.generated.model.RequestStateDto
import com.checkvies.client.generated.model.StartCheckRequestDto
import com.checkvies.client.generated.model.VatCheckRequestWithResultDto
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.ContextualSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID

object UUIDSerializer : KSerializer<UUID> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("UUID", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: UUID) = encoder.encodeString(value.toString())
    override fun deserialize(decoder: Decoder): UUID = UUID.fromString(decoder.decodeString())
}

class CheckViesClientTest {

    private val json = Json {
        ignoreUnknownKeys = true
        serializersModule = SerializersModule {
            contextual(UUID::class, UUIDSerializer)
        }
    }

    @Test
    fun `getCheckDetails success`() = runBlocking {
        val requestId = UUID.randomUUID()
        val mockEngine = MockEngine { request ->
            assertEquals("/api/check/details/$requestId", request.url.encodedPath)
            assertEquals("test-api-key", request.headers["X-API-Key"])
            
            respond(
                content = json.encodeToString(
                    VatCheckRequestWithResultDto(
                        requestId = requestId,
                        state = RequestStateDto.Success,
                        number = "12345678",
                        isoAlpha2 = "DE",
                        requestDate = "2024-01-01T00:00:00Z",
                        changeDate = "2024-01-01T00:00:00Z"
                    )
                ),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val client = CheckViesClient(
            CheckViesConfig(
                apiKey = "test-api-key",
                httpClientEngine = mockEngine
            )
        )

        val result = client.getCheckDetails(requestId)
        assertEquals(requestId, result.requestId)
        assertEquals(RequestStateDto.Success, result.state)
    }

    @Test
    fun `startCheck success`() = runBlocking {
        val mockEngine = MockEngine { request ->
            assertEquals("/api/check/start", request.url.encodedPath)
            respond(
                content = json.encodeToString(CheckRequestCreatedDto(id = UUID.randomUUID())),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val client = CheckViesClient(
            CheckViesConfig(apiKey = "test-api-key", httpClientEngine = mockEngine)
        )

        val result = client.startCheck(StartCheckRequestDto(number = "12345678", isoAlpha2 = "DE"))
        // result.id is UUID, no length property
        assert(result.id.toString().isNotEmpty())
    }

    @Test
    fun `api returns error`() = runBlocking {
        val mockEngine = MockEngine { _ ->
            respond(
                content = """{"error_code": "INVALID_VAT", "error_text": "The VAT number is invalid"}""",
                status = HttpStatusCode.UnprocessableEntity,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val client = CheckViesClient(
            CheckViesConfig(apiKey = "test-api-key", httpClientEngine = mockEngine)
        )

        val exception = assertThrows<CheckViesApiException> {
            runBlocking {
                client.getCheckDetails(UUID.randomUUID())
            }
        }

        assertEquals("INVALID_VAT", exception.errorCode)
        assertEquals(422, exception.statusCode)
    }
}
