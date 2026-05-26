# Phase 6 Bind-Point Teleport Known-List Pet Packet Audit

Date: May 26, 2026
Unit of Work: UOW-1289
Scope: Audit Java `SM_PET` and `SM_PET_EMOTE` packet fields needed by known-list pet visibility.
Source of truth: Java project.

## Summary

UOW-1289 is a docs-only audit of Java pet packets before serializer work.

The known-list pet visibility path from UOW-1288 needs only a narrow subset of Java pet packet behavior at first:

- `SM_PET(Pet)` with action `SPAWN`;
- `SM_PET(int petObjectId, ObjectDeleteAnimation animation)` with action `DISMISS`;
- `SM_PET_EMOTE(Pet, PetEmote.FLY_START)`.

Java `SM_PET` also supports load/adopt/surrender/food/rename/mood/special-function layouts. Those are not required for the immediate known-list pet visibility prerequisite and should not be bundled into the first serializer slice.

## Java Packet Findings

### `SM_PET`

Opcode mapping: Java registers `SM_PET` as server packet opcode `101`.

Common header:

- `writeH(action.getActionId())`

Relevant actions:

| Java Constructor | Action | Action Id | Field Order | Known-List Use |
|---|---:|---:|---|---|
| `SM_PET(Pet pet)` | `PetAction.SPAWN` | `3` | `writeS(pet.getName())`; template id `D`; object id `D`; position `F,F,F`; move target `F,F,F`; heading `C`; master object id `D`; appearance block | Spawn pet after master player visibility callback. |
| `SM_PET(int petObjectId, ObjectDeleteAnimation animation)` | `PetAction.DISMISS` | `4` | pet object id `D`; animation id `C` | Hide pet on not-see; Java does not use `SM_DELETE` for pets. |

Spawn appearance block:

- `writeH(PetFunctionType.APPEARANCE.getId())`;
- color bytes `C,C,C`, currently zero in Java;
- decoration id `D`;
- wings id `D`, currently zero in Java;
- unknown `D`, currently zero in Java.

Full common-data/listing layouts also write pet functions, feed progress, refeed delay, doping bag slots, birthday, expiration, and appearance. These are not needed for first known-list spawn/dismiss serializer work unless `LOAD_PETS` or adopt/surrender flows are included later.

### `PetAction`

Values discovered:

| Name | Id |
|---|---:|
| `LOAD_PETS` | `0` |
| `ADOPT` | `1` |
| `SURRENDER` | `2` |
| `SPAWN` | `3` |
| `DISMISS` | `4` |
| `TALK_WITH_MERCHANT` | `6` |
| `TALK_WITH_MINDER` | `7` |
| `FOOD` | `9` |
| `RENAME` | `10` |
| `MOOD` | `12` |
| `SPECIAL_FUNCTION` | `13` |
| `EXTEND_EXPIRATION` | `15` |
| `H_ADOPT` | `16` |
| `H_ABANDON` | `17` |
| `UNKNOWN` | `255` |

### `SM_PET_EMOTE`

Opcode mapping: Java registers `SM_PET_EMOTE` as server packet opcode `187`.

Common field order:

- pet object id `D`;
- emote id `C`;
- branch by emote.

Relevant known-list branch:

| Java Constructor | Emote | Emote Id | Field Order | Known-List Use |
|---|---:|---:|---|---|
| `SM_PET_EMOTE(Pet pet, PetEmote.FLY_START)` | `FLY_START` | `129` | common fields, then default branch writes `emotionId C` and `param1 C`; both default to `0` for constructor without explicit params | Sent after `SM_PET(pet)` when pet master is flying. |

Movement branches:

- `MOVE_STOP`: common fields, current position `F,F,F`, heading `C`;
- `MOVETO`: common fields, current position `F,F,F`, heading `C`, move target `F,F,F`;
- default branch: common fields, `emotionId C`, `param1 C`.

### `PetEmote`

Relevant values include:

- `MOVE_STOP = 0`;
- `MOVETO = 12`;
- `FLY_START = 129`;
- `FLY_STOP = 130`;
- `FLY = 131`;
- `EMOTION = 133`;
- many mood/feed/loot/attack-mode values;
- `UNKNOWN = Integer.MAX_VALUE`.

## Current C# Gap

No C# packet serializer or model exists yet for:

- `SmPet`;
- `SmPetEmote`;
- `PetAction`;
- `PetEmote`;
- pet object snapshot/common data;
- pet appearance block;
- pet move-controller target coordinates.

The first safe serializer unit should use explicit snapshot DTOs rather than live pet objects.

## Migration Parity Table - UOW-1289

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PET` | future `Aion.GameServer.Network.Aion.ServerPackets.SmPet` | Packet / Serializer | Not Started | No Tests | Needs Verification | Audit identifies known-list spawn/dismiss fields and many unsupported action layouts. Serializer/golden tests are still missing. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PET_EMOTE` | future `Aion.GameServer.Network.Aion.ServerPackets.SmPetEmote` | Packet / Serializer | Not Started | No Tests | Needs Verification | Audit identifies fly-start default branch fields. Movement/emotion branches remain future work. |
| `com.aionemu.gameserver.model.gameobjects.PetAction` | future C# enum | Enum | Not Started | No Tests | Needs Verification | All action ids listed. Unknown/default handling must match Java map lookup. |
| `com.aionemu.gameserver.model.gameobjects.PetEmote` | future C# enum | Enum | Not Started | No Tests | Needs Verification | Relevant ids listed. Full enum and unknown handling need port/tests. |
| `com.aionemu.gameserver.model.gameobjects.player.PetCommonData` | future pet common-data snapshot DTO | DTO / Model | Not Started | No Tests | Needs Verification | Spawn needs name/template/object/master/decoration. Full common-data behaviors for feed/mood/doping/listing remain unported and include live time calculations. |
| `com.aionemu.gameserver.model.templates.pet.PetFunctionType` | future C# enum or packet constants | Enum / Packet Metadata | Not Started | No Tests | Needs Verification | Appearance id and optional function ids audited. Full function packet blocks are future work. |
| `com.aionemu.gameserver.model.templates.pet.PetDopingBag` | future DTO/constants | DTO / Packet Metadata | Not Started | No Tests | Needs Verification | Required only for full `LOAD_PETS`/doping layouts, not first known-list spawn/dismiss serializer. |

## Remaining Risks

- This unit is documentation only; no serializer exists yet.
- Java `writeS` string encoding and frame/opcode behavior must be covered by future packet tests.
- Spawn layout depends on live `Pet`, `PetCommonData`, position, move-controller target, heading, master object id, and appearance data.
- Full `SM_PET` action coverage is broad and should not be assumed from a narrow known-list serializer.
- Pet feed/mood/doping layouts use live timers and mutable common data.
- Java runtime golden vectors were not generated.
- Reflection and threading behavior did not change.

## Summary Metrics

- Total Java artifacts discovered: 7 grouped artifact rows in this unit
- Total artifacts ported: 0 code artifacts; 1 packet audit document
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 7 grouped rows
- Total blocked artifacts: 1 `SM_PET` serializer, 1 `SM_PET_EMOTE` serializer, 1 `PetAction` enum, 1 `PetEmote` enum, 1 pet common-data snapshot DTO, 1 Java runtime packet capture path, and 1 live known-list packet dispatcher
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Add the minimal C# pet packet prerequisites for known-list visibility only:

- `PetAction` values for `SPAWN` and `DISMISS`;
- `PetEmote.FLY_START`;
- snapshot DTOs for pet spawn/dismiss/fly-start packet fields;
- `SmPet` spawn/dismiss serializer and `SmPetEmote` fly-start serializer;
- source-derived packet tests for deterministic payload field order.

Do not implement full `SM_PET` action coverage or live pet visibility dispatch in that unit.
