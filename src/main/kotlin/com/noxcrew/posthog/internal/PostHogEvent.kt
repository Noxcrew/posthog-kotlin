package com.noxcrew.posthog.internal

import kotlinx.serialization.json.JsonElement
import java.time.Instant

/** An event to be submitted to PostHog. */
internal data class PostHogEvent(
    public val event: String,
    public val userId: String,
    public val properties: Map<String, JsonElement>,
    public val timestamp: Instant,
)
