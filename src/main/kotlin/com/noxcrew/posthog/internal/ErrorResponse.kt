package com.noxcrew.posthog.internal

import kotlinx.serialization.Serializable

/** An error response from PostHog. */
@Serializable
internal data class ErrorResponse(
    public val type: String,
    public val code: String,
    public val detail: String,
    public val attr: String? = null,
)
