# Phase 6OI Completion Handoff - Decompose Artifact Id Mapping Support

Date: May 25, 2026
Unit of Work: UOW-887
Branch: `4.8`
Commit: pending at handoff creation (`[Phase 6][UOW-887] Add decompose artifact id mapping`)

## Status

Phase 6 is still in progress. This unit added explicit id-mapping support to the guarded selectable-decompose Java artifact comparison.

No Java runtime artifact was captured in this environment. The comparison helper can now handle Java live-server artifacts that use real XML item ids instead of the logical C# fixture ids, but only when the artifact declares the mapping in `fixture.id_mapping`.

## Files Changed

- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6OI-Completion.md`

## What Changed

- Added `CompareSelectableDecomposeJavaArtifacts_WithDeclaredIdMapping_NormalizesMappedIds`.
- Added helper logic that reads `fixture.id_mapping` from a Java artifact.
- Any numeric Java decoded field value matching a declared `java_*` mapping is normalized to the corresponding `logical_*` value before comparison.
- Added a synthetic live-server-shaped artifact regression with:
  - `logical_source_item_id = 101`
  - `java_source_item_id = 188052590`
  - `logical_reward_index_1 = 202`
  - `java_reward_index_1 = 188052592`
- The synthetic artifact remaps source usage and reward add `item_id` fields and verifies the comparison still passes.
- Unmapped values still compare exactly.

## Tests

Focused:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests --no-restore
```

Result: passed, 32 tests.

Full:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore
```

Result: passed, 1453 tests.

Added test:

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| `CompareSelectableDecomposeJavaArtifacts_WithDeclaredIdMapping_NormalizesMappedIds` | Guarded comparison accepts different Java item ids only when `fixture.id_mapping` explicitly maps them to logical C# fixture ids. | Uses synthetic contract-shaped JSON; no Java runtime artifact yet. |

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.dataholders.DecomposableItemsData` | `Aion.GameServer.Data.StaticData` / decompose artifact comparison helper | Data Holder | Partial | Regression Tested for comparison helper | Needs Verification | Comparison can now normalize explicitly declared Java real XML ids to logical fixture ids. No Java runtime XML capture exists yet, and mappings must be supplied by artifacts. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_SELECT_DECOMPOSABLE` | `Aion.GameServer.Network.Aion.ClientPackets.CmSelectDecomposable` / guarded comparison helper | Client Packet Handler | Partial | Regression Tested; Java Artifact Comparison Guard Added | Partial Parity | Selectable artifact comparison can now handle mapped source/reward item ids. Java runtime JSON is still absent. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ITEM_USAGE_ANIMATION` | `Aion.GameServer.Network.Aion.ServerPackets.SmItemUsageAnimation` | Server Packet | Partial | Regression Tested; Java Artifact Comparison Guard Added | Partial Parity | Mapped Java source `item_id` can be normalized before comparing item-use fields. Constructor/default behavior and Java bytes remain unverified. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_ADD_ITEM` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryAddItem` | Server Packet | Partial | Regression Tested; Java Artifact Comparison Guard Added | Partial Parity | Mapped Java reward `item_id` can be normalized before comparing reward-add fields. Object ids, full blob fields, and runtime bytes remain unverified. |
| `com.aionemu.gameserver.network.aion.iteminfo.ItemInfoBlob` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryInfo.WriteItemInfoBlob` | Serialization Utility | Partial | Regression Tested; Java Artifact Comparison Guard Added | Partial Parity | Touched only through mapped reward/source comparison context. Full blob serialization remains shallow and unverified. |

## Remaining Risks

- Java runtime capture remains blocked locally because Java 25 JDK and Maven are unavailable; `LoopbackCaptureProof` still has not been compiled or run.
- Java artifact files do not exist yet, so id mapping has not been exercised against Java runtime output.
- The id-mapping normalization is intentionally numeric and broad; future artifacts should keep mappings precise to avoid masking unrelated id mismatches.
- Object-id allocation mapping remains unverified because current guarded field list does not compare reward object ids.
- Byte-level payload/frame parity, full item-info blob parity, and live-client behavior remain unverified.

## Summary Metrics

- Total Java artifacts discovered: 5
- Total artifacts ported: 0 production code artifacts; 1 id-mapping normalization path added to guarded comparison tests
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 5
- Total blocked artifacts: 8 blocked/not-started categories, including Java loopback proof validation, live Java JSON artifact generation, fixture SQL/script automation, packet observer implementation, byte capture, object-id mapping/comparison, full item-info blob comparison, and live-client validation
- Estimated overall migration completion: Phase 6 remains about 66% complete

## Next Recommended Unit of Work

If Java 25/Maven tooling is available, run `LoopbackCaptureProof` or execute the live-server runbook to produce Java JSON artifacts.

If tooling remains blocked, good next units are:

- Draft Java packet-observer design notes for producing Level 2 artifact fields with less manual work.
- Add object-id mapping/comparison support once the Java artifact format for generated reward object ids is settled.
- Add a docs-only SQL fixture checklist appendix for the live-server capture runbook.

## Safe Parallel Work Candidates

| Candidate | Files | Parallel Safe? | Notes |
|---|---|---|---|
| Java proof validation/fix | `game-server/test/com/aionemu/gameserver/network/aion/LoopbackCaptureProof.java` | No | Requires Java 25/Maven and sequential compile/run feedback. |
| Execute live-server runbook | Java runtime/config/DB only plus artifact files | No | Runtime environment and artifacts should be controlled by one operator. |
| Java packet-observer design notes | docs only | Yes | Can proceed independently if no progress/handoff docs are edited concurrently. |
| Object-id mapping support | decompose test file | No | Same shared comparison helper; do sequentially. |
| SQL fixture appendix | docs only | Yes | Useful if no Java tooling is available. |

## Do Not Parallelize

- `GameServerConnectionInventoryExpansionUseItemTests.cs` with other decompose/projection edits.
- Java runtime capture with Java proof harness edits.
- Progress and handoff docs.

## Resume Checklist

1. Read `docs/csharp-port.md`, orchestration docs, `docs/PHASE-6-PROGRESS.md`, `docs/Phase-6-Decompose-Java-Capture-Contract.md`, `docs/Phase-6-Decompose-Live-Server-Capture-Runbook.md`, `docs/Phase-6-Java-Loopback-Capture-Design.md`, and this handoff.
2. Confirm branch status and latest commit.
3. Run Parallel Work Discovery before selecting subagents.
4. Use Java as source of truth and preserve breadcrumbs.
5. Prefer Java proof/runbook execution if Java 25/Maven tooling is available.
6. If still tooling-blocked, choose Java packet-observer design notes, object-id mapping support, or SQL fixture appendix work.
7. Run focused and full tests for any C# code changes.
8. Update Migration Parity Table, Remaining Risks, Summary Metrics, and Next Recommended Unit.
9. Create the next handoff and commit the completed unit.
