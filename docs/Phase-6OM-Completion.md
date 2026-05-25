# Phase 6OM Completion Handoff - Decompose Reward-Add Cube Update Diagnostic

Date: May 25, 2026
Unit of Work: UOW-891
Branch: `4.8`
Commit: pending at handoff creation (`[Phase 6][UOW-891] Diagnose decompose trailing cube update`)

## Status

Phase 6 is still in progress. This unit added a guarded comparison diagnostic for the possible Java reward-add trailing `SM_CUBE_UPDATE` in selectable-decompose Java artifacts.

No Java runtime artifact was captured in this environment. The diagnostic does not resolve the parity gap. It makes future Java artifacts fail with a targeted message if they include Java source-reviewed `ItemPacketService.sendStorageUpdatePacket` behavior that C# currently does not emit in the same packet order.

## Files Changed

- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6OM-Completion.md`

## What Changed

- Added `CompareSelectableDecomposeJavaArtifacts_WithJavaRewardAddTrailingCubeUpdate_ReportsParityGap`.
- Added a synthetic Java-shaped artifact path that appends `SM_CUBE_UPDATE` after reward `SM_INVENTORY_ADD_ITEM`.
- Updated `AssertSelectableDecomposeObservationMatchesJavaArtifact` to detect when Java packet order equals the current C# order plus a reward-add trailing `SM_CUBE_UPDATE`.
- The comparison now throws a targeted `InvalidOperationException` explaining that Java `ItemPacketService.sendStorageUpdatePacket` sends `SM_INVENTORY_ADD_ITEM` and then `SM_CUBE_UPDATE` for cube storage.
- The extra packet is not ignored, normalized, or accepted. If a real Java artifact contains it, C# parity must be fixed or an intentional difference must be documented.
- No production Java/C# code changed.

## Tests

Focused:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests --no-restore
```

Result: passed, 34 tests.

Full:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore
```

Result: passed, 1455 tests.

Added/updated tests:

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| `CompareSelectableDecomposeJavaArtifacts_WithJavaRewardAddTrailingCubeUpdate_ReportsParityGap` | Guarded comparison produces a targeted parity-gap message for Java artifacts with `SM_CUBE_UPDATE` after reward add. | Uses synthetic contract-shaped JSON plus source-reviewed Java `ItemPacketService.sendStorageUpdatePacket`; no Java runtime artifact yet. |

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.item.ItemPacketService` | `Aion.GameServer.Services.Items` packet writers / guarded comparison helper | Service / Packet Side Effects | Partial | Regression Tested for diagnostic; Java Artifact Comparison Guard Added | Needs Verification | Java source shows cube storage add sends `SM_INVENTORY_ADD_ITEM` then `SM_CUBE_UPDATE`. The guarded comparison now reports that sequence as a parity gap instead of silently accepting it. Real Java artifact still absent. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_CUBE_UPDATE` | `Aion.GameServer.Network.Aion.ServerPackets.SmCubeUpdate` | Server Packet | Partial | Regression Tested for diagnostic; Java Artifact Comparison Guard Added | Partial Parity | Diagnostic recognizes a reward-add trailing cube update after reward add as source-reviewed Java behavior needing C# follow-up. It does not verify Java runtime bytes, fields, or whether selectable reward add emits it in a live run. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_ADD_ITEM` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryAddItem` | Server Packet | Partial | Regression Tested for diagnostic; Java Artifact Comparison Guard Added | Partial Parity | Diagnostic checks packet-order context after reward add. Reward item fields remain guarded by comparison helper; full blob and byte-level parity remain unverified. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_SELECT_DECOMPOSABLE` | `Aion.GameServer.Network.Aion.ClientPackets.CmSelectDecomposable` / guarded comparison helper | Client Packet Handler | Partial | Regression Tested; Java Artifact Comparison Guard Added | Partial Parity | Future selectable Java artifacts with reward-add trailing cube update now produce a clear gap message. Java runtime handler output remains uncaptured. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `CompareSelectableDecomposeJavaArtifacts_WithJavaRewardAddTrailingCubeUpdate_ReportsParityGap` | Regression / Comparison Readiness | Java `ItemPacketService.sendStorageUpdatePacket` source review | Proves the guarded comparison reports a targeted parity gap when a Java-shaped artifact includes `SM_CUBE_UPDATE` after reward `SM_INVENTORY_ADD_ITEM`. | Synthetic C# JSON artifact shaped like future Java output and source-reviewed Java packet service behavior. | Does not compare real Java runtime artifacts; does not implement the trailing cube update in C#; does not verify byte fields. |

## Remaining Risks

- Java runtime capture remains blocked locally because Java 25/Maven/live-server tooling is unavailable.
- No Java artifact exists yet, so the reward-add trailing cube update has source-review evidence but no runtime confirmation for selectable decompose.
- C# still may need to emit `SM_CUBE_UPDATE` after reward add if runtime Java artifacts confirm the source-reviewed sequence.
- Full item-info blob, byte-level payload/frame parity, Java `IDFactory` allocation, persistence, dispatcher ordering, and live-client behavior remain unverified.

## Summary Metrics

- Total Java artifacts discovered: 4
- Total artifacts ported: 0 production code artifacts; 1 guarded comparison diagnostic added
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 4
- Total blocked artifacts: 8 blocked/not-started categories, including Java observer implementation, Java runtime artifact generation, Java loopback proof validation, real template-id selection, SQL fixture execution, reward-add trailing cube update implementation decision, byte capture, and live-client validation
- Estimated overall migration completion: Phase 6 remains about 66% complete

## Next Recommended Unit of Work

If Java 25/Maven tooling is available, execute the live-server runbook with:

- `docs/Phase-6-Decompose-Java-Capture-Contract.md`
- `docs/Phase-6-Decompose-Live-Server-Capture-Runbook.md`
- `docs/Phase-6-Decompose-Java-Packet-Observer-Design.md`
- `docs/Phase-6-Decompose-Live-Server-SQL-Fixture-Appendix.md`

Target artifacts:

- `docs/parity-artifacts/java/decompose/selectable/JD-SEL-DEC-001.json`
- `docs/parity-artifacts/java/decompose/selectable/JD-SEL-DEL-001.json`

If tooling remains blocked, good next units are:

- Audit real Java decomposable XML data to pick deterministic source/reward template ids for live-server capture.
- Continue an isolated non-decompose Phase 6 gameplay slice that avoids shared decompose comparison helpers.
- If productively small, implement C# reward-add trailing `SM_CUBE_UPDATE` only after Java runtime artifact evidence confirms the sequence.

## Safe Parallel Work Candidates

| Candidate | Files | Parallel Safe? | Notes |
|---|---|---|---|
| Java observer/runtime capture | Java diagnostic patch plus artifact files | No | Runtime capture should be controlled by one owner. |
| Real XML deterministic template-id audit | Java XML/static-data reads; docs notes | Yes | Read-only analysis can be parallelized with unrelated implementation work. |
| Non-decompose gameplay slice | isolated code/test files | Maybe | Only if it avoids shared decompose comparison helpers and progress docs. |
| Reward-add trailing cube implementation | C# connection/packet tests | No | Should wait for runtime artifact evidence and be done sequentially. |

## Do Not Parallelize

- Java observer implementation with live-server artifact capture unless one owner controls both.
- `GameServerConnectionInventoryExpansionUseItemTests.cs` with other decompose/projection edits.
- Progress and handoff docs.

## Resume Checklist

1. Read `docs/csharp-port.md`, orchestration docs, `docs/PHASE-6-PROGRESS.md`, `docs/Phase-6-Decompose-Java-Capture-Contract.md`, `docs/Phase-6-Decompose-Live-Server-Capture-Runbook.md`, `docs/Phase-6-Java-Loopback-Capture-Design.md`, `docs/Phase-6-Decompose-Java-Packet-Observer-Design.md`, `docs/Phase-6-Decompose-Live-Server-SQL-Fixture-Appendix.md`, and this handoff.
2. Confirm branch status and latest commit.
3. Run Parallel Work Discovery before selecting subagents.
4. Prefer Java observer/runtime artifact work if Java 25/Maven tooling is available.
5. If still tooling-blocked, choose real XML template-id audit or an isolated non-decompose Phase 6 gameplay slice.
6. Run focused and full tests for any C# code changes.
7. Update Migration Parity Table, Remaining Risks, Summary Metrics, and Next Recommended Unit.
8. Create the next handoff and commit the completed unit.
