# Phase 6NY Completion Handoff - Decompose Java Capture Contract

Date: May 25, 2026
Unit of Work: UOW-877
Branch: `4.8`
Commit: pending at handoff creation (`[Phase 6][UOW-877] Define Java decompose capture contract`)

## Status

Phase 6 is still in progress. This unit created the fixture contract and artifact schema needed before writing a Java selectable-decompose runtime capture harness.

The contract defines two first Java capture scenarios, their fixture values, expected packet class order, decoded fields, JSON artifact schema, pass/fail criteria, and the implementation checkpoint between Java loopback socket capture, live-server capture, and reflected in-process capture.

## Files Changed

- `docs/Phase-6-Decompose-Java-Capture-Contract.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6NY-Completion.md`

## What Changed

- Added `docs/Phase-6-Decompose-Java-Capture-Contract.md`.
- Defined first Java capture scenarios:
  - `JD-SEL-DEC-001`: selectable decompose source decrement, opcode `236`, source `101 x2`, index `1`, reward `202 x3`.
  - `JD-SEL-DEL-001`: selectable decompose source delete, opcode `236`, source `101 x1`, index `0`, reward `201 x2`.
- Defined capture levels:
  - Level 0 source review
  - Level 1 packet class order
  - Level 2 decoded fields
  - Level 3 optional unencrypted body bytes
  - Level 4 deferred encrypted frame bytes
- Defined shared fixture values, static-data requirements, scenario JSON inputs, decoded packet field requirements, output artifact schema, pass/fail criteria, and implementation checkpoint.
- Documented a newly surfaced parity risk: Java `ItemPacketService.sendItemDeletePacket` emits `SM_CUBE_UPDATE` after `SM_DELETE_ITEM`; existing C# selectable delete observer coverage may not surface that packet.
- Updated `docs/PHASE-6-PROGRESS.md` with Session 877 parity table, risks, metrics, and next recommended UOW.

## Tests

No .NET tests were run because this was a documentation/contract-only unit with no production code or test code changes.

Previous full validation remains from Session 875:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore
```

Result: passed, 1450 tests.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_SELECT_DECOMPOSABLE` | `Aion.GameServer.Network.Aion.ClientPackets.CmSelectDecomposable` | Client Packet Handler | Partial | Regression Tested in C#; Manual Only for contract | Partial Parity | Contract defines two Java runtime capture scenarios for selectable decrement/delete but no Java artifact has been generated. Runtime comparison remains missing. |
| `com.aionemu.gameserver.utils.PacketSendUtility` | `Aion.GameServer.Network.Aion.GameServerConnection.BroadcastItemUsageAnimationAsync` / send helpers | Utility | Partial | Manual Only | Needs Verification | Contract requires empty known-list self-send capture because Java `broadcastPacketAndReceive` sends self first. Broadcast fanout parity remains unverified. |
| `com.aionemu.gameserver.model.items.storage.Storage` | `Aion.GameServer` inventory mutation helpers | Storage | Partial | Manual Only | Needs Verification | Contract requires Java source decrement and delete side effects to be captured through item packet services. Persistence, quest callback, and delete/update packet ordering remain unverified. |
| `com.aionemu.gameserver.services.item.ItemPacketService` | `Aion.GameServer.Services.Items` packet writers | Service / Packet Side Effects | Partial | Manual Only | Needs Verification | Contract explicitly expects `SM_CUBE_UPDATE` after Java delete because `sendItemDeletePacket` sends it. Existing C# observer coverage may not surface that packet; this is a parity risk to investigate. |
| `com.aionemu.gameserver.services.item.ItemService` | `Aion.GameServer.Services.Items.InventoryAddService` / item services | Service | Partial | Manual Only | Needs Verification | Contract fixes reward counts and add type for deterministic Java capture. Runtime reward-add packet bytes and ID allocation remain unverified. |
| `com.aionemu.gameserver.dataholders.DecomposableItemsData` | `Aion.GameServer.Data.StaticData` decomposable item data | Data Holder | Partial | Manual Only | Needs Verification | Contract defines minimal selectable static-data fixture and requires explicit id mapping if real Java XML ids differ. XML load/default parity remains unverified. |

## Remaining Risks

- Java runtime capture remains unimplemented.
- The C# selectable delete observer tests may be missing Java's `SM_CUBE_UPDATE` packet after `SM_DELETE_ITEM`; this needs comparison evidence.
- If Java real XML ids must be used, the simple fixture ids (`101`, `201`, `202`) need explicit mapping.
- DB/DAO and `IDFactory` setup may still push first capture toward live-server fixture.
- Level 3/4 byte capture remains deferred until Java connection/crypt state is controlled.
- Threading and packet-processor ordering remain unverified for any implementation path.

## Summary Metrics

- Total Java artifacts discovered: 6
- Total artifacts ported: 0 code artifacts; 1 capture contract document added
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 6
- Total blocked artifacts: 7 blocked/not-started categories, including Java runtime artifact generation, C# artifact comparison tests, delete-path `SM_CUBE_UPDATE` parity, deterministic Java static-data fixture, Java ID allocation fixture, unencrypted byte capture, and encrypted frame capture
- Estimated overall migration completion: Phase 6 remains about 66% complete

## Next Recommended Unit of Work

Investigate and, if Java source confirms C# is missing it, add focused C# coverage/behavior for Java selectable delete `SM_CUBE_UPDATE` after `SM_DELETE_ITEM`; otherwise document why the C# observer/socket path intentionally excludes it.

Suggested scope:

- Inspect C# `ApplySourceItemMutationAsync` and `SmCubeUpdate`/cube packet support.
- Inspect tests around selectable source delete and normal decompose source delete.
- Compare Java `ItemPacketService.sendItemDeletePacket`:
  - `SM_DELETE_ITEM`
  - `SM_CUBE_UPDATE.cubeSize(storageType, player)`
- Add or update a focused test if C# should emit `SmCubeUpdate`.
- If C# intentionally omits it due missing packet support or observer scope, document that as a parity gap and keep status below verified.

## Safe Parallel Work Candidates

| Candidate | Files | Parallel Safe? | Notes |
|---|---|---|---|
| C# selectable delete cube-update audit | `GameServerConnection.cs`, tests | No | Likely shared handler/test fixture file; do sequentially. |
| Java loopback socket design notes | docs only / read-only Java | Yes as analysis | Can proceed after cube-update gap is understood. |
| Live-server capture runbook | docs only | Yes as analysis | Useful fallback, but does not fix C# gap. |
| Artifact schema fixture directory | docs/parity-artifacts | Maybe | Wait until a real artifact generator exists. |

## Do Not Parallelize

- `GameServerConnectionInventoryExpansionUseItemTests.cs` with any other decompose test edits.
- `GameServerConnection.cs` with any other item-use handler edits.
- Progress and handoff docs.
- Java runtime harness implementation while C# delete-path packet gap is unresolved.

## Resume Checklist

1. Read `docs/csharp-port.md`, orchestration docs, `docs/PHASE-6-PROGRESS.md`, `docs/Phase-6-Java-Decompose-Harness-Feasibility.md`, `docs/Phase-6-Decompose-Java-Capture-Contract.md`, and this handoff.
2. Confirm branch status and latest commit.
3. Run Parallel Work Discovery before selecting subagents.
4. Use Java as source of truth and preserve breadcrumbs.
5. Start with the selectable delete `SM_CUBE_UPDATE` audit.
6. Run focused and full tests for any code changes.
7. Update Migration Parity Table, Remaining Risks, Summary Metrics, and Next Recommended Unit.
8. Create the next handoff and commit the completed unit.
