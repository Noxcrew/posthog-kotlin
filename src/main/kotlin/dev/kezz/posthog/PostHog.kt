package dev.kezz.posthog

import dev.kezz.posthog.internal.EventQueue
import dev.kezz.posthog.internal.PostHogEvent
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.JsonObject
import okhttp3.OkHttpClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Entrypoint for the posthog-kotlin API.
 *
 * @property hostname the hostname of the PostHog instance
 * @property apiKey the API key used to connect to PostHog
 * @property parentJob the parent job to use for scope creation
 * @property flushSize the number of events to store until flushing, cannot be negative
 * @property flushInterval the interval between automatic flushing
 * @property okHttpClient an OkHttp client to use for sending API requests
 * @since 1.0
 */
public class PostHog(
    internal val hostname: String,
    internal val apiKey: String,
    internal val parentJob: Job = SupervisorJob(),
    internal val flushSize: Int = 20,
    internal val flushInterval: Duration = 30.seconds,
    internal val okHttpClient: OkHttpClient = OkHttpClient(),
) {
    init {
        require(flushSize > 0) { "flushSize cannot be negative, was '$flushSize'" }
    }

    private val eventQueue = EventQueue(this)

    internal val logger: Logger = LoggerFactory.getLogger(javaClass)

    /**
     * Identifies a user.
     *
     * @param userId a unique ID representing this user
     * @param properties the properties to set
     * @param propertiesSetOnce properties that will only be set once
     * @since 1.0
     */
    public fun identify(
        userId: String,
        properties: PostHogProperties = PostHogProperties.EMPTY,
        propertiesSetOnce: PostHogProperties = PostHogProperties.EMPTY,
    ) {
        check(eventQueue.isActive) { "This PostHog instance is already closed" }
        require(userId.isNotBlank()) { "userId cannot be blank" }
        eventQueue.addEvent(
            PostHogEvent(
                event = "\$identify",
                properties =
                    JsonObject(
                        buildMap(2) {
                            if (!properties.isEmpty()) {
                                put("\$set", JsonObject(properties.properties))
                            }

                            if (!propertiesSetOnce.isEmpty()) {
                                put("\$set_once", JsonObject(properties.properties))
                            }
                        },
                    ),
                timestamp = Instant.now(),
                userId = userId,
            ),
        )
    }

    /**
     * Captures an event.
     *
     * @param userId a unique ID representing this user
     * @param eventName the name of the event
     * @param properties a map of properties for this event
     * @since 1.0
     */
    public fun capture(
        userId: String,
        eventName: String,
        properties: PostHogProperties = PostHogProperties.EMPTY,
    ) {
        check(eventQueue.isActive) { "This PostHog instance is already closed" }
        require(userId.isNotBlank()) { "userId cannot be blank" }
        require(eventName.isNotBlank()) { "eventName cannot be blank" }
        eventQueue.addEvent(
            PostHogEvent(
                event = eventName,
                properties = properties.properties,
                timestamp = Instant.now(),
                userId = userId,
            ),
        )
    }

    /**
     * Closes this PostHog instance, flushing any remaining events.
     *
     * @throws IllegalStateException if this PostHog instance is already closed
     * @since 1.0
     */
    public suspend fun close() {
        check(eventQueue.isActive) { "This PostHog instance is already closed" }
        eventQueue.close()
    }
}
