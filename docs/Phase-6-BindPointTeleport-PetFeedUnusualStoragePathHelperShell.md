# Phase 6 - Pet Feed Unusual Storage Path Helper Shell

Date: May 27, 2026
Unit of Work: UOW-1369

## Scope

This unit adds a disabled output-path and filename helper shell for future unusual-storage JSON artifacts. It does not create directories, write files, serialize JSON, retain raw packet bytes, mutate storage, dispatch packets, or invoke the helper from `writeArtifact(...)`.

Java remains the source of truth. Source breadcrumbs touched in this unit:

- `com.aionemu.gameserver.services.toypet.PetFeedUnusualStorageArtifactCapture`
- `com.aionemu.gameserver.configs.main.PetFeedUnusualStorageArtifactCaptureConfig`

## Implementation

`PetFeedUnusualStorageArtifactCapture` now carries `playerObjectId` from registration context into the completed artifact snapshot. This closes the filename uniqueness gap identified in UOW-1368 without changing live behavior while capture remains disabled.

The class also has private helper methods for future output:

- `buildArtifactPath(ArtifactSnapshot snapshot)`
- `resolveOutputDirectory(String outputDirectory)`
- `sanitizeFileNameFragment(String value)`
- `isSafeFileNameChar(char c)`

The helper normalizes the configured output directory to an absolute path, rejects blank output directories, sanitizes scenario-name filename fragments, builds a deterministic `.json` filename from scenario/player/storage/item/timing metadata, and validates that the normalized target stays under the resolved output directory.

The helper is intentionally not invoked yet. `writeArtifact(...)` remains a no-op.

## Migration Parity Table - UOW-1369

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.toypet.PetFeedUnusualStorageArtifactCapture` | future C# artifact/schema reader | Utility / Capture Context | Partial | Manual Only | Needs Verification | Adds player object id to capture/snapshot metadata and private output path helper methods. Helper is not invoked; no directory creation, JSON serialization, raw byte retention, or file output exists. |
| `com.aionemu.gameserver.configs.main.PetFeedUnusualStorageArtifactCaptureConfig` | future C# artifact-capture config | Config Class | Partial | Manual Only | Needs Verification | Existing `OUTPUT_DIR` value is consumed by snapshot metadata and future path helper validation. Config defaults and properties are unchanged. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| None | Implementation / Compile Attempt | Java capture source review | Adds deterministic path helper shell and player id snapshot metadata without enabling output. | Source implementation only. | Maven/Java 25 compile unavailable locally; no unit test for sanitization/path containment, no JSON artifact, no raw byte retention, and no C# reader validation. |

## Remaining Risks

- Java compile validation remains blocked locally by missing Maven/Java 25 tooling.
- Helper behavior is source-reviewed only; no unit tests validate path containment, blank path rejection, or filename sanitization.
- `buildArtifactPath(...)` is not invoked by `writeArtifact(...)` yet.
- Atomic write behavior, directory creation, JSON serialization, raw/canonical byte retention, C# artifact reader/schema validation, and warehouse-add byte comparison remain missing.

## Summary Metrics

- Total Java artifacts discovered: 2 grouped artifact rows in this unit
- Total artifacts ported: 1 disabled output path helper shell
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 2 grouped rows
- Total blocked artifacts: Java compile validation, path helper tests, helper invocation, directory creation, atomic output, JSON writer, Java runtime artifacts, C# artifact reader/schema validation, warehouse-add byte comparison
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Add focused Java-side tests or a minimal validation seam for filename sanitization and path containment if a test harness is available; otherwise add a disabled writer invocation boundary that calls `buildArtifactPath(...)` but still does not create directories, serialize JSON, or write files.
