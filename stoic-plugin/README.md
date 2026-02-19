# Radiography Stoic Plugin

A [Stoic](https://github.com/block/stoic) plugin that dumps view hierarchies from running Android apps.

The plugin bundles Radiography and all its dependencies (~850KB APK), so target apps don't need them.

## Build

```bash
../gradlew assembleDebug  # APK only
../gradlew dist            # Full distribution package
```

Distribution output: `build/distributions/radiography-stoic-plugin-{version}.tar.gz`

Version is read from `../gradle.properties` to stay in sync with the main library.

## How it works

When invoked via Stoic, the APK's `main()` function calls `Radiography.scan()` on the main thread and prints the view hierarchy to stdout.

See [prebuilt](prebuilt) for the user-facing README and CLI wrapper script.
