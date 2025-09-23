# kdriver-nextjs

[![License](https://img.shields.io/github/license/cdpdriver/kdriver-nextjs)](LICENSE)
[![Maven Central Version](https://img.shields.io/maven-central/v/dev.kdriver/nextjs)](https://klibs.io/project/cdpdriver/kdriver-nextjs)
[![Issues](https://img.shields.io/github/issues/cdpdriver/kdriver-nextjs)]()
[![Pull Requests](https://img.shields.io/github/issues-pr/cdpdriver/kdriver-nextjs)]()
[![codecov](https://codecov.io/github/cdpdriver/kdriver-nextjs/branch/main/graph/badge.svg?token=F7K641TYFZ)](https://codecov.io/github/cdpdriver/kdriver-nextjs)
[![CodeFactor](https://www.codefactor.io/repository/github/cdpdriver/kdriver-nextjs/badge)](https://www.codefactor.io/repository/github/cdpdriver/kdriver-nextjs)
[![Open Source Helpers](https://www.codetriage.com/cdpdriver/kdriver-nextjs/badges/users.svg)](https://www.codetriage.com/cdpdriver/kdriver-nextjs)

NextJs extensions for [KDriver](https://github.com/cdpdriver/kdriver).

## Installation

Add the dependency to your `build.gradle.kts`:

```kotlin
dependencies {
    implementation("dev.kdriver:nextjs:0.1.4")
}
```

## Usage

```kotlin
fun main() = runBlocking {
    val browser = createBrowser(this)
    val tab = browser.get("about:blank")
    val allObjects = tab.capturePushesFromJs { // Or `capturePushesFromHtml`
        tab.get(url)
        fetchAll()
    }
    println("Raw captured objects: ${allObjects.size}")
}
```
