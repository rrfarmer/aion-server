# Phase 6MT Completion Handoff - Positive-Time Item Use State Trace

Date: May 24, 2026
Unit of Work: UOW-846
Branch: `4.8`
Commit: pending at handoff creation (`[Phase 6][UOW-846] Trace positive item use state`)

## Status

Phase 6 is still in progress. This unit added a non-live metadata trace for positive-time `SmItemUsageAnimation` state behavior. It documents the key mismatch: Java mutates `Player.usingItem` during packet serialization, while C# sets `Player.UsingItemObjectId` after the start animation is sent in scheduled item-use paths.

No runtime bridge or broad `GameServerConnection` refactor was added.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/PlayerSummonSkillExecutionService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerSummonSkillExecutionServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6MT-Completion.md`

## What Changed

- Added `ProjectMercenaryNpcSkillItemUsageStateTrace`.
- Added `PlayerSummonKnownObjectNpcSkillItemUsageStateTrace`, path records, and enums for:
  - Java/C# state steps
  - mutation timing
  - C# cleanup policy
  - Java null policy
  - positive-time path kind
  - send kind
  - cancel message category
- Captured Java positive-time packet-write state steps:
  - serialize positive-time `SM_ITEM_USAGE_ANIMATION`
  - resolve world player
  - resolve inventory item
  - set Java `Player.usingItem` during writer execution
- Captured C# scheduled delayed item-use steps:
  - send positive-time `SmItemUsageAnimation`
  - call `SchedulePendingItemUseAsync`
  - set `Player.UsingItemObjectId` after send
  - cleanup clears matching pending item
  - cancel sends `end=3` style animation through the pending-item cancel helper
- Modeled representative C# positive-time paths:
  - stigma charge
  - enchant/socket
  - godstone socket
  - soul bind
  - assembly
  - animation add
  - ride/toy pet
  - polish/charge

## Tests

Focused:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "PlayerSummonSkillExecutionServiceTests|StaticDataNpcSkillTests"
```

Result: passed, 71 tests.

Full:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj
```

Result: passed, 1421 tests.

New/updated test:

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| `PlayerSummonSkillExecutionServiceTests.ProjectMercenaryNpcSkillItemUsageStateTrace_MapsPositiveTimeUsingItemTiming` | Java positive-time writer steps, C# send/schedule/clear/cancel steps, mutation timing mismatch, delayed path coverage, send kind, cancel state, targeted cancel flags, and null policy metadata. | Source-derived Java/C# review; no runtime Java comparison or live scheduler execution. |

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ITEM_USAGE_ANIMATION` | `PlayerSummonKnownObjectNpcSkillItemUsageStateTrace` / `SmItemUsageAnimation` | Packet / Runtime Side-Effect Metadata | Partial | Unit Tested as metadata | Needs Verification | Java write-time `usingItem` mutation and C# scheduler-side mismatch are explicitly recorded. No runtime bridge. |
| `com.aionemu.gameserver.world.World` | null-policy metadata | Runtime Lookup Dependency | Not Started | Unit Tested as metadata | Needs Verification | Java missing-player lookup can throw; C# does not perform packet-time lookup. |
| `com.aionemu.gameserver.model.gameobjects.player.Player` | `Player.UsingItemObjectId` / state trace | Player State Dependency | Partial | Unit Tested as metadata | Needs Verification | Java stores `Item`; C# stores object id after send. Timing/type semantics differ. |
| `com.aionemu.gameserver.model.gameobjects.player.Inventory` | null-policy metadata | Inventory Dependency | Not Started | Unit Tested as metadata | Needs Verification | Java item lookup may return null and clear `usingItem`; no live C# lookup. |
| `com.aionemu.gameserver.controllers.PlayerController` | pending-item cleanup/cancel trace | Runtime / Cancel Caller | Partial | Unit Tested as metadata | Needs Verification | Java cancel clears `usingItem`; C# cleanup clears matching `UsingItemObjectId`. Runtime parity unverified. |
| `com.aionemu.gameserver.model.templates.item.actions.AssemblyItemAction` | `Assembly` state path | Dynamic Item Action Caller | Partial | Unit Tested as metadata | Needs Verification | Metadata only; no runtime scheduling/inventory mutation validation. |
| `com.aionemu.gameserver.model.templates.item.actions.AnimationAddAction` | `AnimationAdd` state path | Dynamic Item Action Caller | Partial | Unit Tested as metadata | Needs Verification | Metadata only; self-only start animation and scheduler-side state recorded. |
| `com.aionemu.gameserver.model.templates.item.actions.RideAction` / `ToyPetSpawnAction` | `RideOrToyPet` state path | Dynamic Item Action Caller | Partial | Unit Tested as metadata | Needs Verification | Metadata only; emotion preserve and pet mutation behavior remain unverified. |
| `com.aionemu.gameserver.model.templates.item.actions.PolishAction` / `ChargeAction` | `PolishOrCharge` state path | Dynamic Item Action Caller | Partial | Unit Tested as metadata | Needs Verification | Metadata only; charge/polish mutation and cooldown behavior missing. |
| `com.aionemu.gameserver.services.StigmaService` | `StigmaCharge` state path | Service / Packet Caller | Partial | Unit Tested as metadata | Needs Verification | Metadata only; live stigma charge mutation unverified. |
| `com.aionemu.gameserver.services.item.ItemSocketService` | `GodstoneSocket` / `EnchantOrSocket` paths | Service / Packet Caller | Partial | Unit Tested as metadata | Needs Verification | Metadata only; socket/enchant runtime behavior unverified. |
| `com.aionemu.gameserver.model.gameobjects.player.Equipment` | `SoulBind` state path | Player Equipment / Packet Caller | Partial | Unit Tested as metadata | Needs Verification | Metadata only; soul-bind persistence/runtime behavior unverified. |

## Remaining Risks

- Runtime behavior is still not bridged: Java mutates during packet write; C# mutates after sending in scheduled paths.
- `GameServerConnection` implementation was not changed.
- C# stores item object id, not an `Item` reference.
- C# clears only matching pending object id; Java clears the stored item reference directly.
- Live scheduling, cancellation, item mutation, cooldown removal, send-vs-broadcast visibility, threading, exception/null behavior, serialization/frame/crypto, dynamic item actions, and live-client behavior remain missing or unverified.

## Summary Metrics

- Total Java artifacts discovered: 12
- Total artifacts ported: 1 represented non-live positive-time item-use state trace plus tests
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 12
- Total blocked artifacts: 33 blocked/not-started categories
- Estimated overall migration completion: Phase 6 remains about 66% complete

## Next Recommended Unit of Work

Continue item-use parity by adding a narrow `GameServerConnection` runtime test for one positive-time scheduled path, such as animation-add or assembly, proving that C# sends/broadcasts the start animation, sets `Player.UsingItemObjectId`, clears it after completion/cancel, and documenting the remaining Java write-time side-effect mismatch.

Suggested scope:

- Inspect existing `GameServerConnection` item-use tests for the easiest positive-time scheduled path fixture.
- Prefer a single path with minimal static-data setup, likely animation-add or assembly.
- Assert start animation payload/order.
- Assert `Player.UsingItemObjectId` after scheduling.
- Assert cleanup after cancellation or completion.
- Do not change broad item-use implementation unless required by a failing parity test.

## Safe Parallel Work Candidates

| Candidate | Files | Parallel Safe? | Notes |
|---|---|---|---|
| Runtime test seam audit | `dotnetConversion/tests/Aion.GameServer.Tests/*Item*Tests.cs`, `GameServerConnection.cs` read-only | Yes | Find the least expensive scheduled path to test. |
| Java cancellation audit | Java `PlayerController`, representative item actions | Yes | Refine cancellation/end-state expectations. |
| C# runtime test implementation | one existing test file | Yes, if exclusive | Do not edit `GameServerConnection.cs` in parallel. |
| Progress/handoff docs | docs | No | Orchestrator-owned after tests pass. |

## Do Not Parallelize

- `GameServerConnection.cs` implementation edits: large shared item-use surface.
- `PlayerSummonSkillExecutionService.cs` / `PlayerSummonSkillExecutionServiceTests.cs`: shared metadata files.
- Progress and handoff docs: Orchestrator-owned.

## Resume Checklist

1. Read `docs/csharp-port.md`, orchestration docs, `docs/PHASE-6-PROGRESS.md`, and this handoff.
2. Confirm branch status and latest commit.
3. Run parallel work discovery before selecting subagents.
4. Use Java as source of truth and preserve breadcrumbs in code.
5. Keep the next unit small: one positive-time scheduled runtime test.
6. Run focused and full tests.
7. Update Migration Parity Table, Remaining Risks, Summary Metrics, and Next Recommended Unit.
8. Create the next handoff and commit the completed unit.
