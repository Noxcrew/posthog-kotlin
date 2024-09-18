# posthog-kotlin
<img alt="GitHub License" src="https://img.shields.io/github/license/noxcrew/posthog-kotlin"> <img alt="GitHub Actions Workflow Status" src="https://img.shields.io/github/actions/workflow/status/noxcrew/posthog-kotlin/build.yml"> <img alt="GitHub Release" src="https://img.shields.io/github/v/release/noxcrew/posthog-kotlin">

posthog-kotlin is a Kotlin library to interact with the PostHog API.

## Features
posthog-kotlin currently supports the following features:

| Feature | Implemented   |
| --- |--- |
| Single/Batch events | ✅             |
| Identify | ✅             |

## Usage
### Dependency
posthog-kotlin can be found on Noxcrew's public Maven repository and added to a Gradle project as follows:

```kotlin
repositories {
    maven {
        name = "noxcrew"
        url = uri("https://maven.noxcrew.com/public")
    }
}

dependencies {
    implementation("com.noxcrew.posthog-kotlin:posthog-kotlin:VERSION")
}
```

### Example
Some simple examples of how to use the library can be seen below.
For further examples, see the test files.

```kotlin
// Create your PostHog instance (do this once, centrally).
val postHog = PostHog(
    hostname = "https://eu.i.posthog.com",
    apiKey = "<your_api_key_here>",
    // See docs for more settings you can change here.
    ...
)

// PostHog events have properties that can be constructed using the PostHogProperties class.
val properties = PostHogProperties.fromMap(
    mapOf(
        "some_key" to "my_value",
        "data_here" to "cool_setting",
    )
)

// Events can be added to the queue using your PostHog instance from earlier.
postHog.capture(
    userId = "kezz",
    eventName = "made a cool library",
    properties = properties,
)

// You can also identify users.
postHog.identify(
    userId = "kezz",
    properties = properties,
)

// When you are finished with your application, close the PostHog instance to flush the queue.
postHog.close()
```

## Documentation
Documentation for how to use the library can be found on the library's entrypoint, the `PostHog` interface.
Javadocs/Dokka docs are also provided.

## Issues and Feature Requests
Should you encounter any issues while using posthog-kotlin, please create an [issue,](https://github.com/Noxcrew/posthog-kotlin/issues/new) and we will get back to you as soon as possible.
For questions and feedback, please use the  [discussions tab](https://github.com/Noxcrew/posthog-kotlin/discussions) or the [#api-chat](https://discord.com/channels/707193125478596668/1134515300742733985) channel in the [MCC Discord](https://discord.gg/mcc).
