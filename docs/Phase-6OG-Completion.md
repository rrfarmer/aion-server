# Phase 6OG Completion Handoff - Guarded Decompose Java Artifact Comparison

Date: May 25, 2026
Unit of Work: UOW-885
Branch: `4.8`
Commit: pending at handoff creation (`[Phase 6][UOW-885] Add guarded decompose artifact comparison`)

## Status

Phase 6 is still in progress. This unit added a guarded C# comparison test for future Java selectable-decompose JSON artifacts.

The Java artifacts are still absent locally, so this does not verify parity. The new test logs an explicit needs-verification message when the Java files are missing, and it will compare packet order plus selected decoded fields once Java runtime artifacts exist.

## Files Changed

- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6OG-Completion.md`

## What Changed

- Added `CompareSelectableDecomposeJavaArtifacts_WhenPresent_ComparesContractFields`.
- Added repository-root discovery for locating future artifact files from the test output directory.
- Added guarded lookup for:
  - `docs/parity-artifacts/java/decompose/selectable/JD-SEL-DEC-001.json`
  - `docs/parity-artifacts/java/decompose/selectable/JD-SEL-DEL-001.json`
- Missing artifacts now produce explicit xUnit output:
  - `Needs Verification: Java selectable-decompose artifacts are not present yet.`
- When artifacts are present, the test requires:
  - `capture_method = live-java-server`
  - matching scenario id
  - matching client opcode
  - matching select index and ignored dword
  - matching Java packet class order
  - matching selected decoded fields for decrement and delete scenarios

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

Added test:

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| `CompareSelectableDecomposeJavaArtifacts_WhenPresent_ComparesContractFields` | Guards future Java JSON artifact comparison and compares packet order plus selected decoded fields when files are present. | No Java artifact exists yet; comparison infrastructure only. |

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_SELECT_DECOMPOSABLE` | `Aion.GameServer.Network.Aion.ClientPackets.CmSelectDecomposable` / `GameServerConnection.HandleSelectDecomposableAsync` | Client Packet Handler | Partial | Regression Tested; Java Artifact Comparison Guard Added | Partial Parity | Test now has a future Java-artifact comparison path for both selectable scenarios, but artifacts are absent locally. Missing Java runtime JSON prevents verified parity. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ITEM_USAGE_ANIMATION` | `Aion.GameServer.Network.Aion.ServerPackets.SmItemUsageAnimation` | Server Packet | Partial | Regression Tested; Java Artifact Comparison Guard Added | Partial Parity | Guard compares usage item id/time/end/unknown3 when Java artifacts exist. Constructor/default behavior remains unverified until runtime artifacts are present. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage` | Server Packet | Partial | Regression Tested | Needs Verification | Artifact comparison currently validates packet class order but not factory-name/message-id parity. Java factory mapping remains a known gap. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_UPDATE_ITEM` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryUpdateItem` | Server Packet | Partial | Regression Tested; Java Artifact Comparison Guard Added | Partial Parity | Guard compares decrement source count and update type mask/name when Java artifacts exist. Broader blob fields and byte-level parity remain unverified. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_DELETE_ITEM` | `Aion.GameServer.Network.Aion.ServerPackets.SmDeleteItem` | Server Packet | Partial | Regression Tested; Java Artifact Comparison Guard Added | Partial Parity | Guard compares delete type mask/name for delete scenario when Java artifacts exist. Java packet bytes remain absent. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_CUBE_UPDATE` | `Aion.GameServer.Network.Aion.ServerPackets.SmCubeUpdate` | Server Packet | Partial | Regression Tested; Java Artifact Comparison Guard Added | Partial Parity | Guard compares action, storage, item count, and expansion fields for delete scenario when Java artifacts exist. Other cube callers remain unverified. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SECONDARY_SHOW_DECOMPOSABLE` | `Aion.GameServer.Network.Aion.ServerPackets.SmSecondaryShowDecomposable` | Server Packet | Partial | Regression Tested; Java Artifact Comparison Guard Added | Partial Parity | Guard compares secondary reward count when Java artifacts exist. Byte-level Java output remains missing. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_ADD_ITEM` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryAddItem` | Server Packet | Partial | Regression Tested; Java Artifact Comparison Guard Added | Partial Parity | Guard compares add type, item count, item id, reward count, slot, and cloth flag when Java artifacts exist. Object-id and bytes remain unverified. |
| `com.aionemu.gameserver.network.aion.iteminfo.ItemInfoBlob` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryInfo.WriteItemInfoBlob` | Serialization Utility | Partial | Regression Tested; Java Artifact Comparison Guard Added | Partial Parity | Guard compares only decoded general-info counts surfaced by projection. Full blob serialization, optional entries, and equipment/temporary-data fields remain unverified. |

## Remaining Risks

- Java runtime capture remains blocked locally because Java 25 JDK and Maven are unavailable; `LoopbackCaptureProof` still has not been compiled or run.
- Java artifact files do not exist yet, so the guarded comparison path has not compared Java output.
- The missing-artifact branch passes while logging needs-verification output; docs remain the authority that parity is not verified.
- `SM_SYSTEM_MESSAGE` factory-name/message-id mapping remains outside this unit.
- Full item-info blob, object-id allocation, byte-level payload/frame parity, and live-client behavior remain unverified.
- Future real Java XML id mapping may require enhancing the comparison helper before mapped artifacts can pass.

## Summary Metrics

- Total Java artifacts discovered: 9
- Total artifacts ported: 0 production code artifacts; 1 guarded Java-artifact comparison test added
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 9
- Total blocked artifacts: 8 blocked/not-started categories, including Java loopback proof validation, live Java JSON artifact generation, fixture SQL/script automation, packet observer implementation, byte capture, system-message factory mapping, object-id/id-mapping support, and live-client validation
- Estimated overall migration completion: Phase 6 remains about 66% complete

## Next Recommended Unit of Work

If Java 25/Maven tooling is available, run `LoopbackCaptureProof` or execute the live-server runbook to produce the Java JSON artifacts.

If tooling remains blocked, good next units are:

- Add system-message factory-name mapping to the C# projection/comparison.
- Draft Java packet-observer design notes for producing Level 2 artifact fields with less manual work.
- Add id-mapping support to the comparison helper if live Java captures will use real XML ids instead of the logical C# fixture ids.

## Safe Parallel Work Candidates

| Candidate | Files | Parallel Safe? | Notes |
|---|---|---|---|
| Java proof validation/fix | `game-server/test/com/aionemu/gameserver/network/aion/LoopbackCaptureProof.java` | No | Requires Java 25/Maven and sequential compile/run feedback. |
| Execute live-server runbook | Java runtime/config/DB only plus artifact files | No | Runtime environment and artifacts should be controlled by one operator. |
| System-message factory mapping | decompose test file | No | Same shared projection helper; do sequentially. |
| Java packet-observer design notes | docs only | Yes | Can proceed independently if no progress/handoff docs are edited concurrently. |
| Id-mapping comparison support | decompose test file | No | Same shared comparison helper; do sequentially. |

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
6. If still tooling-blocked, choose system-message mapping, Java observer design notes, or id-mapping support.
7. Run focused and full tests for any C# code changes.
8. Update Migration Parity Table, Remaining Risks, Summary Metrics, and Next Recommended Unit.
9. Create the next handoff and commit the completed unit.
