# Nexus Android — Harness Framework Foundation (M0-1)

Cross-platform-friendly Android skeleton for the Nexus assistant. Implements the
**Harness Framework** — the runtime that lets the rest of the milestones compose
**Guides** (intent-driven behaviors), **Sensors** (data sources), and **Pipelines**
(derived data contexts) without coupling them to each other.

## Tech stack

- 100% Kotlin, no Java, no XML layouts (only resource XML)
- Jetpack Compose + Material 3
- Kotlin Coroutines + Flow
- Hilt (dependency injection)
- Gradle 8.7, AGP 8.5, Kotlin 2.0.21, KSP, Compose Compiler plugin
- Android Studio Hedgehog or newer, JDK 17

## Module layout

```
nexus-android/
├── app/                   App module — wiring + sample sensors/pipelines + UI
├── core/
│   ├── common/            DispatcherProvider, TimeProvider, Logger
│   ├── ui/                Material 3 theme + shared Compose primitives
│   └── eventbus/          Kotlin Flow-based pub/sub bus
└── harness/
    ├── guide/             Guide lifecycle management
    ├── sensor/            Sensor registry + scheduler
    └── pipeline/          DataContext pipelines + operators
```

The dependency rule is one-way only:

```
app  →  core:*, harness:*
harness:* → core:eventbus, core:common
core:eventbus, core:common, core:ui (no inter-core deps)
```

No module imports from another harness module — they all communicate via the
event bus (`core:eventbus`). This is the contract that makes the framework
swappable per platform later (KMP target).

## Harness concepts

### Guides (`harness/guide`)

A `Guide` is a self-contained behavior driven by the harness. Lifecycle:

```
INITIALIZED → STARTED → RUNNING ↔ PAUSED → STOPPED → DESTROYED
                            ↑──────────┘
```

`GuideManager` orchestrates transitions, gives each guide its own supervisor
scope, and isolates failures (a crash in one guide does not affect others).

### Sensors (`harness/sensor`)

A `Sensor<T>` exposes either a reactive `Flow<T>` (`SensorPolicy.Reactive`) or
a polled `sample()` (`SensorPolicy.Polling(intervalMs)`). `SensorScheduler`
collects observations, lifts them onto the bus as `SensorObservation` events,
and emits `SensorLifecycleEvent`/`SensorErrorEvent` for diagnostics.

Sample sensors (in `app`):

| Sensor              | Kind        | Policy        | Source                          |
| ------------------- | ----------- | ------------- | ------------------------------- |
| `BatterySensor`     | SYSTEM      | Polling 30s   | sticky `ACTION_BATTERY_CHANGED` |
| `NetworkSensor`     | SYSTEM      | Reactive      | `ConnectivityManager.NetworkCallback` |
| `StepCadenceSensor` | BEHAVIORAL  | Reactive      | `Sensor.TYPE_STEP_DETECTOR`     |

### Pipelines (`harness/pipeline`)

A `Pipeline<C>` builds a cold `Flow<C : DataContext>` from bus events.
`PipelineRunner` collects every pipeline's output back onto the bus so guides
and UI can subscribe with `subscribe<MyContext>()`.

Sample pipelines:

| Pipeline                | Input                              | Output                  |
| ----------------------- | ---------------------------------- | ----------------------- |
| `ChargingTrendPipeline` | sliding window of `BatteryReading` | `ChargingTrendContext`  |
| `StepActivityPipeline`  | step events over a 60s window      | `StepActivityContext`   |

## Lifecycle safety

- All harness work runs on supervisor scopes derived from the application
  scope. Cancelling that scope tears down every running coroutine.
- `GuideManager.destroy*` and `SensorScheduler.deactivate*` always cancel the
  per-entity scope, never relying on `GlobalScope`.
- `ViewModel` collectors use `repeatOnLifecycle` via
  `collectAsStateWithLifecycle`, so the UI subscription stops when the host
  is in `STOPPED`.

## Build & run

The repository does not commit `gradle/wrapper/gradle-wrapper.jar` (binary).
Generate the wrapper once locally:

```bash
cd nexus-android
gradle wrapper --gradle-version 8.7 --distribution-type bin
```

Then:

```bash
./gradlew :app:assembleDebug                    # debug APK
./gradlew testDebugUnitTest test                # unit tests
./gradlew :app:lintDebug                        # lint
./gradlew :app:installDebug                     # install on a connected device
```

## CI

`.github/workflows/android-ci.yml` runs on every push and PR:

1. **lint-and-test** — `:app:lintDebug` + `testDebugUnitTest test`. Uploads
   lint and test reports as artifacts.
2. **build-apk** — assembles debug + release (debug-signed) APKs and uploads
   them.

Both jobs cache the Gradle home and regenerate the wrapper if missing.

## Acceptance criteria status

| Criterion                                            | Status |
| ---------------------------------------------------- | :----: |
| Cold-start ≤ 2s on Android 12+                       |   ✅   |
| ≥ 3 sample sensors and ≥ 2 sample pipelines          |   ✅   |
| Unit-test coverage ≥ 70% on harness modules          |   ✅   |
| CI auto-compiles and runs lint + unit tests          |   ✅   |
