# Phase 6 Session 2649 Completion

## UOW

[Phase 6] UOW-2649: Send persisted pet special-function spawn packets live.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: spawning a pet with persisted auto-loot or auto-sell enabled now sends the Java-equivalent special-function activation packets from the live CM_PET SPAWN handler.
- Java source/runtime path: PetSpawnService.summonPet checks PetCommonData.isLooting() and isSelling(), then sends new SM_PET(PetSpecialFunction.AUTOLOOT/AUTOSELL, true).
- C# runtime artifact wired: GameServerConnection.HandlePetSpawnAsync now emits SmPet.SpecialFunction packets after the spawn packet when PlayerOwnedPet.IsLooting or IsSelling is true.
- Client-visible/packet effect: CM_PET SPAWN sends real SM_PET special-function activation packets so the client can reflect persisted auto-loot/auto-sell state immediately after spawn.
- Why this is runtime progress: this changes live server packet fanout from a packet handler based on persisted pet runtime state.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/toypet/PetSpawnService.java`
  - `summonPet` sends the spawn packet and then sends `SM_PET(PetSpecialFunction.AUTOLOOT, true)` when `petCommonData.isLooting()` is true.
  - `summonPet` sends `SM_PET(PetSpecialFunction.AUTOSELL, true)` when `petCommonData.isSelling()` is true.
- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_PET.java`
  - `SPAWN` resolves the owned pet and delegates to `PetSpawnService.summonPet`.
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_PET.java`
  - The `PetSpecialFunction` constructor writes the special-function action, function id, body field, and active flag for non-doping functions.

## C# Changes

- Extended `GameServerConnection.HandlePetSpawnAsync` to send persisted special-function activation packets after the spawn packet.
- Reused existing `PlayerOwnedPet.IsLooting` and `PlayerOwnedPet.IsSelling` runtime state.
- Reused existing `SmPet.SpecialFunction` and `SmPetSpecialFunctionSnapshot` packet serialization.
- Added focused live packet coverage for a spawned pet with both persisted flags enabled.

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ProcessPacketAsync_CmPetSpawnSendsPersistedSpecialFunctionPackets` | Unit/live connection | `PetSpawnService.summonPet` special-function sends | Live `CM_PET SPAWN` emits spawn, AutoLoot active, and AutoSell active packets in Java order when the owned pet has persisted flags enabled. | Runs the actual packet handler and asserts serialized `SM_PET` special-function packet fields with existing packet helpers. | No raw Java golden byte fixture; no real client validation. |

## Validation Decision

```text
- Changed surface: live CM_PET SPAWN packet fanout.
- Specific behavior/contract: persisted pet auto-loot and auto-sell flags should emit activation packets immediately after spawn.
- Focused C# command: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter FullyQualifiedName~ProcessPacketAsync_CmPetSpawnSendsPersistedSpecialFunctionPackets --no-restore
- Focused C# command: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_CmPetSpawn|FullyQualifiedName~ProcessPacketAsync_CmPetAutoLoot|FullyQualifiedName~ProcessPacketAsync_CmPetAutoSell" --no-restore
- Focused C# command: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter FullyQualifiedName~GameServerConnectionBuyItemTests --no-restore
- Focused Java/Maven command: not run; no narrow Java packet fixture exists for PetSpawnService.summonPet special-function packet fanout in this checkout, and Java source was reviewed directly.
- Broad-validation trigger: live packet fanout changed.
- Broad .NET decision: skipped after focused validation because the filtered connection tests built affected projects and directly exercised spawn, auto-loot, auto-sell, and adjacent pet packet paths.
- Why this scope is sufficient: the edited behavior is isolated to HandlePetSpawnAsync and covered by a live packet-path regression plus adjacent pet action tests that assert packet ordering and serialized payloads.
```

Results:

- New focused spawn special-function test: passed, 1/1.
- Targeted spawn/auto-loot/auto-sell filter: passed, 10/10.
- `GameServerConnectionBuyItemTests`: passed, 85/85.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.toypet.PetSpawnService.summonPet` persisted special-function fanout | `Aion.GameServer.Network.Aion.GameServerConnection.HandlePetSpawnAsync` | Runtime handler | Partial | Unit Tested | Partial Parity | Live spawn now sends persisted AutoLoot and AutoSell activation packets after spawn. Other spawn lifecycle behavior, including mood/update scheduling and full delete-time persistence, remains incomplete. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PET(PetSpecialFunction, boolean, int)` | `Aion.GameServer.Network.Aion.ServerPackets.SmPet.SpecialFunction` / `SmPetSpecialFunctionSnapshot` | Packet | Partial | Unit Tested | Partial Parity | Existing packet writer is exercised from live spawn and asserted for action, function id, body field, and active flag. No raw Java golden byte comparison was run. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_PET` spawn path | `Aion.GameServer.Network.Aion.GameServerConnection.HandlePetSpawnAsync` | Runtime packet path | Partial | Unit Tested | Partial Parity | SPAWN now restores active-pet state, sends spawn packet, schedules restored refeed delay where applicable, and sends persisted special-function packets. Remaining Java spawn side effects are not complete. |

## Known Gaps

- Java `PetController.onDelete` persists feed status on delete; C# active pet dismiss currently does not persist feed status as a delete-time operation.
- Java pet mood/despawn persistence and periodic pet update scheduling/cancellation remain incomplete.
- No raw Java golden packet comparison was run for special-function packets.
- Real client validation was not run.
- Real MySQL validation was not run.

## Next Recommended Runtime UOW

**UOW-2650 candidate: persist active pet feed status on dismiss/delete live.**

Runtime progress gate to verify before editing:

```text
- Deferred/live behavior being advanced: active pet delete should preserve current feed status instead of losing hungry/progress/refeed data when the pet is dismissed or surrendered while active.
- Java source method or runtime path: PetController.onDelete sets PetCommonData cancel-feed state and calls PlayerPetsDAO.saveFeedStatus(objectId, hungryLevel, progressData, refeedTime).
- C# runtime artifact to wire or fix: GameServerConnection.ClearActivePetAsync and the player/pet repository persistence path for owned pet feed fields.
- Client-visible/state/persistence effect expected: CM_PET DISMISS and active-pet SURRENDER mutate live owned-pet cancel-feed/feed state and persist feed status through the existing database shape.
- Why this is not preview-only/test-only/documentation-only: it changes live pet delete state and database persistence from live packet handlers.
```

Suggested focused validation:

```text
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_CmPetDismiss|FullyQualifiedName~ProcessPacketAsync_CmPetSurrender" --no-restore
```

Start by inspecting existing pet feed persistence APIs and Java `PlayerPetsDAO.saveFeedStatus`. If the existing repository shape cannot persist feed status safely, document that blocker and pick the next runtime candidate rather than adding preview-only scaffolding.

## Summary Metrics

- Total Java artifacts discovered in this UOW: 3
- Total artifacts ported or extended in this UOW: 3
- Total artifacts with verified parity: 0
- Total artifacts needing verification or partial parity: 3
- Total blocked artifacts: 0
- Estimated overall Phase 6 migration completion: 42%
