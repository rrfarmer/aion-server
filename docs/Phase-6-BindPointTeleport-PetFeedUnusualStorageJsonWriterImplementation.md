# Phase 6 - Pet Feed Unusual Storage JSON Writer Implementation

Date: May 27, 2026
Unit of Work: UOW-1382

## Scope

This unit adds disabled JSON/file output to the Java unusual-storage artifact writer boundary. Capture remains disabled by default and no live storage mutation, packet dispatch, C# reader behavior, or warehouse-add byte comparison is enabled.

Java remains the source of truth. Source breadcrumbs touched in this unit:

- `com.aionemu.gameserver.services.toypet.PetFeedUnusualStorageArtifactCapture`
- `com.aionemu.gameserver.configs.main.PetFeedUnusualStorageArtifactCaptureConfig`
- `com.alibaba.fastjson2.JSON`
- `com.alibaba.fastjson2.JSONWriter.Feature`
- `java.nio.file.Files`

## Implementation

`writeArtifact(...)` now builds the validated artifact path and schema-v1 DTO, then writes JSON through a guarded `writeJsonArtifact(...)` helper. Writer failures are still swallowed by the writer boundary, preserving the rule that artifact generation must never affect packet dispatch or writer lifecycle.

The helper:

- rejects a target with no parent;
- rejects an existing target rather than overwriting an artifact;
- creates the validated output directory;
- creates a same-directory temporary file;
- serializes the existing `LinkedHashMap` DTO with fastjson2 and `JSONWriter.Feature.WriteMapNullValue`;
- encodes JSON using explicit UTF-8 bytes;
- moves the temporary file to the final path with `StandardCopyOption.ATOMIC_MOVE`;
- falls back to a regular same-directory move when atomic move is unsupported;
- best-effort deletes the temporary file in `finally`.

The schema notes now state that capture remains disabled by default and artifact output requires explicit config opt-in.

## Boundaries Preserved

- `PetFeedUnusualStorageArtifactCaptureConfig.ENABLED` remains false by default.
- The observer is installed only through the existing config-gated lifecycle hook.
- File output happens from the writer queue/worker path, not directly from packet serialization.
- Writer exceptions are caught at the boundary.
- No C# reader/schema behavior changed.
- No warehouse-add byte comparison was enabled.
- No parity was promoted to verified.

## Migration Parity Table - UOW-1382

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.toypet.PetFeedUnusualStorageArtifactCapture` | future C# artifact/schema reader | Utility / Capture Context | Partial | Manual Only | Needs Verification | Adds guarded JSON/file writer output using existing schema DTO and path helper. Capture is disabled by default; no runtime artifact, C# reader validation, or Java/C# byte comparison exists. Threading remains queue/worker-based with writer exceptions swallowed. |
| `com.aionemu.gameserver.configs.main.PetFeedUnusualStorageArtifactCaptureConfig` | future C# artifact fixture path/config | Config | Partial | Manual Only | Needs Verification | Existing config controls opt-in enablement, output directory, and queue bounds. No config defaults changed; output remains disabled unless explicitly enabled. |
| `com.alibaba.fastjson2.JSON` / `JSONWriter.Feature` | C# JSON artifact reader/schema validator | Dependency / Serialization Utility | Partial | Manual Only | Needs Verification | Uses `JSON.toJSONString(..., WriteMapNullValue)` and explicit UTF-8 bytes. fastjson2 feature behavior and field order still need runtime validation. |
| `java.nio.file.Files` output boundary | future C# artifact fixture ingestion | Filesystem Utility | Partial | Manual Only | Needs Verification | Creates directories, writes a sibling temp file, atomically moves when supported, falls back to regular move, and cleans up temp files. File collision behavior is fail-closed; no runtime filesystem validation yet. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| None | Implementation / Compile Attempt | Java capture/config/writer-source review | Adds disabled guarded JSON/file output with explicit UTF-8 and atomic-move fallback. | Source implementation only. | Maven/Java 25 compile unavailable locally; no runtime artifact, no C# reader validation, and no Java/C# byte comparison. |

## Remaining Risks

- Java compile validation remains blocked locally by missing Maven/Java 25 tooling.
- Local fastjson2 feature disassembly remains blocked because `javap` is not available on PATH.
- fastjson2 `WriteMapNullValue` behavior and `LinkedHashMap` field order are source-assumed until runtime validation.
- Atomic move fallback is source-reviewed only and not filesystem-tested locally.
- Existing target collision fails closed and drops the artifact through the writer catch; this is safe but not surfaced in metrics yet.
- JSON output can expose player/item identifiers and remains opt-in only.
- C# artifact reader/schema validation, runtime artifact generation, and warehouse-add byte comparison remain missing.

## Summary Metrics

- Total Java artifacts discovered: 4 grouped artifact rows in this unit
- Total artifacts ported: 1 disabled JSON/file writer implementation
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 4 grouped rows
- Total blocked artifacts: Java compile validation, runtime artifact validation, C# artifact reader/schema validation, warehouse-add byte comparison
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Add a read-only Java runtime activation plan for unusual-storage artifact generation: define how to run the disabled writer with `gameserver.petfeed.unusual_storage_artifacts.enabled=true` in a local Java 25/Maven environment, which scenario to trigger, where output should land, and what artifact fields must be checked before C# reader work starts.
