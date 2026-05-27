# Phase 6 - Pet Feed Unusual Storage Capture Config Consumption

Date: May 27, 2026
Unit of Work: UOW-1354

## Scope

This unit wires the disabled config shell into the Java unusual-storage capture shell without enabling capture, installing observers, writing files, or copying packet bytes.

Java source touched:

- `com.aionemu.gameserver.services.toypet.PetFeedUnusualStorageArtifactCapture`
- `com.aionemu.gameserver.configs.main.PetFeedUnusualStorageArtifactCaptureConfig`

## What Changed

- `PetFeedUnusualStorageArtifactCapture.isEnabled()` now reads `PetFeedUnusualStorageArtifactCaptureConfig.ENABLED`.
- Pending-context queue trimming now reads `MAX_PENDING_CONTEXTS_PER_PLAYER` from config and clamps it to at least one.
- The in-memory no-output snapshot now carries configured scenario and output-directory values for a future writer boundary.
- Capture remains disabled by default because the registered config default is `false`.

## Boundaries Preserved

- No observer installation was added.
- No artifact writer was added.
- No output directory is created.
- No raw packet bytes are copied or retained.
- No JSON serializer was added.
- No C# reader/test behavior changed.

## Validation

- Attempted `mvn -pl game-server -am -DskipTests compile`.
- Validation was blocked because `mvn` is not available on PATH and no Maven wrapper exists in the repository (`mvnw`/`mvnw.cmd` absent).
- No Java runtime artifacts were generated.
- No .NET tests were required for this Java-only disabled config-consumption slice.

## Migration Parity Table - UOW-1354

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.toypet.PetFeedUnusualStorageArtifactCapture` | future C# artifact/schema reader | Utility / Capture Context | Partial | Manual Only | Needs Verification | Reads registered config for enabled flag, queue bound, scenario name, and output directory. Defaults keep capture disabled. No observer install, writer, byte copy, or runtime validation. |
| `com.aionemu.gameserver.configs.main.PetFeedUnusualStorageArtifactCaptureConfig` | future C# artifact capture configuration | Config Class | Partial | Manual Only | Needs Verification | Config fields are now consumed by the capture shell, but no runtime config-load validation was possible locally. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| None | Implementation / Compile Attempt | Java config and capture source review | Wires disabled config fields into the capture shell without enabling output. | Source implementation only. | Maven/Java 25 compile unavailable locally; no runtime config load or artifact output validation. |

## Remaining Risks

- Java compile validation remains blocked locally by missing Maven/Java 25 tooling.
- Capture could become active only if config is explicitly enabled in a future runtime, but no observer is installed by this unit.
- Config values are not range-validated beyond the pending-context lower bound.
- Future writer must still implement bounded queue/drop policy, deterministic JSON output, directory creation, and security guards.
- No packet bytes or decoded item/blob fields are captured.

## Summary Metrics

- Total Java artifacts discovered: 2 grouped artifact rows in this unit
- Total artifacts ported: 1 disabled config-consumption seam
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 2 grouped rows
- Total blocked artifacts: Java compile validation, observer installation, artifact writer, byte copying, item blob decoder, live unusual-storage adapter
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Add a read-only `ItemInfoBlob` decoded-entry audit for unusual-storage warehouse-add artifacts: map Java blob entry ids/order and compare them to the existing C# known-gap classifications before any artifact writer emits decoded blob fields.
