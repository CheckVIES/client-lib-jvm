package com.checkvies.client

import com.checkvies.client.generated.model.ErrorResponse

/**
 * Base exception for CheckVIES API errors.
 */
open class CheckViesException(
    val errorCode: String?,
    override val message: String?,
    val statusCode: Int? = null,
    override val cause: Throwable? = null
) : Exception(message, cause)

/**
 * Exception thrown when the API returns an error response (4xx or 5xx).
 */
class CheckViesApiException(
    errorCode: String,
    message: String?,
    statusCode: Int,
    val response: ErrorResponse? = null
) : CheckViesException(errorCode, message, statusCode)

/**
 * Exception thrown when there's a problem with serialization or network.
 */
class CheckViesClientException(
    message: String?,
    cause: Throwable? = null
) : CheckViesException(null, message, cause = cause)
