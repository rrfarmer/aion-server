# Phase 6OH Completion Handoff - Decompose System Message Factory Mapping

Date: May 25, 2026
Unit of Work: UOW-886
Branch: `4.8`
Commit: pending at handoff creation (`[Phase 6][UOW-886] Map decompose system message factories`)

## Status

Phase 6 is still in progress. This unit tightened the selectable-decompose C# JSON projection and guarded Java artifact comparison by adding Java `SM_SYSTEM_MESSAGE` factory-name mapping for the decompose success system message.

No Java runtime artifact was captured in this environment. The comparison guard is ready for future artifacts but has not verified parity.

## Files Changed

- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6OH-Completion.md`

## What Changed

- Reviewed Java `SM_SYSTEM_MESSAGE.STR_UNCOMPRESS_COMPRESSED_ITEM_SUCCEEDED(String)`.
- Confirmed Java emits message id `1400452` for selectable decompose success.
- Added local projection mapping:
  - `1400452` -> `STR_UNCOMPRESS_COMPRESSED_ITEM_SUCCEEDED`
  - `1300447` -> `STR_DECOMPOSE_ITEM_INVENTORY_IS_FULL`
- Updated `CaptureSelectableDecomposeObservationJson_ProjectsContractComparablePackets` to assert the success factory name.
- Updated `CompareSelectableDecomposeJavaArtifacts_WhenPresent_ComparesContractFields` so future Java artifacts must include matching `message_id` and `factory_name` for packet sequence 2.

## Tests

Focused:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests --no-restore
```

Result: passed, 31 tests.

Full:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore
```

Result: passed, 1452 tests.

Updated tests:

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| `CaptureSelectableDecomposeObservationJson_ProjectsContractComparablePackets` | C# selectable projection includes `STR_UNCOMPRESS_COMPRESSED_ITEM_SUCCEEDED`. | Java `SM_SYSTEM_MESSAGE` source reviewed; no runtime artifact yet. |
| `CompareSelectableDecomposeJavaArtifacts_WhenPresent_ComparesContractFields` | Future Java artifacts must include matching `message_id` and `factory_name` for selectable success system messages. | Guard only until Java artifacts exist. |

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage` / decompose projection helper | Server Packet | Partial | Regression Tested; Java Artifact Comparison Guard Added | Partial Parity | Projection now maps `1400452` to `STR_UNCOMPRESS_COMPRESSED_ITEM_SUCCEEDED` and `1300447` to `STR_DECOMPOSE_ITEM_INVENTORY_IS_FULL`. Only these decompose-related factories are mapped; broader message factory coverage remains partial and Java runtime artifacts remain absent. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_SELECT_DECOMPOSABLE` | `Aion.GameServer.Network.Aion.ClientPackets.CmSelectDecomposable` / `GameServerConnection.HandleSelectDecomposableAsync` | Client Packet Handler | Partial | Regression Tested; Java Artifact Comparison Guard Added | Partial Parity | Selectable projection/comparison now requires success system-message factory names. No Java runtime JSON exists yet. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_UPDATE_ITEM` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryUpdateItem` | Server Packet | Partial | Regression Tested; Java Artifact Comparison Guard Added | Partial Parity | Touched only as part of guarded comparison field list context; no new serialization behavior. Full blob and Java byte comparison remain unverified. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_DELETE_ITEM` | `Aion.GameServer.Network.Aion.ServerPackets.SmDeleteItem` | Server Packet | Partial | Regression Tested; Java Artifact Comparison Guard Added | Partial Parity | Touched only as part of guarded comparison field list context; delete type remains guarded for future Java artifacts. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_ADD_ITEM` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryAddItem` | Server Packet | Partial | Regression Tested; Java Artifact Comparison Guard Added | Partial Parity | Touched only as part of projection/comparison context; reward fields remain guarded but not Java-runtime compared. |

## Remaining Risks

- Java runtime capture remains blocked locally because Java 25 JDK and Maven are unavailable; `LoopbackCaptureProof` still has not been compiled or run.
- Java artifact files do not exist yet, so `factory_name` comparison has not been exercised against Java runtime output.
- Only two decompose-related system-message ids are mapped in the helper; broader `SM_SYSTEM_MESSAGE` factory-name projection remains partial.
- Future Java artifacts may use different ids if live XML/static-data mappings change scenario shape; the artifact must document id mappings explicitly.
- Byte-level payload/frame parity, full item-info blob parity, object-id allocation, and live-client behavior remain unverified.

## Summary Metrics

- Total Java artifacts discovered: 5
- Total artifacts ported: 0 production code artifacts; 1 projection/comparison mapping update for decompose system-message factories
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 5
- Total blocked artifacts: 8 blocked/not-started categories, including Java loopback proof validation, live Java JSON artifact generation, fixture SQL/script automation, packet observer implementation, byte capture, broader system-message factory mapping, object-id/id-mapping support, and live-client validation
- Estimated overall migration completion: Phase 6 remains about 66% complete

## Next Recommended Unit of Work

If Java 25/Maven tooling is available, run `LoopbackCaptureProof` or execute the live-server runbook to produce Java JSON artifacts.

If tooling remains blocked, good next units are:

- Draft Java packet-observer design notes for producing Level 2 artifact fields with less manual work.
- Add id-mapping support to the guarded comparison helper if live captures will use real XML ids instead of the logical C# fixture ids.
- Add a docs-only SQL fixture checklist appendix for the live-server capture runbook.

## Safe Parallel Work Candidates

| Candidate | Files | Parallel Safe? | Notes |
|---|---|---|---|
| Java proof validation/fix | `game-server/test/com/aionemu/gameserver/network/aion/LoopbackCaptureProof.java` | No | Requires Java 25/Maven and sequential compile/run feedback. |
| Execute live-server runbook | Java runtime/config/DB only plus artifact files | No | Runtime environment and artifacts should be controlled by one operator. |
| Java packet-observer design notes | docs only | Yes | Can proceed independently if no progress/handoff docs are edited concurrently. |
| Id-mapping comparison support | decompose test file | No | Same shared comparison helper; do sequentially. |
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
6. If still tooling-blocked, choose Java packet-observer design notes, id-mapping support, or SQL fixture appendix work.
7. Run focused and full tests for any C# code changes.
8. Update Migration Parity Table, Remaining Risks, Summary Metrics, and Next Recommended Unit.
9. Create the next handoff and commit the completed unit.
