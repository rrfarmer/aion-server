# Phase 6OF Completion Handoff - Decompose Live-Server Capture Runbook

Date: May 25, 2026
Unit of Work: UOW-884
Branch: `4.8`
Commit: pending at handoff creation (`[Phase 6][UOW-884] Add decompose live capture runbook`)

## Status

Phase 6 is still in progress. This unit added a docs-only live Java server capture runbook for producing selectable-decompose JSON artifacts matching `docs/Phase-6-Decompose-Java-Capture-Contract.md`.

No Java runtime artifact was captured in this environment. Local blockers remain Java 25 JDK, `javac`, and Maven availability.

## Files Changed

- `docs/Phase-6-Decompose-Live-Server-Capture-Runbook.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6OF-Completion.md`

## What Changed

- Added a live-server capture fallback runbook.
- Reused the existing mixed-mode startup scripts from `docs/PHASE-3-MIXED-MODE-VALIDATION.md`.
- Defined fixture and DB setup requirements for:
  - `JD-SEL-DEC-001`
  - `JD-SEL-DEL-001`
- Defined required packet order and decoded fields for Level 1/2 capture.
- Defined artifact paths:
  - `docs/parity-artifacts/java/decompose/selectable/JD-SEL-DEC-001.json`
  - `docs/parity-artifacts/java/decompose/selectable/JD-SEL-DEL-001.json`
- Documented capture implementation options:
  - client-side frame capture
  - targeted Java instrumentation logger
  - server log plus manual decoder fallback
- Added pass/fail gates and stop conditions.

## Tests

No tests were run because this was a documentation-only runbook unit.

Latest validation remains UOW-883:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests --no-restore
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore
```

Results: focused 30 tests passed; full 1451 tests passed.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_SELECT_DECOMPOSABLE` | `Aion.GameServer.Network.Aion.ClientPackets.CmSelectDecomposable` / C# JSON projection helper | Client Packet Handler | Partial | Manual Only for runbook; Regression Tested in C# projection | Needs Verification | Runbook defines live Java capture steps for both selectable scenarios. No Java artifact was captured in this unit. |
| `com.aionemu.gameserver.utils.PacketSendUtility` | `Aion.GameServer.Network.Aion.GameServerConnection` send observer / projection helper | Utility | Partial | Manual Only | Needs Verification | Runbook recommends self-send isolation and optional Java instrumentation at send boundaries. Broadcast/threading behavior remains uncaptured. |
| `com.aionemu.gameserver.model.items.storage.Storage` | `Aion.GameServer` inventory mutation helpers | Storage | Partial | Manual Only | Needs Verification | Runbook requires source decrement/delete side effects to be captured from Java runtime. Persistence and quest callback behavior remain outside first artifacts. |
| `com.aionemu.gameserver.services.item.ItemPacketService` | `Aion.GameServer.Services.Items` packet writers / server packets | Service / Packet Side Effects | Partial | Manual Only | Needs Verification | Runbook requires Java `SM_CUBE_UPDATE` after delete and source/reward packet fields. No runtime artifact yet. |
| `com.aionemu.gameserver.services.item.ItemService` | `Aion.GameServer.Network.Aion.GameServerConnection.SendDecomposeRewardItemsAsync` | Service | Partial | Manual Only | Needs Verification | Runbook covers deterministic reward counts and id mapping requirements. Java reward object-id allocation remains uncaptured. |
| `com.aionemu.gameserver.dataholders.DecomposableItemsData` | `Aion.GameServer.Data.StaticData` decomposable item data | Data Holder | Partial | Manual Only | Needs Verification | Runbook allows test ids or real XML ids with explicit mapping. Java XML/static-data runtime parity remains unverified. |

## Remaining Risks

- Local Java 25/Maven tooling remains unavailable, so the runbook was not executed here.
- Live client setup may need account/character creation, client patching, and DB fixture work not fully scripted.
- Real Java XML ids may differ from C# fixture ids and require explicit mapping.
- Packet observation may need temporary Java diagnostics; any such patch must be isolated and documented.
- Login/event systems can emit unrelated item packets and pollute capture order.
- Byte-level capture remains deferred.

## Summary Metrics

- Total Java artifacts discovered: 6
- Total artifacts ported: 0 production code artifacts; 1 live-server capture runbook added
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 6
- Total blocked artifacts: 8 blocked/not-started categories, including Java loopback proof validation, live Java JSON artifact generation, fixture SQL/script automation, packet observer implementation, byte capture, C# Java-artifact comparison tests, object-id parity, and live-client validation
- Estimated overall migration completion: Phase 6 remains about 66% complete

## Next Recommended Unit of Work

If Java 25/Maven tooling is available, run `LoopbackCaptureProof` or execute the live-server runbook to generate the first Java JSON artifacts.

If tooling remains blocked, add a guarded C# comparison test that looks for future Java artifacts at `docs/parity-artifacts/java/decompose/selectable/*.json`, reports absence as an explicit needs-verification condition, and compares packet order/decoded fields when the files exist.

## Safe Parallel Work Candidates

| Candidate | Files | Parallel Safe? | Notes |
|---|---|---|---|
| Java proof validation/fix | `game-server/test/com/aionemu/gameserver/network/aion/LoopbackCaptureProof.java` | No | Requires Java 25/Maven and sequential compile/run feedback. |
| Execute live-server runbook | Java runtime/config/DB only plus artifact files | No | Runtime environment and artifacts should be controlled by one operator. |
| Guarded C# Java-artifact comparison test | decompose test file plus future artifact paths | No | Shared projection helper/test fixture; do sequentially. |
| Packet observer design notes | docs only | Yes | Useful if deciding where Java instrumentation should hook packet sends. |

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
6. If still tooling-blocked, add guarded future-artifact comparison tests or another isolated comparison-readiness unit.
7. Run focused and full tests for any C# code changes.
8. Update Migration Parity Table, Remaining Risks, Summary Metrics, and Next Recommended Unit.
9. Create the next handoff and commit the completed unit.
