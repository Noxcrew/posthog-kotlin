package dev.kezz.posthog

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

/**
 * A store of PostHog properties.
 *
 * Instances can be constructed using the companion methods.
 *
 * @since 1.0
 */
public class PostHogProperties private constructor(
    internal val properties: Map<String, JsonElement>,
) {
    public companion object {
        /**
         * A properties instance with no data.
         *
         * @since 1.0
         */
        public val EMPTY: PostHogProperties = PostHogProperties(emptyMap())

        /**
         * Creates a properties instance from a map.
         *
         * @param map the map
         * @return the properties instance
         * @since 1.0
         */
        public fun fromMap(map: Map<String, String>): PostHogProperties {
            return PostHogProperties(map.mapValues { (_, value) -> JsonPrimitive(value) })
        }
    }

    /**
     * Checks if this properties instance contains no data.
     *
     * @return `true` if there is no data in this instance
     * @since 1.0
     */
    public fun isEmpty(): Boolean = properties.isEmpty()
}
