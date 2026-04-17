package com.checkvies.client

import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.DEFAULT

/**
 * Configuration for [CheckViesClient].
 *
 * @property apiKey The API key for authentication.
 * @property baseUrl The base URL of the CheckVIES API.
 * @property httpClientEngine Optional Ktor [HttpClientEngine] for custom networking.
 * @property httpClientConfig Optional configuration block for the internal Ktor [io.ktor.client.HttpClient].
 * @property logLevel The logging level for HTTP requests and responses.
 * @property logger The logger implementation for Ktor logging.
 */
data class CheckViesConfig(
    val apiKey: String,
    val baseUrl: String = "https://api.checkvies.com",
    val httpClientEngine: HttpClientEngine? = null,
    val httpClientConfig: (HttpClientConfig<*>.() -> Unit)? = null,
    val logLevel: LogLevel = LogLevel.NONE,
    val logger: Logger = Logger.DEFAULT
)
