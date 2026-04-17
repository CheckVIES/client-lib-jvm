package com.checkvies.client

import com.checkvies.client.generated.api.ApiVatCheckApi
import com.checkvies.client.generated.infrastructure.HttpResponse
import com.checkvies.client.generated.model.CheckRequestCreatedDto
import com.checkvies.client.generated.model.RequestListStateCheckDto
import com.checkvies.client.generated.model.RequestListStateDto
import com.checkvies.client.generated.model.StartCheckRequestDto
import com.checkvies.client.generated.model.VatCheckRequestWithResultDto
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModule
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.client.request.url
import io.ktor.util.reflect.typeInfo
import java.util.UUID

private object UUIDSerializer : KSerializer<UUID> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("UUID", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: UUID) = encoder.encodeString(value.toString())
    override fun deserialize(decoder: Decoder): UUID = UUID.fromString(decoder.decodeString())
}

/**
 * Main client for interacting with the CheckVIES API.
 */
class CheckViesClient(private val config: CheckViesConfig) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        serializersModule = SerializersModule {
            contextual(UUID::class, UUIDSerializer)
        }
    }

    private val api: ApiVatCheckApi = ApiVatCheckApi(
        baseUrl = config.baseUrl,
        httpClientEngine = config.httpClientEngine,
        httpClientConfig = { ktorConfig ->
            ktorConfig.install(ContentNegotiation) {
                json(json)
            }
            ktorConfig.install(Logging) {
                level = config.logLevel
                logger = config.logger
            }
            // Add custom config if provided
            config.httpClientConfig?.invoke(ktorConfig)

            ktorConfig.defaultRequest {
                url(config.baseUrl)
            }
        }
    ).apply {
        setApiKey(config.apiKey)
    }

    /**
     * Get details of a single VAT number check request.
     *
     * @param id Key identifier (UUID)
     * @return [VatCheckRequestWithResultDto] containing request details and results.
     * @throws CheckViesApiException if the server returns an error.
     * @throws CheckViesClientException if a client-side error occurs.
     */
    suspend fun getCheckDetails(id: UUID): VatCheckRequestWithResultDto {
        return handleResponse { api.apiCheckDetailsIdGet(id) }
    }

    /**
     * Start a new VAT number check.
     *
     * @param request [StartCheckRequestDto] containing VAT number and optional parameters.
     * @return [CheckRequestCreatedDto] containing the new request ID.
     * @throws CheckViesApiException if the server returns an error.
     * @throws CheckViesClientException if a client-side error occurs.
     */
    suspend fun startCheck(request: StartCheckRequestDto): CheckRequestCreatedDto {
        return handleResponse { api.apiCheckStartPost(request) }
    }

    /**
     * Get the current states of a list of VAT number requests.
     *
     * @param requestList [RequestListStateCheckDto] containing a list of request IDs.
     * @return [RequestListStateDto] containing states for each request.
     * @throws CheckViesApiException if the server returns an error.
     * @throws CheckViesClientException if a client-side error occurs.
     */
    suspend fun getCheckStates(requestList: RequestListStateCheckDto): RequestListStateDto {
        return handleResponse { api.apiCheckStateListPost(requestList) }
    }

    private suspend fun <T : Any> handleResponse(call: suspend () -> HttpResponse<T>): T {
        return try {
            val response = call()
            if (response.success) {
                response.body()
            } else {
                val errorBody = response.typedBody<com.checkvies.client.generated.model.ErrorResponse>(typeInfo<com.checkvies.client.generated.model.ErrorResponse>())
                throw CheckViesApiException(
                    errorCode = errorBody.errorCode,
                    message = errorBody.errorText ?: "API returned error ${response.status}",
                    statusCode = response.status,
                    response = errorBody
                )
            }
        } catch (e: CheckViesException) {
            throw e
        } catch (e: Exception) {
            throw CheckViesClientException("Failed to execute API call", e)
        }
    }
}
