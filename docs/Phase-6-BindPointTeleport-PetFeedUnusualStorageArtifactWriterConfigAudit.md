# Phase 6 - Pet Feed Unusual Storage Artifact Writer Config Audit

Date: May 27, 2026
Unit of Work: UOW-1352

## Scope

This unit performs a read-only Java audit for the future unusual-storage rejected-food runtime artifact writer. It does not add output, config keys, byte copying, or capture enablement.

Java source and build artifacts reviewed:

- `commons/pom.xml`
- `com.alibaba.fastjson2.JSON`
- `com.aionemu.gameserver.configs.Config`
- `com.aionemu.commons.configuration.Property`
- `com.aionemu.commons.configuration.ConfigurableProcessor`
- `com.aionemu.gameserver.utils.ThreadPoolManager`
- `com.aionemu.commons.utils.concurrent.RunnableWrapper`
- `com.aionemu.commons.utils.concurrent.ExecuteWrapper`
- `com.aionemu.commons.logging.DiscordChannelAppender`
- `com.aionemu.gameserver.configs.main.LoggingConfig`
- `com.aionemu.gameserver.configs.network.NetworkConfig`

## Findings

- JSON dependency:
  - `commons/pom.xml` already declares `com.alibaba.fastjson2:fastjson2:2.0.60`.
  - Existing Java usage is narrow: `ExternalAuth` uses `JSON.toJSONString(...)` / `JSON.parseObject(...)`, and `DiscordChannelAppender` uses `JSON.toJSONBytes(...)`.
  - Future artifact writer can likely use `fastjson2` without adding a new dependency, but exact field ordering/format must be verified before C# reader comparisons depend on it.
- Config convention:
  - `Config.load(...)` reads defaults from `./config/administration`, `./config/main`, and `./config/network`, then overlays `./config/mygs.properties`.
  - Config classes are static fields annotated with `@Property`.
  - New capture config should use a dedicated class under `game-server/src/com/aionemu/gameserver/configs/main` and must be added to `Config.CONFIGS`; otherwise `mygs.properties` overrides will be ignored as unknown.
  - Defaults should keep capture disabled and point to a bounded local output path such as `./parity-artifacts/pet-feed-unusual-storage/java`, but adding that default is future work.
- Output/file conventions:
  - Existing Java code writes under working-directory-relative paths such as `./log`, `./log/stats`, `./cache/classes`, generated XML under `data/static_data`, and config under `./config/...`.
  - Future parity artifacts should avoid `game-server/data` and production logs. A repository-root or game-server-working-dir `./parity-artifacts/...` path is preferable and should be configurable.
  - Future writer must create directories with `Files.createDirectories(...)`, write UTF-8, and use bounded file names that do not include raw player names or client-controlled strings.
- Threading / dispatch safety:
  - `AionServerPacket.write` runs during connection packet serialization, so artifact writing must not happen inline.
  - `ThreadPoolManager.execute(...)` uses the instant pool and `RunnableWrapper` / `ExecuteWrapper` for exception isolation and runtime warnings.
  - The instant pool has an `ArrayBlockingQueue<>(100000)`, but a capture writer still needs its own small bounded queue or drop policy so a bad capture session cannot flood normal gameplay work.
  - `executeLongRunning(...)` uses a cached pool and is not appropriate for per-packet artifact writes.
- Security / privacy:
  - Capture output must stay disabled by default.
  - Output should be scenario allow-listed and avoid credential/session/client input capture.
  - The future writer should copy only necessary clear-frame bytes after packet allow-listing and should not retain mutable `ByteBuffer` references.

## Recommended Future Writer Shape

Before enabling output, add:

- `PetFeedUnusualStorageArtifactCaptureConfig` with disabled default, output directory, max pending contexts, max queued artifacts, and scenario allow-list fields.
- Explicit registration in `Config.CONFIGS`.
- A bounded writer queue dedicated to capture output, not a direct `ThreadPoolManager.execute(...)` call per packet.
- Immediate byte-array copy from the clear frame only after matching the guarded scenario and packet class.
- `fastjson2` serialization with deterministic field names matching schema-v1, then C# reader tests that tolerate absent files and compare exact bytes only when Java artifacts exist.

## Boundaries Preserved

- No Java source behavior was changed.
- No config key was added.
- No artifact writer was added.
- No observer was installed or enabled.
- No raw packet bytes were copied or retained.
- No C# reader/test behavior was changed.

## Validation

- Documentation-only audit; no compile was required for code changes.
- Attempted Java compile is still known blocked locally because `mvn` is not available on PATH and no Maven wrapper exists in the repository.
- No Java runtime artifacts were generated.
- No .NET tests were required for this docs-only audit.

## Migration Parity Table - UOW-1352

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `commons/pom.xml` dependency `com.alibaba.fastjson2:fastjson2` | future C# artifact reader JSON parser | Build Dependency | Not Started | Manual Only | Needs Verification | Existing dependency can likely serialize future artifacts. Field ordering/format and C# parser compatibility remain unverified. |
| `com.alibaba.fastjson2.JSON` | future C# schema-v1 reader | Serialization Utility | Not Started | Manual Only | Needs Verification | Existing Java uses `JSON.toJSONBytes`/`toJSONString`/`parseObject`. Future artifact writer must verify deterministic field output before byte-level schema claims. |
| `com.aionemu.gameserver.configs.Config` | future C# parity artifact test configuration | Config Loader | Not Started | Manual Only | Needs Verification | New capture config must be added to `CONFIGS` or overrides will be ignored as unknown. No config added in this unit. |
| `com.aionemu.commons.configuration.Property` | future capture config class | Config Annotation | Not Started | Manual Only | Needs Verification | Future config should use static fields annotated with `@Property`. Defaults must keep capture disabled. |
| `com.aionemu.commons.configuration.ConfigurableProcessor` | future C# config parity notes | Config Processor | Not Started | Manual Only | Needs Verification | Processor applies defaults, overlay properties, placeholder replacement, and unknown-property reporting. New keys require registered config class. |
| `com.aionemu.gameserver.utils.ThreadPoolManager` | future capture writer scheduler/queue | Threading Utility | Not Started | Manual Only | Needs Verification | Future writer should not block packet serialization. Direct instant-pool use still needs a dedicated bounded capture queue/drop policy. |
| `com.aionemu.commons.utils.concurrent.RunnableWrapper` | future C# background writer exception policy | Threading Utility | Not Started | Manual Only | Needs Verification | Existing wrapper catches/logs throwables when configured. Future writer still needs capture-specific isolation and backpressure. |
| `com.aionemu.commons.utils.concurrent.ExecuteWrapper` | future C# background writer exception policy | Threading Utility | Not Started | Manual Only | Needs Verification | Existing runtime warning/exception behavior is known from source only; capture writer behavior remains unimplemented. |
| `com.aionemu.commons.logging.DiscordChannelAppender` | N/A | JSON Usage Example | Not Started | Manual Only | Needs Verification | Demonstrates `JSON.toJSONBytes(Map.of(...))` use, but webhook behavior is unrelated to capture artifacts and must not be copied wholesale. |
| `com.aionemu.gameserver.configs.main.LoggingConfig` | future capture config class | Config Class Example | Not Started | Manual Only | Needs Verification | Existing logging toggles are static `@Property` fields. Capture should use a separate config class rather than overloading production logging. |
| `com.aionemu.gameserver.configs.network.NetworkConfig` | future capture config class | Config Class Example | Not Started | Manual Only | Needs Verification | Existing network packet logging keys are unrelated to server-packet artifact output; capture should remain separately guarded. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| None | Documentation / Audit | Java build/config/threading/source review | Identifies safe prerequisites for a future disabled artifact writer. | Manual source review only. | No writer, config, runtime artifacts, or C# schema comparisons exist. |

## Remaining Risks

- Java compile validation remains blocked locally by missing Maven/Java 25 tooling.
- Future JSON output field ordering/format is unverified.
- Future writer queue/drop policy is not designed in code.
- Future output directory security and cleanup are not implemented.
- No packet bytes or decoded item/blob fields are captured.
- Capture enablement remains absent and should stay absent until compile/runtime validation is available.

## Summary Metrics

- Total Java artifacts discovered: 11 grouped artifact rows in this unit
- Total artifacts ported: 0 code artifacts; 1 writer/config audit document
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 11 grouped rows
- Total blocked artifacts: Java compile validation, capture config class, config registration, writer queue/drop policy, JSON output determinism, byte copying, item blob decoder, live unusual-storage adapter
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Add a disabled capture config shell only: `PetFeedUnusualStorageArtifactCaptureConfig` with safe defaults and registration in `Config.CONFIGS`, but do not install the observer, enable capture, write files, or copy packet bytes.
