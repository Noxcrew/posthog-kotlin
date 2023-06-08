package dev.kezz.posthog.internal

import dev.kezz.posthog.PostHog
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.channels.onSuccess
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.onTimeout
import kotlinx.coroutines.selects.whileSelect
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
internal class EventQueue(
    private val postHog: PostHog,
) : Callback {
    private companion object {
        private val JSON = "application/json".toMediaType()
    }

    private val cleanHostname = postHog.hostname.removeSuffix("/")

    private val queueJob = SupervisorJob(postHog.parentJob)
    private val scope = CoroutineScope(queueJob + CoroutineName("posthog-kotlin-event-queue") + Dispatchers.IO)

    private val eventChannel = Channel<PostHogEvent>(Channel.UNLIMITED)

    init {
        scope.launch {
            var buffer = mutableListOf<PostHogEvent>()

            /** Processes the events, clearing the queue. */
            fun processAndClear() {
                process(buffer)
                buffer = mutableListOf()
            }

            whileSelect {
                if (buffer.isNotEmpty() && postHog.flushInterval.isPositive() && postHog.flushInterval.isFinite()) {
                    onTimeout(postHog.flushInterval) {
                        processAndClear()
                        true
                    }
                }

                eventChannel.onReceiveCatching { result ->
                    result
                        .onSuccess { event ->
                            buffer += event

                            if (buffer.size > postHog.flushSize) {
                                processAndClear()
                            }
                        }
                        .onFailure {
                            if (buffer.isNotEmpty()) {
                                processAndClear()
                            }
                        }
                        .isSuccess
                }
            }
        }
    }

    /** Adds an event to the queue. */
    public fun addEvent(event: PostHogEvent) {
        eventChannel.trySend(event)
    }

    /** Checks if this queue is active. */
    public val isActive: Boolean
        get() = queueJob.isActive

    /** Closes this event queue. */
    public suspend fun close() {
        queueJob.cancelAndJoin()
    }

    /** Processes some events. */
    private fun process(events: List<PostHogEvent>) {
        /** Creates a call. */
        fun createCall(
            url: String,
            builder: JsonObjectBuilder.() -> Unit,
        ): Call {
            return postHog.okHttpClient.newCall(
                Request.Builder()
                    .header("Authorization", "Bearer ${postHog.apiKey}")
                    .post(Json.encodeToString(buildJsonObject(builder)).toRequestBody(JSON))
                    .url(url)
                    .build(),
            )
        }

        // We can use the batch endpoint if there are multiple events.
        val call =
            when (events.size) {
                0 -> return
                1 ->
                    createCall("$cleanHostname/capture") {
                        val event = events.single()

                        put("event", event.event)
                        put("distinct_id", event.userId)
                        put("timestamp", event.timestamp.toString())
                        put("properties", JsonObject(event.properties))
                    }
                else ->
                    createCall("$cleanHostname/batch") {
                        putJsonArray("batch") {
                            for (event in events) {
                                addJsonObject {
                                    put("event", event.event)
                                    put("distinct_id", event.userId)
                                    put("timestamp", event.timestamp.toString())
                                    put("properties", JsonObject(event.properties))
                                }
                            }
                        }
                    }
            }

        // Now post the call.
        call.enqueue(this)
    }

    override fun onFailure(
        call: Call,
        e: IOException,
    ) {
        postHog.logger.error("An unknown error occurred whilst posting some events! The events will be discarded.", e)
    }

    override fun onResponse(
        call: Call,
        response: Response,
    ) {
        response.use {
            if (response.code == 400 || response.code == 401) {
                val body = response.body?.string()

                if (body == null) {
                    postHog.logger.error("An unknown error occurred whilst posting some events! The events will be discarded.")
                } else {
                    val error = Json.decodeFromString<ErrorResponse>(body)
                    postHog.logger.error("An error occurred whilst posting some events (${error.type}: ${error.code}). ${error.detail}")
                }
            }
        }
    }
}
