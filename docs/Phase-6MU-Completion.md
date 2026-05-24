# Phase 6MU Completion Handoff - Animation-Add Scheduled Item Use Runtime Test

Date: May 24, 2026
Unit of Work: UOW-847
Branch: `4.8`
Commit: pending at handoff creation (`[Phase 6][UOW-847] Test animation add item use scheduling`)

## Status

Phase 6 is still in progress. This unit added a narrow `GameServerConnection` runtime regression test for the animation-add positive-time item-use path.

The test proves current C# behavior for one scheduled path: send the start `SmItemUsageAnimation`, set `Player.UsingItemObjectId` after scheduling, and clear it through pending-use cleanup. It does not bridge Java's packet-write side effect where `SM_ITEM_USAGE_ANIMATION.writeImpl` sets `Player.usingItem` during serialization.

## Files Changed

- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6MU-Completion.md`

## What Changed

- Added an animation-add item template to the existing minimal item-use fixture:
  - item id `188500000`
  - `<animation idle="1" run="2" jump="3" rest="4" minutes="60" />`
- Added optional `ThreadPoolManager` support to the fixture so existing inventory-expansion tests keep their immediate no-scheduler behavior.
- Added `HandleUseItemAsync_AnimationAddSchedulesPositiveTimeUseAndClearsUsingItem`.
- The new test verifies:
  - the animation-add path sends a positive-time `SmItemUsageAnimation`
  - packet payload fields are source-derived Java shape for this call site
  - C# sets `Player.UsingItemObjectId` to the source item object id after scheduling
  - delayed pending-use cleanup clears the matching `UsingItemObjectId`

## Tests

Focused:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "GameServerConnectionInventoryExpansionUseItemTests"
```

Result: passed, 6 tests.

Full:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj
```

Result: passed, 1422 tests.

New/updated test:

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| `GameServerConnectionInventoryExpansionUseItemTests.HandleUseItemAsync_AnimationAddSchedulesPositiveTimeUseAndClearsUsingItem` | C# animation-add scheduled use sends the start item-use animation, encodes the expected positive-time payload, sets `Player.UsingItemObjectId`, and clears it through delayed cleanup. | Source-derived Java packet/caller review; no Java runtime comparison. |

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.templates.item.actions.AnimationAddAction` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleAnimationAddUseItemAsync` / `GameServerConnectionInventoryExpansionUseItemTests` | Dynamic Item Action Caller / Runtime Test | Partial | Regression Tested | Partial Parity | Runtime test covers start packet, scheduler-side state, and delayed cleanup. It does not cover final motion mutation, persistence, expirable registration, active-player completion, or Java runtime behavior. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ITEM_USAGE_ANIMATION` | `Aion.GameServer.Network.Aion.ServerPackets.SmItemUsageAnimation` | Packet / Runtime Call-Site Dependency | Partial | Regression Tested for this call-site payload | Partial Parity | Test validates the start packet payload for player/target/item/time/end/unknown fields. Opcode/frame/crypto and Java-generated runtime bytes remain unverified, and C# still has no write-time `usingItem` side effect. |
| `com.aionemu.gameserver.model.gameobjects.player.Player` | `Aion.GameServer.Model.GameObjects.Player.UsingItemObjectId` | Player State / Runtime Side Effect | Partial | Regression Tested | Needs Verification | C# object-id state is set after scheduling and cleared later. Java stores an `Item` reference during packet write; timing/type/null behavior remains different. |
| `com.aionemu.gameserver.model.gameobjects.player.Inventory` | `Aion.GameServer.Model.GameObjects.InventoryItem` | Inventory Dependency | Partial | Regression Tested for source-item lookup | Needs Verification | C# handler source-item lookup is exercised. Java packet-time inventory lookup and null/exception behavior remain unverified. |
| `com.aionemu.gameserver.utils.ThreadPoolManager` | `Aion.GameServer.Utils.ThreadPoolManager` | Scheduler / Runtime Dependency | Partial | Regression Tested through live scheduled cleanup | Needs Verification | Live C# scheduler is used for delayed cleanup. Java scheduler/task-id/race behavior remains unverified. |
| `com.aionemu.gameserver.controllers.PlayerController` | `GameServerConnection` pending item cleanup/cancel helpers | Runtime / Cancel Dependency | Partial | Regression Tested for completion cleanup only | Needs Verification | Completion cleanup is covered. Movement/emotion cancellation, cancel packet ordering, and Java `cancelUseItem` remain untested. |
| `game-server/data/static_data/items/item_templates.xml` animation action entries | `Aion.GameServer.Dataholders.ItemAnimationActionInfo` / fixture XML | Static Data / DTO | Partial | Regression Tested through fixture load | Needs Verification | Minimal fixture routes to animation-add. Broader Java XML edge cases and duration semantics remain unverified here. |

## Remaining Risks

- Java mutates `Player.usingItem` during packet serialization; C# mutates `Player.UsingItemObjectId` after sending the start packet.
- Direct handler invocation does not establish `_activePlayer`, so this unit validates cleanup but not final motion-learning mutation.
- Cancellation parity is still missing for movement/emotion interruption, `end=2` animation-add cancel packet, and `ItemCanceled` ordering.
- C# stores an item object id, not a live item reference; stale-item, removed-item, null lookup, and packet-write exception behavior remain unverified.
- Scheduler timing/race behavior, opcode/frame/crypto, socket ordering, persistence, expirable registration, reflection/dynamic item actions, and live-client validation remain missing or unverified.

## Summary Metrics

- Total Java artifacts discovered: 7
- Total artifacts ported: 1 represented C# runtime test slice for animation-add scheduled positive-time item use
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 7
- Total blocked artifacts: 26 blocked/not-started categories
- Estimated overall migration completion: Phase 6 remains about 66% complete

## Next Recommended Unit of Work

Continue item-use parity by adding a focused cancellation runtime test for the same animation-add positive-time path: start the scheduled use, trigger the relevant C# cancel path if accessible, assert `UsingItemObjectId` clears, assert the cancel `SmItemUsageAnimation` uses the animation-add `end=2` state plus `ItemCanceled` message ordering, and document remaining Java `PlayerController.cancelUseItem` differences.

Suggested scope:

- Inspect accessible cancel triggers in `GameServerConnection`, especially movement or emotion handlers.
- Reuse the UOW-847 fixture with `includeThreadPoolManager: true`.
- Keep the unit narrow: one cancellation trigger and one expected packet sequence.
- Do not refactor broad item-use scheduling unless a failing parity assertion forces it.

## Safe Parallel Work Candidates

| Candidate | Files | Parallel Safe? | Notes |
|---|---|---|---|
| Cancel trigger audit | `GameServerConnection.cs` read-only, relevant client packet tests | Yes | Find the lowest-friction public/internal handler that cancels pending item use. |
| Java cancel source audit | Java `PlayerController`, `CM_MOVE`, `CM_EMOTION`, `AnimationAddAction` | Yes | Confirm cancel end state and message ordering from source. |
| Runtime cancellation test | `GameServerConnectionInventoryExpansionUseItemTests.cs` | Yes, if exclusive | Reuse the existing fixture and avoid unrelated cases. |
| Progress/handoff docs | docs | No | Orchestrator-owned after tests pass. |

## Do Not Parallelize

- Broad `GameServerConnection.cs` implementation edits.
- Progress and handoff docs.
- Shared packet serialization files unless the unit explicitly changes packet behavior.

## Resume Checklist

1. Read `docs/csharp-port.md`, orchestration docs, `docs/PHASE-6-PROGRESS.md`, and this handoff.
2. Confirm branch status and latest commit.
3. Run parallel work discovery before selecting subagents.
4. Use Java as source of truth and preserve breadcrumbs in code/docs.
5. Keep the next unit small: one animation-add cancellation runtime test.
6. Run focused and full tests.
7. Update Migration Parity Table, Remaining Risks, Summary Metrics, and Next Recommended Unit.
8. Create the next handoff and commit the completed unit.
