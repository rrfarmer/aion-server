# Phase 6 UOW-1306 - `CM_PET` Parser Metadata

Date: May 27, 2026

## Scope

This unit adds a C# parser-only surface for Java `CM_PET` opcode `22` so later pet runtime work can consume source-derived packet metadata without mixing parser and mutation behavior.

Java source of truth:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_PET.java`
- `game-server/src/com/aionemu/gameserver/model/gameobjects/PetAction.java`
- `game-server/src/com/aionemu/gameserver/network/aion/AionClientPacketFactory.java`

C# artifacts:

- `dotnetConversion/src/Aion.GameServer/Network/Aion/ClientPackets/CmPet.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameClientPacketFactory.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmPetTests.cs`

## Implemented

- Registered C# opcode `22` as `CmPet` for `GameConnectionState.InGame`, matching Java `AionClientPacketFactory`.
- Added `CmPet` parser properties for the Java `CM_PET.readImpl` fields used by:
  - `ADOPT`
  - `SURRENDER`
  - `SPAWN`
  - `DISMISS`
  - `FOOD`
  - `RENAME`
  - `MOOD`
- Added parser tests for:
  - factory registration and state gating;
  - adopt field order;
  - template-only surrender/spawn/dismiss actions;
  - food autoloot/autosell activation shape;
  - food doping sub-actions `0`, `1`, `2`, and `3`;
  - feed/cancel/not-hungry shared object/count/unknown shape;
  - rename object/name shape;
  - mood subtype/emotion shape;
  - unknown action default-field behavior.

## Explicitly Deferred

- `CM_PET.runImpl` is not ported. No adoption, surrender, spawn, dismiss, food, rename, mood, item mutation, DAO mutation, validation, packet sending, or pet service side effects were added.
- `EXTEND_EXPIRATION` remains intentionally out of this parser slice. Java reads `eggObjId` and `objectId`, but `runImpl` is a no-op. UOW-1301 recommends keeping the expiration path deferred until the expiration system has a C# home.
- `SPECIAL_FUNCTION` is a server-packet action in Java `SM_PET`; it is not handled as a Java `CM_PET.readImpl` branch.
- Java malformed/underflow behavior was not separately tested here. Factory-created C# client packets use non-strict packet reads, but direct unit tests use complete payloads.

## Validation

- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "CmPet|PetActionAndEmoteResolvers|SmPet|PetJavaVectorArtifactReader"` passed 39 tests.
- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "CharacterSelectionServerPackets_WriteJavaShapedPayloads|BindPointTeleport|CmBindPointTeleport|SmBindPointTeleport|PlayerKnownList|SmPlayerInfo|SmPlayerStance|SmAbnormalEffect|SmPet|CmPet|PetActionAndEmoteResolvers|PetJavaVectorArtifactReader|PlayerVisualStatsUpdate"` passed 407 tests.

No Java runtime packet capture was executed. No live `GameServerConnection` pet dispatch was enabled.

## Migration Parity Table - UOW-1306

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_PET` | `Aion.GameServer.Network.Aion.ClientPackets.CmPet` | Client Packet / Parser | Partial | Unit + Regression Tested | Partial Parity | Parser-only coverage now exists for `ADOPT`, `SURRENDER`, `SPAWN`, `DISMISS`, `FOOD`, `RENAME`, and `MOOD`. Runtime `runImpl` side effects are not ported. `EXTEND_EXPIRATION` body parsing is intentionally deferred despite Java reading two `D` fields. |
| `com.aionemu.gameserver.model.gameobjects.PetAction` | `Aion.GameServer.Model.GameObjects.PetAction` / `PetActionResolver` | Enum / Resolver | Partial | Unit + Regression Tested | Partial Parity | Existing resolver maps Java action ids. This unit consumes it from a client parser. Unknown ids map to `Unknown`, while Java `getActionById` can return null; C# default branch preserves field defaults and documents the gap. |
| `com.aionemu.gameserver.network.aion.AionClientPacketFactory` | `Aion.GameServer.Network.Aion.GameClientPacketFactory` | Packet Factory | Partial | Unit Tested | Partial Parity | Opcode `22` now registers as in-game only like Java. Reflection-based Java packet construction differs from C# delegate registration by design. Many Java opcodes remain unregistered in C#. |
| `com.aionemu.gameserver.services.toypet.PetAdoptionService` | future C# live adoption runtime | Service | Not Started | Manual Only | Needs Verification | Discovered dependency for `CM_PET.ADOPT`: name validation, egg/template validation, inventory decrement, pet DAO insert, `SM_PET` adopt response, and expiration timer registration remain missing. |
| `com.aionemu.gameserver.services.toypet.PetSpawnService` | future C# live pet spawn runtime | Service | Not Started | Manual Only | Needs Verification | Discovered dependency for `CM_PET.SPAWN` and `DISMISS`: active-pet state, controller delete, spawn visibility, and known-list integration remain missing. |
| `com.aionemu.gameserver.services.toypet.PetService` | future C# live pet service | Service | Not Started | Manual Only | Needs Verification | Discovered dependency for food, doping, looting, autosell, remove-object, and rename flows. Inventory mutation, pet common-data mutation, DAO updates, and response packet order are unported. |
| `com.aionemu.gameserver.services.toypet.PetMoodService` | future C# live pet mood service | Service | Not Started | Manual Only | Needs Verification | Discovered dependency for `CM_PET.MOOD`: mood/gift cooldown checks and mood side effects are unported. |
| `com.aionemu.gameserver.services.NameRestrictionService` | future C# pet-name validation boundary | Service | Not Started | Manual Only | Needs Verification | Discovered dependency for adopt/rename runtime. Java validation and forbidden-name checks are not invoked by this parser-only unit. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `TryCreatePacket_RegistersJavaFunctionalPetOpcodeAsInGameOnly` | Unit | `AionClientPacketFactory` opcode `22` row | C# factory creates `CmPet` only for in-game state. | Source-derived state/opcode assertion. | Does not exercise encrypted socket dispatch. |
| `ReadFrom_AdoptReadsJavaFieldOrder` | Unit | `CM_PET.readImpl` `ADOPT` branch | Reads egg object id, template id, byte unknown, three integer unknown/decor fields, and UTF-16 name in Java order. | Source-derived field assertions. | No runtime adoption validation or DAO mutation. |
| `ReadFrom_TemplateOnlyActionsReadTemplateIdLikeJava` | Unit | `CM_PET.readImpl` `SURRENDER`/`SPAWN`/`DISMISS` branches | Reads template id for all three branches. | Source-derived field assertions. | Runtime pet spawn/surrender/dismiss behavior missing. |
| `ReadFrom_FoodSpecialFunctionReadsActivationAndSkipsPaddingLikeJava` | Unit | `CM_PET.readImpl` `FOOD` action types `3` and `4` | Reads activation flag and skips the two Java zero fields. | Source-derived field assertions. | Runtime autoloot/autosell activation missing. |
| `ReadFrom_FoodDopingReadsSubActionsLikeJava` | Unit | `CM_PET.readImpl` `FOOD` action type `2` | Reads doping sub-action shapes for add, remove, switch, and use. | Source-derived field assertions. | Runtime doping inventory/buff behavior missing. |
| `ReadFrom_FoodFeedReadsObjectCountAndUnknownLikeJava` | Unit | `CM_PET.readImpl` `FOOD` fallback branch | Reads object id, count, and unknown field for feed/cancel/not-hungry payloads. | Source-derived field assertions. | Runtime feed/remove-object and packet responses missing. |
| `ReadFrom_RenameReadsObjectIdAndNameLikeJava` | Unit | `CM_PET.readImpl` `RENAME` branch | Reads object id and UTF-16 name. | Source-derived field assertions. | Java runtime ignores parsed object id; C# runtime absent. |
| `ReadFrom_MoodReadsSubtypeAndEmotionLikeJava` | Unit | `CM_PET.readImpl` `MOOD` branch | Reads subtype and emotion id. | Source-derived field assertions. | Mood cooldown/service side effects missing. |
| `ReadFrom_UnknownActionKeepsJavaDefaultFields` | Unit | Java switch default/no branch behavior | Unknown action id leaves parser fields at defaults. | Source-derived default-field assertion, adapted through C# `Unknown`. | Java null-action switch behavior needs deeper malformed-packet audit. |

## Remaining Risks

- Runtime `CM_PET.runImpl` behavior remains unported and broad.
- `EXTEND_EXPIRATION` parser fields are deferred; Java reads two ints but then does nothing.
- Unknown action behavior is only partially comparable because Java enum lookup returns null and the switch would be risky if executed with an unknown id.
- Java name validation, forbidden-name checks, inventory mutation, pet DAO writes, expiration timers, pet controller deletion, pet known-list fanout, and pet-service packet responses are missing.
- Threading differences remain unknown for future pet service/controller state. This unit is parser-only and introduces no scheduling.
- Serialization/date/time/precision concerns are mostly deferred to runtime: pet expiration epoch seconds, birthday, feed timers, doping state, and common-data mutation are not hydrated.
- No Java runtime packet vectors or live client dispatch tests were produced.

## Summary Metrics

- Total Java artifacts discovered: 8 grouped artifact rows in this unit
- Total artifacts ported: 1 parser class plus 1 factory registration and 9 focused parser tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 8 grouped rows
- Total blocked artifacts: live pet adoption, spawn, dismiss, surrender, food/doping/autoloot/autosell, rename, mood, expiration extension, name validation, DAO/persistence, inventory mutation, timers, known-list fanout, and socket dispatch
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Port the adjacent opcode `CM_PET_EMOTE` parser metadata for Java functional-pet movement/emote packets, or audit and implement the lowest-risk `SM_PET.SPECIAL_FUNCTION` server-packet subtypes for autoloot/autosell activation. Keep runtime pet mutation disabled until pet/common-data/service boundaries exist.

