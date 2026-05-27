# Phase 6 - Pet Feed Unusual Storage Fastjson Activation Audit

Date: May 27, 2026
Unit of Work: UOW-1381

## Scope

This is a read-only audit for the next JSON/file-output activation step now that the schema DTO shell contains packet body/canonical hex, item-blob hex, and packet-body self-check metadata. It does not implement JSON serialization, create directories, write files, mutate packet buffers, mutate storage, dispatch packets, or enable capture by default.

Java remains the source of truth. Source breadcrumbs reviewed in this unit:

- `com.aionemu.gameserver.services.toypet.PetFeedUnusualStorageArtifactCapture`
- `com.aionemu.gameserver.configs.main.PetFeedUnusualStorageArtifactCaptureConfig`
- `com.alibaba.fastjson2.JSON`
- `com.alibaba.fastjson2.JSONWriter.Feature`
- `java.nio.file.Files`

## Current Readiness

The writer boundary already drains snapshots off the queue before calling `writeArtifact(...)`, so future filesystem work can stay outside the packet serialization callback. `writeArtifact(...)` already validates the output path and builds the schema DTO inside a catch-all guard, which preserves the capture safety boundary.

The schema DTO is built from insertion-ordered `LinkedHashMap` instances and stable `List.of(...)`/`ArrayList` values. This gives a reasonable field-order foundation, but runtime output still needs validation because fastjson2 feature selection and map-order behavior have not been proven in this repository.

The DTO now includes the byte fields needed before first JSON output:

- packet `bodyHex`;
- packet `canonicalPayloadHex`;
- `itemBlob.hex`;
- `itemBlob.packetBodyVerification`.

## Recommended Writer Shape

Future JSON/file output should:

- keep `writeArtifact(...)` as the only file-output boundary;
- call `buildArtifactPath(snapshot)` once and `buildSchemaV1Artifact(snapshot)` once inside the guarded writer section;
- serialize with fastjson2 using a documented feature set that preserves null fields where schema absence matters;
- avoid relying on `Map.of(...)` or unordered maps for artifact content;
- encode bytes with `StandardCharsets.UTF_8`;
- create the output directory with `Files.createDirectories(target.getParent())` after path validation;
- write to a same-directory temporary file;
- move the temporary file to the final target, using `StandardCopyOption.ATOMIC_MOVE` when supported and a documented fallback when not;
- delete or best-effort clean up the temporary file after failure;
- keep all exceptions swallowed inside `writeArtifact(...)` so artifact generation never affects packet dispatch or writer lifecycle.

## Activation Gates

Do not add runtime output until these decisions are fixed in code/docs:

- exact fastjson2 call and feature flags;
- whether pretty formatting is acceptable or compact JSON is required for artifact size;
- whether null schema fields are always emitted;
- deterministic filename collision policy;
- behavior when target file already exists;
- temporary-file suffix and cleanup policy;
- whether dropped artifact count needs to be exported before enabling output;
- Java runtime validation plan for one local artifact before C# reader consumption.

## Migration Parity Table - UOW-1381

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.toypet.PetFeedUnusualStorageArtifactCapture` | future C# artifact/schema reader | Utility / Capture Context | Partial | Manual Only | Needs Verification | Read-only audit confirms the DTO has enough byte fields for first JSON output, but writer implementation, runtime artifact validation, and C# consumption remain missing. |
| `com.aionemu.gameserver.configs.main.PetFeedUnusualStorageArtifactCaptureConfig` | future C# artifact fixture path/config | Config | Partial | Manual Only | Needs Verification | Existing disabled config provides output directory and queue bounds. Activation must keep `ENABLED=false` by default and validate output path before file creation. |
| `com.alibaba.fastjson2.JSON` / `JSONWriter.Feature` | C# JSON artifact reader/schema validator | Dependency / Serialization Utility | Complete dependency, implementation not started | Manual Only | Needs Verification | Dependency exists, but exact feature flags and deterministic output behavior still need implementation and runtime validation. |
| `java.nio.file.Files` output boundary | future C# artifact fixture ingestion | Filesystem Utility | Not Started | Manual Only | Needs Verification | Future writer should use explicit UTF-8, same-directory temporary files, and atomic move with fallback. No file output exists yet. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| None | Read-only audit | Java capture/config/writer-source review | Documents JSON/file activation gates after byte fields were added to the DTO shell. | Source inspection only. | No writer implementation, no Java compile, no runtime artifact, no C# reader validation, and no Java/C# byte comparison. |

## Remaining Risks

- Java compile validation remains blocked locally by missing Maven/Java 25 tooling.
- fastjson2 feature behavior and `LinkedHashMap` output order remain source-assumed until runtime validation.
- File collision and temp-file cleanup policy is not implemented.
- Atomic move can fail across filesystems or on unsupported platforms; fallback behavior must be explicit.
- JSON output can expose item/player identifiers and must remain opt-in.
- C# artifact reader/schema validation and warehouse-add byte comparison remain missing.

## Summary Metrics

- Total Java artifacts discovered: 4 grouped artifact rows in this unit
- Total artifacts ported: 0 source artifacts; 1 read-only fastjson activation audit
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 4 grouped rows
- Total blocked artifacts: Java compile validation, JSON writer implementation, runtime artifact validation, C# artifact reader/schema validation, warehouse-add byte comparison
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Add disabled JSON/file writer implementation inside `PetFeedUnusualStorageArtifactCapture.writeArtifact(...)`: serialize the existing schema DTO with fastjson2, write explicit UTF-8 to a same-directory temp file, and move it into place atomically when supported. Keep capture disabled by default and preserve the catch-all writer safety boundary.
