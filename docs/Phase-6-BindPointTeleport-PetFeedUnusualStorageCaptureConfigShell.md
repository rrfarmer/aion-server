# Phase 6 - Pet Feed Unusual Storage Capture Config Shell

Date: May 27, 2026
Unit of Work: UOW-1353

## Scope

This unit adds a disabled Java config shell for future unusual-storage rejected-food runtime artifact capture. It does not enable capture, install observers, write files, or copy packet bytes.

Java source touched:

- `com.aionemu.gameserver.configs.Config`
- `com.aionemu.gameserver.configs.main.PetFeedUnusualStorageArtifactCaptureConfig`
- `com.aionemu.commons.configuration.Property`
- `com.aionemu.commons.configuration.ConfigurableProcessor`

## What Changed

- Added `PetFeedUnusualStorageArtifactCaptureConfig`.
- Registered the config class in `Config.CONFIGS`.
- Added safe default config fields:
  - `gameserver.petfeed.unusual_storage_artifacts.enabled = false`
  - `gameserver.petfeed.unusual_storage_artifacts.output_dir = ./parity-artifacts/pet-feed-unusual-storage/java`
  - `gameserver.petfeed.unusual_storage_artifacts.max_pending_contexts_per_player = 4`
  - `gameserver.petfeed.unusual_storage_artifacts.max_queued_artifacts = 32`
  - `gameserver.petfeed.unusual_storage_artifacts.allowed_scenario = pet_feed_unusual_storage`

The new config is intentionally not consumed by `PetFeedUnusualStorageArtifactCapture` yet. This preserves the disabled runtime boundary while making future `mygs.properties` overrides recognized by Java's config loader.

## Boundaries Preserved

- Capture remains disabled.
- No observer installation was added.
- No artifact writer was added.
- No output directory is created.
- No raw packet bytes are copied or retained.
- No C# reader/test behavior changed.

## Validation

- Attempted `mvn -pl game-server -am -DskipTests compile`.
- Validation was blocked because `mvn` is not available on PATH and no Maven wrapper exists in the repository (`mvnw`/`mvnw.cmd` absent).
- No Java runtime artifacts were generated.
- No .NET tests were required for this Java-only config shell.

## Migration Parity Table - UOW-1353

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.configs.Config` | future C# parity artifact test configuration | Config Loader | Partial | Manual Only | Needs Verification | Registered the new capture config class so future overrides are recognized. Maven compile unavailable locally. |
| `com.aionemu.gameserver.configs.main.PetFeedUnusualStorageArtifactCaptureConfig` | future C# artifact capture configuration | Config Class | Partial | No Tests | Needs Verification | New disabled config shell only. Fields are not consumed by capture code yet; no observer install, writer, or byte copy exists. |
| `com.aionemu.commons.configuration.Property` | future C# config parity notes | Config Annotation | Complete | Manual Only | Needs Verification | Used existing static-field annotation convention. Annotation behavior itself was not changed. |
| `com.aionemu.commons.configuration.ConfigurableProcessor` | future C# config parity notes | Config Processor | Complete | Manual Only | Needs Verification | Existing processor will bind fields after `Config.CONFIGS` registration. Processor behavior was not changed or runtime-tested. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| None | Implementation / Compile Attempt | Java config source review | Adds a disabled registered config shell using the Java config convention. | Source implementation only. | Maven/Java 25 compile unavailable locally; no runtime config load validation. |

## Remaining Risks

- Java compile validation remains blocked locally by missing Maven/Java 25 tooling.
- Config fields are intentionally unused by capture code.
- No default `.properties` file entry was added; defaults come from `@Property(defaultValue=...)`.
- Future writer must still implement bounded queue/drop policy, deterministic JSON output, directory creation, and security guards.
- No packet bytes or decoded item/blob fields are captured.

## Summary Metrics

- Total Java artifacts discovered: 4 grouped artifact rows in this unit
- Total artifacts ported: 1 disabled Java config shell plus config registration
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 4 grouped rows
- Total blocked artifacts: Java compile validation, config consumption, observer installation, artifact writer, byte copying, item blob decoder, live unusual-storage adapter
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Wire the config shell into `PetFeedUnusualStorageArtifactCapture` without enabling capture by default: replace hard-coded bounds/output placeholders with config reads and keep `ENABLED` false unless Java config explicitly enables it. Do not install the observer globally, write files, or copy packet bytes yet.
