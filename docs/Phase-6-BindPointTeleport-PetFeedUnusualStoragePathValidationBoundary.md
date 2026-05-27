# Phase 6 - Pet Feed Unusual Storage Path Validation Boundary

Date: May 27, 2026
Unit of Work: UOW-1370

## Scope

This unit invokes the disabled artifact path helper from the no-op writer boundary. It does not create directories, write files, serialize JSON, retain raw packet bytes, mutate storage, dispatch packets, or change live behavior while capture remains disabled by default.

Java remains the source of truth. Source breadcrumbs touched in this unit:

- `com.aionemu.gameserver.services.toypet.PetFeedUnusualStorageArtifactCapture`

## Implementation

`writeArtifact(ArtifactSnapshot snapshot)` now calls `buildArtifactPath(snapshot)` inside a guarded `try/catch`. Any runtime validation failure is swallowed at the capture boundary because artifact capture must never affect packet dispatch or writer lifecycle.

The method still performs no filesystem output. This only moves the path helper from unused shell to disabled writer-boundary validation.

## Migration Parity Table - UOW-1370

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.toypet.PetFeedUnusualStorageArtifactCapture` | future C# artifact/schema reader | Utility / Capture Context | Partial | Manual Only | Needs Verification | `writeArtifact(...)` now calls the path helper and swallows validation failures, but still creates no directories, writes no JSON, retains no raw bytes, and has no runtime validation. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| None | Implementation / Compile Attempt | Java capture source review | Moves path helper invocation into the no-op writer boundary without enabling output. | Source implementation only. | Maven/Java 25 compile unavailable locally; no test harness exists under `game-server/src/test` or `commons/src/test`; no runtime writer validation. |

## Remaining Risks

- Java compile validation remains blocked locally by missing Maven/Java 25 tooling.
- Helper behavior is still source-reviewed only; no unit tests validate path containment, blank path rejection, or filename sanitization.
- Path validation failures are swallowed to preserve capture safety; future logging/metrics may be needed before enabling output.
- Atomic write behavior, directory creation, JSON serialization, raw/canonical byte retention, C# artifact reader/schema validation, and warehouse-add byte comparison remain missing.

## Summary Metrics

- Total Java artifacts discovered: 1 grouped artifact row in this unit
- Total artifacts ported: 1 disabled path validation invocation boundary
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 1 grouped row
- Total blocked artifacts: Java compile validation, path helper tests, directory creation, atomic output, JSON writer, Java runtime artifacts, C# artifact reader/schema validation, warehouse-add byte comparison
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Add a read-only JSON DTO/field-order audit for future `ArtifactSnapshot` serialization with fastjson2. Do not serialize or write JSON until the DTO shape, field order, and raw/canonical byte fields are documented.
