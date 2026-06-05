# Phase 6 Session 2592 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2592: Open keyed static doors. See
[Phase-6-Session-2592-Completion.md](Phase-6-Session-2592-Completion.md).

## Commits Made

- `ca69019` - `[Phase 6][UOW-2578] Persist abandoned quest state`
- `559431a` - `[Phase 6][UOW-2579] Persist NPC faction abort state`
- `5aa6cdf` - `[Phase 6][UOW-2580] Re-send assigned NPC faction daily quest`
- `d5a7adf` - `[Phase 6][UOW-2581] Assign NPC faction daily quest`
- `d84e7b53b` - `[Phase 6][UOW-2582] Filter NPC faction daily handlers`
- `ed8eabf35` - `[Phase 6][UOW-2583] Persist quest work item deletes`
- `a767ee5cb` - `[Phase 6][UOW-2584] Wire quest-start item use`
- `5f98483` - `[Phase 6][UOW-2585] Send quest-start rejection messages`
- `84c0f0b` - `[Phase 6][UOW-2586] Send quest-start condition messages`
- `d92efac` - `[Phase 6][UOW-2587] Reject quest-start items at normal quest cap`
- `7c7c3e3` - `[Phase 6][UOW-2588] Send quest-start inventory-item warning`
- `cd9ecd7` - `[Phase 6][UOW-2589] Send quest-start combine-skill warning`
- `0f1a440` - `[Phase 6][UOW-2590] Send quest-start rank warning`
- `b9af493` - `[Phase 6][UOW-2591] Wire keyless static-door open`
- Current commit - `[Phase 6][UOW-2592] Open keyed static doors`

## Session Summary

- UOW-2574 wired `CM_DELETE_QUEST` live for core quest-state mutation and ABANDON/TIMER packets.
- UOW-2575 removed Java quest work-item stacks from live inventory and sent item-delete/cube-size packets.
- UOW-2576 reset active matching NPC-faction quest state to `NOTING` during live abandon.
- UOW-2577 loaded Java work-order `recipe_id` data and wired live recipe delete/send into `CM_DELETE_QUEST`.
- UOW-2578 persisted the live quest abandon mutation to `player_quests` delete/update rows.
- UOW-2579 persisted the live NPC-faction abort mutation to `player_npc_factions`.
- UOW-2580 sent Java `SM_QUEST_ACTION(int questId)` for the reusable assigned NPC-faction daily quest branch after abort.
- UOW-2581 supported Java's random NPC-faction daily replacement branch after abort, including assignment state mutation, packet send, and persistence.
- UOW-2582 loaded quest-handler availability into runtime static data and wired it into the live random NPC-faction daily selector.
- UOW-2583 persisted live quest work-item inventory deletions through the existing inventory repository delete path.
- UOW-2584 loaded Java `queststart` item actions and wired live `CM_USE_ITEM` quest-start state/persistence/packet effects.
- UOW-2585 added Java-equivalent client feedback for active and non-repeatable quest-start item rejections.
- UOW-2586 added Java-equivalent client feedback for race, minimum-level, maximum-level, class, and gender quest-start condition failures.
- UOW-2587 added Java-equivalent client feedback for full normal quest lists, including membership and no-count bypass behavior.
- UOW-2588 added Java-equivalent client feedback for missing required inventory items.
- UOW-2589 added Java-equivalent client feedback for missing combine/crafting skill rank.
- UOW-2590 added Java-equivalent client feedback for missing abyss rank.
- UOW-2591 wired live `CM_OPEN_STATICDOOR` for closed keyless static doors, mutating per-instance state and sending the Java open-door emotion packet.
- UOW-2592 extended live `CM_OPEN_STATICDOOR` to keyed doors, including missing-key feedback, key consumption, locked-state mutation, and open-door packet output.

## Files Changed In UOW-2592

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmSystemMessage.cs`
- `dotnetConversion/src/Aion.GameServer/Services/StaticPlaceableStateService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionOpenStaticDoorTests.cs`
- `docs/Phase-6-Session-2592-Completion.md`
- `docs/Phase-6-Session-2592-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.services.StaticDoorService#openStaticDoor`
- `com.aionemu.gameserver.services.StaticDoorService#checkStaticDoorKey`
- `com.aionemu.gameserver.model.gameobjects.StaticDoor#setLocked`
- `com.aionemu.gameserver.model.gameobjects.StaticDoor#setOpen`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE`

## C# Artifacts Touched

- `Aion.GameServer.Network.Aion.GameServerConnection.HandleOpenStaticDoorAsync`
- `Aion.GameServer.Network.Aion.GameServerConnection.TryUnlockStaticDoorAsync`
- `Aion.GameServer.Services.StaticPlaceableStateService`
- `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage`
- `Aion.GameServer.Tests.GameServerConnectionOpenStaticDoorTests`

## Validation

```text
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionOpenStaticDoorTests|FullyQualifiedName~ClientPacketFactory_ParsesOpenStaticDoorPacket|FullyQualifiedName~SpawnWorldNpcsForInstance_SetsStaticDoorStateLikeJavaStaticDoorSpawnManager" --no-restore
```

Result: passed, 5/5.

Java/Maven: not run. No narrow Java fixture exists; behavior was verified by source review against Java methods listed
above.

Broad .NET: skipped after focused packet/state coverage. The filter built `Aion.GameServer` and directly covered the
modified live packet branch, packet parsing, static-door spawn state seeding, keyless open, missing-key feedback, keyed
stack decrement, locked-state mutation, inventory update output, and open-door packet output.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `StaticDoorService.checkStaticDoorKey` missing-key branch | `GameServerConnection.TryUnlockStaticDoorAsync` | Client feedback | Partial | Unit Tested | Partial Parity | Sends `1300723` from live handler when no matching key item exists. |
| `Inventory.decreaseByItemId(keyId, 1)` | `TryUnlockStaticDoorAsync` inventory mutation | Inventory state | Partial | Unit Tested | Partial Parity | Stack decrement path is covered; single-stack deletion path is implemented but not separately tested. |
| `StaticDoor.setLocked(false)` | `StaticPlaceableStateService.SetDoorLockedState(..., false)` | World/static state | Partial | Unit Tested | Partial Parity | Tracks per-instance locked state in runtime memory. |
| `StaticDoor.setOpen(true)` | `GameServerConnection.HandleOpenStaticDoorAsync` | World/static state + packet | Partial | Unit Tested | Partial Parity | Reuses UOW-2591 open-state mutation and open-door emotion packet. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ProcessPacketAsync_OpenStaticDoorKeyedDoorWithoutKeySendsNeedKeyMessage` | Unit/live handler | `StaticDoorService.checkStaticDoorKey`, `SM_SYSTEM_MESSAGE` | Missing key leaves door closed and sends `1300723` | Source-reviewed Java + live C# handler assertion | Does not cover named-key message; Java path used here sends generic key message. |
| `ProcessPacketAsync_OpenStaticDoorKeyedDoorWithKeyConsumesKeyUnlocksAndOpens` | Unit/live handler | `Inventory.decreaseByItemId`, `StaticDoor.setLocked`, `StaticDoor.setOpen` | Key stack decrements, door unlocks/opens, inventory update precedes open-door packet | Source-reviewed Java + live C# handler assertion | Does not cover key stack deletion at count 1. |

## Known Gaps

- Java `WorldMapInstance.getInstanceHandler().onOpenDoor(doorId)` callback parity is still missing.
- Key deletion at count 1 is implemented but not separately tested.
- Inventory persistence is not immediate in this handler; it follows nearby live in-memory inventory mutation patterns.
- Broader known-list fidelity is partial; the C# path uses connection-registry visibility when available.
- Real client validation was not run.

## Remaining Risks

- Static-door instance scripts may depend on the missing `onOpenDoor` callback.
- Consumed key persistence may need review with the broader inventory save scheduler.
- The unrelated composition-stone cleanup-flag test failure observed in UOW-2589's broad class filter remains outside this UOW.

## Blocked Candidate Checked During Static-Door Discovery

`CM_GROUP_LOOT` still does not pass the Runtime Progress Gate for a small safe UOW.

- Java `CM_GROUP_LOOT.runImpl` dispatches `DropDistributionService.handleRollOrBid`.
- Java roll/bid behavior depends on `DropNpc` runtime fields that C# does not currently model: current index,
  max roll, looting team id, distribution id, in-range players, per-player roll/bid status, highest value/winner, and
  loot group rules.
- C# has the parser, `SmGroupLoot`, and dice/pay `SmSystemMessage` helpers, but `WorldNpcDropRegistrationService`
  only tracks current drops, allowed looters/free-for-all, and looting player.
- Wiring only parser-to-packet shell would be misleading; a faithful branch needs missing live distribution state.

XML start-condition warning packets remain blocked until the live C# condition result carries concrete Java-equivalent
failure detail.

## Next Recommended Runtime UOW

**UOW-2593 candidate: static-door `onOpenDoor` instance callback only if C# has a concrete live instance-handler surface
that can execute behavior from the live door-open path.**

Runtime progress gate to verify before editing:

```text
- Deferred/live behavior being advanced: opening a static door should trigger the Java-equivalent instance callback path when one exists.
- Java source method or runtime path: StaticDoorService.openStaticDoor -> WorldMapInstance.getInstanceHandler().onOpenDoor(doorId).
- C# runtime artifact to wire or fix: GameServerConnection.HandleOpenStaticDoorAsync plus a real instance handler/runtime execution surface.
- Client-visible/state/runtime effect expected: a zone/instance handler changes live state, sends packets, spawns/despawns objects, or advances scripted behavior.
- Why this is not preview-only/test-only/documentation-only if feasible: callback execution must run from live CM_OPEN_STATICDOOR after the door opens.
```

If no live instance-handler execution surface exists, skip callback scaffolding and select another deferred live
packet/state/persistence/runtime-loading path.

## Safe Runtime Candidates

- Static-door `onOpenDoor` callback only with an existing live instance handler surface.
- Static-door key deletion coverage can be added with a runtime code fix only if a behavior gap is found; do not do test-only cleanup.
- Another deferred `GameServerConnection` packet path with an existing service/repository/world surface that can be wired without adapters.
- A narrow `QuestEngine.onItemUseEvent` scripted-handler path only after a concrete C# quest-handler execution surface can be used live.

## Context Needed By Next Session

- `CM_OPEN_STATICDOOR` now opens keyless and keyed static doors from live packet handling.
- Missing key sends `SmSystemMessage.CannotOpenDoorNeedKeyItem()` id `1300723`.
- Present key stacks decrement and send `SmInventoryUpdateItem.DecreaseItemUse`; single-key stacks delete and send `SmDeleteItem.UseDeleteType`.
- Static door open state and locked state are tracked per world/instance/static id in `StaticPlaceableStateService`.
- The Java instance-handler callback after open is not ported.
- `CM_GROUP_LOOT` was reviewed and blocked by missing live drop-distribution state.
- Avoid preview/metadata/evidence-only work. If the next candidate cannot wire live behavior, re-plan from Java source and current live runtime gaps.
