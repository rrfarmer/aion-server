# Phase 6 - Pet Feed Unusual Storage Fastjson Writer Audit

Date: May 27, 2026
Unit of Work: UOW-1361

## Scope

This unit performs a read-only audit for future deterministic JSON artifact writing. No Java or C# source behavior was changed.

Files/source audited:

- `commons/pom.xml`
- `login-server/src/com/aionemu/loginserver/utils/ExternalAuth.java`
- `commons/src/com/aionemu/commons/logging/DiscordChannelAppender.java`
- local Maven cache: `fastjson2-2.0.60.jar`
- `game-server/src/com/aionemu/gameserver/services/toypet/PetFeedUnusualStorageArtifactCapture.java`

## Findings

- `commons/pom.xml` already provides `com.alibaba.fastjson2:fastjson2:2.0.60`.
- Existing fastjson2 usage is transient network/logging JSON only:
  - `ExternalAuth` uses `JSON.toJSONString(Map.of(...))` and `JSON.parseObject(...)`.
  - `DiscordChannelAppender` uses `JSON.toJSONBytes(Map.of(...))`.
- These existing usages do not establish deterministic artifact conventions. `Map.of(...)` ordering should not be treated as a stable artifact precedent.
- The local fastjson2 jar contains `JSON`, `JSONWriter`, and `JSONWriter$Feature`.
- Local class-string inspection found deterministic/shape-related feature names including:
  - `MapSortField`
  - `SortMapEntriesByKeys`
  - `WriteMapNullValue`
  - `WriteNulls`
  - `FieldBased`
  - `PrettyFormat`
  - `PrettyFormatWith2Space`
  - `PrettyFormatWith4Space`
- `jar` and `javap` are not available on PATH locally, so exact method signatures were not disassembled in this unit.

## Writer Safety Requirements

Future artifact writing should be implemented only after these choices are fixed in code and docs:

- Use a dedicated DTO shape, not ad hoc `Map.of(...)` payloads.
- Use deterministic field order. Prefer explicit DTO field order and a documented fastjson2 feature set; if maps are used anywhere, require sorted map entries.
- Preserve null fields when absence versus null matters for parity.
- Write UTF-8 bytes explicitly.
- Use stable filenames that include scenario, player object id, storage id/ordinal, item object id, and capture timestamp or monotonic sequence.
- Write to a configured output directory only after validating it is not empty and not a production data/log directory.
- Use atomic file creation where practical: write to a temporary file first, then rename.
- Do not write from the packet serialization hot path. Use a bounded queue and a dedicated writer worker.
- Define queue-full behavior before enabling output. Dropping the newest artifact with an explicit dropped-count metric is safer than blocking packet serialization.
- Keep capture disabled by default and require explicit config opt-in.
- Keep raw/canonical packet bytes encoded as hex or base64 consistently; do not depend on platform default charset.

## Boundaries Preserved

- No artifact writer was added.
- No queue or worker was added.
- No observer install, file output, byte copying, or JSON serialization was added.
- No C# reader/schema behavior changed.
- No parity was promoted to verified.

## Validation

- Ran `git diff --check`.
- Java compile validation was not required for this docs-only audit and remains blocked locally by missing Maven/JDK command-line tooling.
- No Java runtime artifacts were generated.

## Migration Parity Table - UOW-1361

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `commons/pom.xml` dependency `com.alibaba.fastjson2:fastjson2:2.0.60` | future C# artifact JSON reader/schema tests | Dependency | Complete | Manual Only | Needs Verification | Dependency exists in commons and can be reused by game-server transitively, but deterministic writer feature selection is not implemented yet. |
| `com.aionemu.loginserver.utils.ExternalAuth` | none | Utility / Existing JSON Usage | Complete | Manual Only | Needs Verification | Uses fastjson2 for transient HTTP auth JSON via `Map.of(...)`; not a deterministic artifact-writing precedent. |
| `com.aionemu.commons.logging.DiscordChannelAppender` | none | Logging Utility / Existing JSON Usage | Complete | Manual Only | Needs Verification | Uses fastjson2 for transient Discord webhook JSON via `Map.of(...)`; not a deterministic artifact-writing precedent. |
| `com.aionemu.gameserver.services.toypet.PetFeedUnusualStorageArtifactCapture` | future C# artifact/schema reader | Utility / Capture Context | Partial | Manual Only | Needs Verification | Future writer should use dedicated DTOs, deterministic features, bounded queueing, and atomic file output. No writer exists yet. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| None | Read-only Audit | Existing dependency and source review | Documents deterministic JSON writer prerequisites before output is added. | Source/cache audit only. | No writer implementation, runtime artifact, C# reader, or byte comparison validation. |

## Remaining Risks

- Exact fastjson2 API signatures were not disassembled because `jar`/`javap` are unavailable on PATH.
- Deterministic field ordering still needs implementation and validation once writer code exists.
- Writer queue failure/drop behavior is not implemented.
- Atomic file write behavior and path validation are not implemented.
- C# artifact reader/schema validation remains missing.

## Summary Metrics

- Total Java artifacts discovered: 4 grouped artifact rows in this unit
- Total artifacts ported: 0 source artifacts; 1 audit document
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 4 grouped rows
- Total blocked artifacts: deterministic writer implementation, queue worker, atomic output, C# artifact reader/schema validation, Java runtime artifacts
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Add a disabled bounded writer queue shell to `PetFeedUnusualStorageArtifactCapture`: queue type, max queued artifact bound from config, dropped-count tracking, and no-op writer boundary only. Do not create files, serialize JSON, retain bytes, or install the observer yet.
