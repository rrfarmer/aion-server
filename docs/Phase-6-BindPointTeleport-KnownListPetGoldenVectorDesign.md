# Phase 6 Bind-Point Teleport Known-List Pet Golden-Vector Design

Date: May 27, 2026
Unit of Work: UOW-1295
Status: Design complete; no Java runtime vectors were generated in this unit.

## Scope

This document defines the smallest Java packet-vector capture plan needed to move known-list pet visibility from source-derived packet assertions toward objective runtime/golden-file evidence.

The immediate target is the known-list path only:

- `PlayerController.see(Pet)` sends `SM_PET(Pet)`;
- if `pet.getMaster().isInFlyingState()` is true, it then sends `SM_PET_EMOTE(pet, PetEmote.FLY_START)`;
- `PlayerController.notSee(Pet, ObjectDeleteAnimation)` sends `SM_PET(objectId, animation)`.

Full toy-pet management packets remain out of scope for the first vector because they require mutable feed, mood, doping, template-function, expiration, DAO, and scheduler state.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_PET.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_PET_EMOTE.java`
- `game-server/src/com/aionemu/gameserver/model/gameobjects/Pet.java`
- `game-server/src/com/aionemu/gameserver/model/gameobjects/player/PetCommonData.java`
- `game-server/src/com/aionemu/gameserver/model/gameobjects/PetAction.java`
- `game-server/src/com/aionemu/gameserver/model/gameobjects/PetEmote.java`
- `game-server/src/com/aionemu/gameserver/model/templates/pet/PetFunctionType.java`
- `game-server/src/com/aionemu/gameserver/controllers/PlayerController.java`
- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_PET_EMOTE.java`
- `game-server/src/com/aionemu/gameserver/controllers/PetController.java`
- `game-server/src/com/aionemu/gameserver/services/toypet/PetSpawnService.java`

## Minimum Golden Vectors

| Vector | Java Constructor / Path | Required Java Inputs | Expected Payload Shape | Why It Matters |
|---|---|---|---|---|
| Spawn | `new SM_PET(pet)` from `PlayerController.see(Pet)` | Pet name, template id, pet object id, current XYZ, move target XYZ, heading, master object id, common-data decoration | `H action=SPAWN(3)`, string name, template id, object id, current XYZ, target XYZ, heading byte, master object id, appearance block | Confirms C# `SmPet` spawn field order, string encoding, and float/byte layout against Java runtime. |
| Fly start | `new SM_PET_EMOTE(pet, PetEmote.FLY_START)` after `master.isInFlyingState()` | Pet object id; emote id `129`; default emotion/param zeros | `D petObjectId`, `C emote=129`, `C 0`, `C 0` | Confirms Java default branch behavior for known-list fly-start and guards against conflating flying with gliding. |
| Dismiss | `new SM_PET(petObjectId, animation)` from `PlayerController.notSee(Pet)` | Pet object id; `ObjectDeleteAnimation` id | `H action=DISMISS(4)`, `D petObjectId`, `C animationId` | Confirms delete-animation byte and minimal dismiss packet shape. |

## Recommended Java Harness Shape

The most useful capture harness should create packet instances directly and serialize unencrypted packet payloads with fixed inputs.

Suggested fixture setup:

1. Create or mock a `Player` master with fixed object id, world id, position, and `isInFlyingState()` value.
2. Create `PetCommonData` with fixed object id, template id, master object id, name, decoration, and non-null `despawnTime` if the construction path touches spawn services.
3. Create a `PetTemplate` that resolves through `DataManager.PET_DATA` or an injectable/static-data fixture.
4. Create `Pet` with fixed `PetController`, `PetCommonData`, `PetTemplate`, and master.
5. Set pet world position/current XYZ and heading.
6. Set pet move-controller target XYZ.
7. Serialize:
   - `new SM_PET(pet)`;
   - `new SM_PET_EMOTE(pet, PetEmote.FLY_START)`;
   - `new SM_PET(pet.getObjectId(), ObjectDeleteAnimation.<chosen>)`.

Preferred output fields:

- vector name;
- Java class/constructor;
- packet opcode if available from the packet registry;
- unencrypted payload bytes after opcode/framing policy is normalized to match existing C# packet tests;
- decoded field list with numeric values and float values;
- Java git commit/source revision;
- static-data/template ids used.

## Fixture Values To Standardize

Use deliberately non-zero, non-equal values so order mistakes are obvious:

| Field | Suggested Value | Notes |
|---|---:|---|
| Master object id | `9002` | Match existing C# pet snapshot tests where practical. |
| Pet object id | `9102` | Distinct from master and item ids. |
| Pet template id | `900001` | Must exist in Java static pet data or be fixture-backed. |
| Pet name | `Tog` | Short ASCII name avoids localization/string surprises. |
| Current X/Y/Z | `10`, `20`, `30` | Float order must be visible. |
| Target X/Y/Z | `11`, `21`, `31` | Distinct from current position. |
| Heading | `90` | Fits Java byte/C# byte. |
| Decoration | `12345` | Confirms appearance block. |
| Fly-start emote | `129` | `PetEmote.FLY_START`. |

## Movement-Emote Follow-Up Vectors

`SM_PET_EMOTE` has two movement branches not currently ported in C#:

| Emote | Java Payload After Pet Id + Emote Id | Input Source |
|---|---|---|
| `MOVE_STOP(0)` | current X/Y/Z, heading | `CM_PET_EMOTE` updates world position, then broadcasts `new SM_PET_EMOTE(pet, emote)`. |
| `MOVETO(12)` | current X/Y/Z, heading, move-target X/Y/Z | `CM_PET_EMOTE` updates world position, calls `setNewDirection`, then broadcasts. |

These should be a second capture batch after known-list spawn/fly-start/dismiss vectors exist. They depend on Java `World.updatePosition` and `CreatureMoveController.setNewDirection` side effects, so the harness must set current and target state through the same path or document any direct-field setup.

## Out-Of-Scope First-Batch Packets

Do not include these in the first known-list packet vector batch:

- `LOAD_PETS`
- `ADOPT`
- `SURRENDER`
- `FOOD`
- `RENAME`
- `MOOD`
- `SPECIAL_FUNCTION`
- house-pet `H_ADOPT` / `H_ABANDON`

Reasons:

- `writePetData` reads Java static pet templates through `DataManager.PET_DATA`.
- Food packets read `PetFeedProgress`, `getRefeedDelay()`, and mutable hunger/timer state.
- Mood packets mutate `lastSentPoints`, `moodCdStarted`, and `giftCdStarted` using `System.currentTimeMillis()`.
- Doping packets depend on `PetDopingBag.MAX_ITEMS`, item slot state, and delayed scheduled use behavior.
- Special-function packets branch on autoloot/autosell/doping subtypes and sometimes NPC object ids.

## C# Readiness Mapping

| Java Requirement | Current C# Surface | Status |
|---|---|---|
| Spawn payload serializer | `Aion.GameServer.Network.Aion.ServerPackets.SmPet` | Partial, source-derived tests only. |
| Dismiss payload serializer | `SmPet` | Partial, source-derived tests only. |
| Fly-start/default emote serializer | `Aion.GameServer.Network.Aion.ServerPackets.SmPetEmote` | Partial, source-derived tests only. |
| Pet snapshot input | `SmPetSpawnSnapshot`; `PlayerKnownListPetSpawnSnapshotProviderInput` | Partial supplied metadata. |
| Provider blockers | `PlayerKnownListPetSpawnSnapshotProviderService`; population diagnostics | Partial non-live diagnostics. |
| Live pet/common-data/template hydration | None | Blocked. |
| Movement emote serializers | `SmPetEmote` `MOVE_STOP` / `MOVETO` branches | Partial, source-derived tests only after UOW-1296. |
| Java runtime golden vectors | None | Blocked by missing harness/run. |

## Migration Parity Table - UOW-1295

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PET` | `Aion.GameServer.Network.Aion.ServerPackets.SmPet`; `docs/Phase-6-BindPointTeleport-KnownListPetGoldenVectorDesign.md` | Packet / Serializer / Design | Partial | Manual Only | Needs Verification | Java source reviewed for spawn, dismiss, and broader action branches. First golden-vector batch is scoped to known-list spawn/dismiss only; full action coverage remains unsupported. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PET_EMOTE` | `Aion.GameServer.Network.Aion.ServerPackets.SmPetEmote`; design doc | Packet / Serializer / Design | Partial | Manual Only | Needs Verification | Java source reviewed for fly-start default branch and movement branches. C# still lacks `MOVE_STOP`/`MOVETO` serialization. |
| `com.aionemu.gameserver.controllers.PlayerController.see(Pet)` | `PlayerKnownListPetVisibilityOrderPlanService`; `PlayerKnownListPetVisibilityPacketConstructionService`; design doc | Controller Packet Flow | Partial | Manual Only | Needs Verification | Source order is spawn then optional fly-start when `master.isInFlyingState()`. No live callback execution or runtime packet capture exists. |
| `com.aionemu.gameserver.controllers.PlayerController.notSee(Pet)` | `PlayerKnownListPetVisibilityPacketConstructionService`; design doc | Controller Packet Flow | Partial | Manual Only | Needs Verification | Source review confirms dismiss uses object id plus delete-animation byte. Viewer spawned guard and live socket send remain unported. |
| `com.aionemu.gameserver.model.gameobjects.Pet` | `SmPetSpawnSnapshot`; `PlayerKnownListPetSpawnSnapshotProviderInput` | Model / Snapshot Source | Partial | Manual Only | Needs Verification | Java dependencies are master, common data, template, position, move controller, and heading. Live C# pet model is still missing. |
| `com.aionemu.gameserver.model.gameobjects.player.PetCommonData` | Provider input name/decoration; design doc | Common Data / Snapshot Source | Partial | Manual Only | Needs Verification | Known-list spawn needs name and decoration; full pet-list/feed/mood/doping vectors need birthday, expiration, feed, doping, mood, timers, and scheduler/DAO behavior. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_PET_EMOTE` | future movement-emote vector plan | Client Packet / Movement Source | Not Started | Manual Only | Unknown | Java source reviewed to identify `MOVE_STOP` and `MOVETO` side effects. C# movement-emote serializer and parser/runtime path remain unported. |

## Tests Added

No executable tests were added in UOW-1295. This was a documentation/design unit based on Java source review.

## Remaining Risks

- No Java runtime packet captures exist yet, so parity remains unverified.
- Existing C# pet packet tests are source-derived, not golden-vector compared.
- Java fixture setup may need static-data loading or controlled `DataManager.PET_DATA` access for `PetTemplate`.
- Movement-emote vectors require careful current-position and move-controller target setup.
- Full `SM_PET` action vectors depend on mutable timers, static pet functions, scheduled tasks, DAO state, and pet feed/doping/mood systems.
- Live socket dispatch and known-list mutation remain disabled.

## Summary Metrics

- Total Java artifacts discovered: 7 grouped artifact rows in this unit
- Total artifacts ported: 0 code artifacts; 1 golden-vector design document
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 6 grouped rows
- Total blocked artifacts: Java runtime packet harness, live pet model/hydration, movement-emote serializer branches, full `SM_PET` action coverage, static pet data fixture, and live known-list dispatch
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Implement the next smallest executable pet packet prerequisite:

- either add C# `SmPetEmote` `MOVE_STOP` and `MOVETO` serializer branches with source-derived tests; or
- build the Java packet golden-vector harness if Java tooling/static-data fixture setup is ready.

Keep live known-list dispatch disabled in either path.
