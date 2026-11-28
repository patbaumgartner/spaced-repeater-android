# Third-party notices

This inventory covers the resolved **release runtime** and bundled resources,
not build plugins or test-only dependencies. It was checked against
`./gradlew :app:dependencies --configuration releaseRuntimeClasspath`, the
published dependency license metadata, and the bundled font notice. Versions
below are the direct declarations or resolved runtime versions at the time of
the inventory; update this file when those dependencies change.

## Apache License 2.0 components

The following projects declare the Apache License, Version 2.0. The license
text is reproduced in [LICENSE](LICENSE); upstream source and license links are
provided for attribution. These notices do not relicense the components.

| Component | Version(s) | Upstream / license evidence |
|-----------|------------|-----------------------------|
| AndroidX Core, Activity, Lifecycle, Compose UI and Material 3, Navigation, WorkManager, Room, SQLite, Hilt AndroidX, and their resolved `androidx.*` runtime dependencies | Direct versions in `gradle/libs.versions.toml`; Compose BOM 2026.11.01 resolves Compose 1.13.0 and Material 3 1.5.0 | [AndroidX source](https://github.com/androidx/androidx), [license](https://github.com/androidx/androidx/blob/androidx-main/LICENSE.txt) |
| Dagger and Hilt (`com.google.dagger`) | 2.60.1 | [Project](https://github.com/google/dagger), [resolved POM](https://repo1.maven.org/maven2/com/google/dagger/hilt-android/2.60.1/hilt-android-2.60.1.pom) |
| Kotlin standard library and JetBrains annotations | Kotlin 2.4.21; annotations 13.0 | [Kotlin POM](https://repo1.maven.org/maven2/org/jetbrains/kotlin/kotlin-stdlib/2.4.21/kotlin-stdlib-2.4.21.pom), [annotations POM](https://repo1.maven.org/maven2/org/jetbrains/annotations/13.0/annotations-13.0.pom) |
| kotlinx.coroutines | 1.11.0 | [Project](https://github.com/Kotlin/kotlinx.coroutines), [POM](https://repo1.maven.org/maven2/org/jetbrains/kotlinx/kotlinx-coroutines-core/1.11.0/kotlinx-coroutines-core-1.11.0.pom) |
| kotlinx.serialization | 1.13.0 | [Project](https://github.com/Kotlin/kotlinx.serialization), [POM](https://repo1.maven.org/maven2/org/jetbrains/kotlinx/kotlinx-serialization-json/1.13.0/kotlinx-serialization-json-1.13.0.pom) |
| Guava ListenableFuture-only artifact | 1.0 | [Project](https://github.com/google/guava), [parent license metadata](https://repo1.maven.org/maven2/com/google/guava/guava-parent/26.0-android/guava-parent-26.0-android.pom) |
| JSR 305 annotations (`com.google.code.findbugs:jsr305`) | 3.0.2 | [Published POM and license declaration](https://repo1.maven.org/maven2/com/google/code/findbugs/jsr305/3.0.2/jsr305-3.0.2.pom) |
| `javax.inject` API | 1 | [Published POM and license declaration](https://repo1.maven.org/maven2/javax/inject/javax.inject/1/javax.inject-1.pom) |
| Jakarta Dependency Injection API | 2.0.1 | [Project](https://github.com/eclipse-ee4j/injection-api), [POM](https://repo1.maven.org/maven2/jakarta/inject/jakarta.inject-api/2.0.1/jakarta.inject-api-2.0.1.pom) |
| JSpecify annotations | 1.0.0 | [Project](https://github.com/jspecify/jspecify), [POM](https://repo1.maven.org/maven2/org/jspecify/jspecify/1.0.0/jspecify-1.0.0.pom) |

## Manrope typeface

The app bundles Manrope Light, Regular, Medium, and SemiBold TTF resources.

> Copyright 2018 The Manrope Project Authors
> (https://github.com/sharanda/manrope)

Manrope is licensed under the **SIL Open Font License 1.1**, not Apache 2.0.
The complete copyright and license text is preserved in
[`app/src/main/res/raw/manrope_license.txt`](app/src/main/res/raw/manrope_license.txt)
and displayed from the app's Settings screen.
