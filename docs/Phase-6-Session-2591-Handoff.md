# Phase 6 Session 2591 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2591: Wire keyless static-door open. See
[Phase-6-Session-2591-Completion.md](Phase-6-Session-2591-Completion.md).

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
- Current commit - `[Phase 6][UOW-2591] Wire keyless static-door open`

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

## Files Changed In UOW-2591

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameClientSocketServer.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionOpenStaticDoorTests.cs`
- `docs/Phase-6-Session-2591-Completion.md`
- `docs/Phase-6-Session-2591-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.network.aion.clientpackets.CM_OPEN_STATICDOOR`
- `com.aionemu.gameserver.services.StaticDoorService#openStaticDoor`
- `com.aionemu.gameserver.services.StaticDoorService#checkStaticDoorKey`
- `com.aionemu.gameserver.model.gameobjects.StaticDoor#setOpen`
- `com.aionemu.gameserver.world.geo.GeoService#setDoorState`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_EMOTION`

## C# Artifacts Touched

- `Aion.GameServer.Network.Aion.GameServerConnection.ProcessPacketAsync`
- `Aion.GameServer.Network.Aion.GameServerConnection.HandleOpenStaticDoorAsync`
- `Aion.GameServer.Network.Aion.GameClientSocketServer`
- `Aion.GameServer.Services.IStaticPlaceableStateService`
- `Aion.GameServer.Dataholders.StaticDoorTable`
- `Aion.GameServer.Network.Aion.ServerPackets.SmEmotion`

## Validation

```text
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionOpenStaticDoorTests|FullyQualifiedName~ClientPacketFactory_ParsesOpenStaticDoorPacket|FullyQualifiedName~SpawnWorldNpcsForInstance_SetsStaticDoorStateLikeJavaStaticDoorSpawnManager" --no-restore
```

Result: passed, 4/4.

Java/Maven: not run. No narrow Java fixture exists; behavior was verified by source review against Java methods listed
above.

Broad .NET: skipped after focused packet/state coverage. The filter built `Aion.GameServer` and directly covered the
modified live packet branch, packet parsing, Java XML static-door table usage, static-door spawn state seeding, keyed
defer behavior, and serialized open-door packet payload.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `CM_OPEN_STATICDOOR.runImpl` | `GameServerConnection.ProcessPacketAsync` | Client packet | Partial | Unit Tested | Partial Parity | Keyless branch is live; keyed and instance-handler callback branches remain missing. |
| `StaticDoorService.openStaticDoor` keyless branch | `GameServerConnection.HandleOpenStaticDoorAsync` | Runtime state | Partial | Unit Tested | Partial Parity | Resolves static data from the player's world, checks current door state, and mutates per-instance state. |
| `GeoService.setDoorState` | `StaticPlaceableStateService.SetDoorState` | World/static state | Partial | Unit Tested | Partial Parity | Existing C# per-instance state is used from live packet handling. |
| `SM_EMOTION(staticId, OPEN_DOOR, 0x9)` | `SmEmotion(doorId, EmotionType.OpenDoor, 0x9, ...)` | Packet | Partial | Unit Tested | Partial Parity | Serialized payload assertion covers sender id, emotion id, state, and speed. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ProcessPacketAsync_OpenStaticDoorKeylessClosedDoorSetsRuntimeStateAndSendsOpenEmotion` | Unit/live handler | `StaticDoorService.openStaticDoor`, `StaticDoor.setOpen` | Closed keyless door opens and sends open-door emotion packet | Source-reviewed Java + live C# handler assertion | Does not cover connection-registry fanout recipient count. |
| `ProcessPacketAsync_OpenStaticDoorKeyedDoorLeavesDeferredStateUntouched` | Unit/live handler | `StaticDoorService.checkStaticDoorKey` | Keyed door remains deferred until lock/key-consumption state is ported | Source-reviewed Java + no-mutation assertion | Keyed parity is still future work. |

## Known Gaps

- Keyed static doors remain deferred: Java needs `StaticDoor.locked`, inventory `decreaseByItemId`, key item updates, and unlocked state.
- Java `WorldMapInstance.getInstanceHandler().onOpenDoor(doorId)` callback parity is still missing.
- Broader known-list fidelity is partial; the C# path uses connection-registry visibility when available.
- Real client validation was not run.

## Remaining Risks

- Static-door instance scripts may depend on the missing `onOpenDoor` callback.
- Keyed doors may need persistence or instance-lifetime cleanup decisions once lock/unlock runtime state is modeled.
- The unrelated composition-stone cleanup-flag test failure observed in UOW-2589's broad class filter remains outside this UOW.

## Blocked Candidate Checked During UOW-2591 Discovery

`CM_GROUP_LOOT` does not currently pass the Runtime Progress Gate for a small safe UOW.

- Java `CM_GROUP_LOOT.runImpl` dispatches `DropDistributionService.handleRollOrBid`.
- Java roll/bid behavior depends on `DropNpc` runtime fields that C# does not currently model: current index,
  max roll, looting team id, distribution id, in-range players, per-player roll/bid status, highest value/winner, and
  loot group rules.
- C# has the parser, `SmGroupLoot`, and dice/pay `SmSystemMessage` helpers, but `WorldNpcDropRegistrationService`
  only tracks current drops, allowed looters/free-for-all, and looting player.
- Wiring only parser-to-packet shell would be misleading; a faithful branch needs missing live distribution state.

XML start-condition warning packets remain blocked for the same reason recorded in UOW-2590: C# collapses concrete XML
failure detail into a generic result, so wiring a packet from that result would be guesswork unless a runtime UOW
also carries concrete Java-equivalent failure detail.

## Next Recommended Runtime UOW

**UOW-2592 candidate: continue `CM_OPEN_STATICDOOR` into keyed-door behavior only if discovery confirms a safe live
inventory mutation/update route and a minimal door locked-state model.**

Runtime progress gate to verify before editing:

```text
- Deferred/live behavior being advanced: keyed CM_OPEN_STATICDOOR attempts consume an existing key item and open/unlock the door, or send Java's missing-key message.
- Java source method or runtime path: StaticDoorService.checkStaticDoorKey -> Inventory.decreaseByItemId -> StaticDoor.setLocked(false) -> StaticDoor.setOpen(true).
- C# runtime artifact to wire or fix: GameServerConnection.HandleOpenStaticDoorAsync, player inventory mutation/update packet path, StaticPlaceableStateService or equivalent door lock runtime state.
- Client-visible/state/persistence effect expected: key item count changes, door open/locked state changes, inventory update/delete and open-door packet/message output.
- Why this is not preview-only/test-only/documentation-only if feasible: it would execute from live CM_OPEN_STATICDOOR and mutate inventory plus world/static-door state.
```

If keyed static doors require a metadata-only lock-state adapter first, skip them and select another deferred live
packet/state/persistence/runtime-loading path.

## Safe Runtime Candidates

- `CM_OPEN_STATICDOOR` keyed branch, only with live inventory mutation and runtime locked-door state.
- A narrow static-door instance callback path only if an existing live instance-handler surface can execute behavior, not just log metadata.
- Another deferred `GameServerConnection` packet path with an existing service/repository/world surface that can be wired without adapters.
- A narrow `QuestEngine.onItemUseEvent` scripted-handler path only after a concrete C# quest-handler execution surface can be used live.

## Context Needed By Next Session

- `CM_OPEN_STATICDOOR` keyless closed doors now open, set per-instance state, and send/broadcast open-door emotion state `0x9`.
- Static door initial open/closed state is already seeded by `WorldNpcSpawnService.SpawnStaticDoorsForInstance`.
- Keyed static-door behavior is intentionally not implemented; do not claim parity there.
- `CM_GROUP_LOOT` was reviewed and blocked by missing live drop-distribution state.
- Quest-start item packets now cover active/non-repeatable/race/min-level/max-level/class/gender/full-normal-list/missing-item/combine-skill/rank feedback.
- Avoid preview/metadata/evidence-only work. If the next candidate cannot wire live behavior, re-plan from Java source and current live runtime gaps.
