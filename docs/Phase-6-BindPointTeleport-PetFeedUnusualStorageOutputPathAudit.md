# Phase 6 - Pet Feed Unusual Storage Output Path Audit

Date: May 27, 2026
Unit of Work: UOW-1368

## Scope

This is a read-only audit for future unusual-storage JSON artifact output path and filename safety. It does not implement path helpers, serialize JSON, retain raw packet bytes, create directories, write files, mutate storage, dispatch packets, or change capture runtime behavior.

Java remains the source of truth. Source breadcrumbs reviewed in this unit:

- `com.aionemu.gameserver.services.toypet.PetFeedUnusualStorageArtifactCapture`
- `com.aionemu.gameserver.configs.main.PetFeedUnusualStorageArtifactCaptureConfig`
- `com.aionemu.commons.logging.Logging`
- `com.aionemu.commons.utils.concurrent.RunnableStatsManager`
- `com.aionemu.gameserver.dataholders.SpawnsData`

## Findings

The configured unusual-storage output path currently defaults to `./parity-artifacts/pet-feed-unusual-storage/java`. That location is outside production `data` and `log` trees, which is appropriate for parity artifacts and avoids mixing generated packet snapshots with runtime logs or static game data.

Existing Java output examples are useful but not sufficient as-is:

- `Logging` writes archives under `log/archived`, creates parent directories, and uses `Files.newOutputStream(...)`.
- `RunnableStatsManager` creates `./log/stats` and writes a fixed log file.
- `SpawnsData` writes XML directly through `Files.writeString(...)` after parent directory creation.

Those paths are controlled internal outputs. The artifact writer will consume a configurable directory, so it needs stricter containment before any file output is enabled.

## Future Output Safety Rules

A future helper should:

- reject blank output directories;
- resolve the configured output directory through `Path.of(...).toAbsolutePath().normalize()`;
- create the output directory only after validation;
- build filenames from deterministic artifact metadata, not packet class names alone;
- sanitize scenario names and any string filename fragments to `[A-Za-z0-9._-]`;
- write only `.json` files below the resolved output directory;
- write to a sibling temporary file first, then move into place with atomic move when supported;
- fall back to a non-atomic move only if atomic move is unavailable and the failure is logged/documented;
- avoid doing filesystem work inside the packet serialization callback.

Candidate deterministic filename fields:

- scenario name;
- player object id once the snapshot carries it;
- storage id and storage ordinal;
- item object id;
- registration/completion timestamp millis.

The current snapshot does not yet carry player object id, so filename uniqueness would need either player id added to `CaptureContext`/`ArtifactSnapshot` or a monotonic sequence number. That should be solved before JSON output is enabled.

## Migration Parity Table - UOW-1368

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.toypet.PetFeedUnusualStorageArtifactCapture` | future C# artifact/schema reader | Utility / Capture Context | Partial | Manual Only | Needs Verification | Read-only audit identifies output helper prerequisites. Current snapshot lacks player object id or sequence number for stronger filename uniqueness. `writeArtifact(...)` remains no-op. |
| `com.aionemu.gameserver.configs.main.PetFeedUnusualStorageArtifactCaptureConfig` | future C# artifact-capture config | Config Class | Partial | Manual Only | Needs Verification | Default output directory is suitable for parity artifacts, but future code must validate and normalize it before creating directories or files. |
| `com.aionemu.commons.logging.Logging` | No C# artifact equivalent in current slice | Utility | Not Started | Manual Only | Needs Verification | Reviewed as a Java file-output example. It creates directories and writes archives under `log`, but it is not a generic safe configurable artifact writer. |
| `com.aionemu.commons.utils.concurrent.RunnableStatsManager` | No C# artifact equivalent in current slice | Utility | Not Started | Manual Only | Needs Verification | Reviewed as a fixed diagnostic output example under `./log/stats`; not suitable as-is for configurable parity artifact output. |
| `com.aionemu.gameserver.dataholders.SpawnsData` | No C# artifact equivalent in current slice | Data Holder / Utility | Not Started | Manual Only | Needs Verification | Reviewed direct XML `Files.writeString(...)` behavior. Future artifact output should be stricter because output directory is configurable and artifacts are generated asynchronously. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| None | Read-only audit | Java config and output helper source review | Documents safe output path and filename prerequisites for future artifact writer. | Source inspection only. | No helper implementation, no filesystem test, no JSON artifact, no raw byte retention, and no C# reader validation. |

## Remaining Risks

- Java compile validation remains blocked locally by missing Maven/Java 25 tooling.
- Output helper is not implemented yet.
- Current snapshot metadata is probably insufficient for collision-resistant deterministic filenames.
- Atomic write behavior and fallback policy are not implemented.
- JSON serialization, raw/canonical byte retention, C# artifact reader/schema validation, and warehouse-add byte comparison remain missing.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 0 source artifacts; 1 read-only output-path audit
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: Java compile validation, output path helper, filename uniqueness metadata, JSON writer, atomic output, Java runtime artifacts, C# artifact reader/schema validation, warehouse-add byte comparison
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Add a disabled output-path/filename helper shell to `PetFeedUnusualStorageArtifactCapture`: validate/normalize the configured output directory, sanitize filename fragments, and construct a target `.json` path from snapshot metadata without writing files or serializing JSON.
