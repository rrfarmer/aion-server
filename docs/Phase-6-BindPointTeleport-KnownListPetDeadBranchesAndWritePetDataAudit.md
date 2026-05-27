# Phase 6 Bind-Point Teleport Known-List Pet Dead Branches and writePetData Audit

Date: May 27, 2026
Unit of Work: UOW-1301
Status: Read-only audit complete; no serializer behavior added.

## Scope

This unit audits two low-state `SM_PET` expansion candidates before adding more packet shapes:

- Java `SM_PET(int petId, int petObjectId)`;
- Java `PetAction.EXTEND_EXPIRATION(15)`;
- prerequisites for a future `SM_PET.writePetData(PetCommonData)` snapshot contract.

The result is intentionally conservative:

- do not port `SM_PET(int, int)` as a working C# constructor;
- do not add `EXTEND_EXPIRATION` to `SmPet(PetAction)` action-only allow-list;
- design `writePetData` before porting `LOAD_PETS` or `ADOPT`.

No C# production behavior, packet serializer, parser, live dispatch, or tests were changed in this unit.

## Parallel Work Discovery

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | `SM_PET(int,int)` call-site audit | `SM_PET`, pet services/controllers | none/read-only | Java Analysis | Yes | Low | Isolated question; determines whether an unsafe-looking overload matters. |
| B | `EXTEND_EXPIRATION` call-site audit | `CM_PET`, `PetAction`, `SM_PET` | none/read-only | Java Analysis | Yes | Low | Isolated question; determines whether action-only allow-list can expand. |
| C | `writePetData` snapshot design | `SM_PET.writePetData`, `PetCommonData`, `PetTemplate`, `PetDopingBag`, `PetFeedProgress` | docs only | Java Analysis / Documentation | Yes | Medium | Bigger design task, but doc-only and separate from code. |
| D | First `writePetData` C# DTO/test | `SM_PET.writePetData` | `SmPet.cs`, `GamePacketTests.cs` | Implementation | No with C | Medium/High | Should wait for design and touches shared serializer/test files. |

Selected batch:

| Agent | Assigned Task | Task Type | Allowed Files | Forbidden Files | Expected Result |
|---|---|---|---|---|---|
| Explorer A | Audit `SM_PET(int,int)` call sites and risk | Java Analysis | read-only | all writes | Report whether constructor is used and whether C# should port/avoid it. |
| Explorer B | Audit `EXTEND_EXPIRATION` server response behavior | Java Analysis | read-only | all writes | Report if server emits action `15` response and packet/parser implications. |
| Orchestrator | Inspect `writePetData` source/design prerequisites | Java Analysis / Documentation | docs only at integration | production C# and tests | Snapshot-contract notes and next unit recommendation. |

Both explorers completed read-only and were closed.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_PET.java`
- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_PET.java`
- `game-server/src/com/aionemu/gameserver/model/gameobjects/PetAction.java`
- `game-server/src/com/aionemu/gameserver/model/gameobjects/player/PetCommonData.java`
- `game-server/src/com/aionemu/gameserver/model/templates/pet/PetFunctionType.java`
- `game-server/src/com/aionemu/gameserver/model/templates/pet/PetFunction.java`
- `game-server/src/com/aionemu/gameserver/model/templates/pet/PetTemplate.java`
- `game-server/src/com/aionemu/gameserver/model/templates/pet/PetDopingBag.java`
- `game-server/src/com/aionemu/gameserver/services/toypet/PetFeedProgress.java`
- `game-server/src/com/aionemu/gameserver/services/toypet/PetAdoptionService.java`
- `game-server/src/com/aionemu/gameserver/model/Expirable.java`
- `game-server/src/com/aionemu/gameserver/taskmanager/tasks/ExpireTimerTask.java`

## Audit Findings

### `SM_PET(int petId, int petObjectId)`

The constructor exists in Java but no in-repo direct call site was found.

Observed Java constructor behavior:

- sets `action = PetAction.SURRENDER`;
- stores only `petObjectId`;
- ignores `petId`;
- does not populate `commonData`.

Java `writeImpl` for `SURRENDER` reads `commonData.getTemplateId()` and `commonData.getObjectId()`. Serializing a packet created through this overload would likely throw `NullPointerException`.

Reflective construction appears implausible in current Java server-packet flow. `ServerPacketsOpcodes` registers classes for opcode lookup, and packets are sent as already-created instances through `PacketSendUtility`; explorer searches found no reflective server-packet constructor path.

Decision: keep this overload unported/blocked in C# unless future evidence shows a real Java call site and runtime behavior.

### `PetAction.EXTEND_EXPIRATION`

`CM_PET.readImpl` parses action `15` as:

- `eggObjId = readD()` for item object id;
- `objectId = readD()` for pet object id.

`CM_PET.runImpl` then explicitly does nothing for this action:

- no validation;
- no item consume;
- no pet lookup;
- no DAO update;
- no `ExpireTimerTask` update;
- no `SM_PET` response packet.

Java `SM_PET(PetAction.EXTEND_EXPIRATION)` could theoretically serialize only `H 15` because `writeImpl` writes the action id before the switch and has no action `15` branch. However, no Java call site sends this packet.

Decision: keep `PetAction.ExtendExpiration` in the enum/resolver, but do not allow `new SmPet(PetAction.ExtendExpiration)` as an action-only response. Java runtime behavior is silence/no-op, not a server acknowledgment.

### `writePetData` Future Contract

Java `SM_PET.writePetData(PetCommonData)` emits:

1. `S petCommonData.getName()`;
2. `D petCommonData.getTemplateId()`;
3. `D petCommonData.getObjectId()`;
4. `D petCommonData.getMasterObjectId()`;
5. `D 0`;
6. `D 0`;
7. `D petCommonData.getBirthday()` as epoch seconds, or `0`;
8. `D petCommonData.secondsUntilExpiration()`;
9. up to two writable pet-function records, padded with `NONE`;
10. `writeAppearance(petCommonData)`.

Writable function record behavior:

| Java Function | Function Id | Payload |
|---|---:|---|
| `WAREHOUSE` | `0` | `C 0`, `C 0` |
| `LOOT` | `3` | `C 3`, `C 1`, `C 0` |
| `DOPING` | `2` | `C 2`, `C PetDopingBag.MAX_ITEMS * 4`, then 8 `D` item ids padded with zero |
| `FOOD` | `1` | `C 1`, `C 8`, `D feedProgress.getDataForPacket()`, `D refeedDelaySeconds` |
| absent/pad `NONE` | `6` | `H 6` for each missing function slot |

Important dependencies and risks:

- `PetTemplate.getPetFunctions()` mutates template state by adding `NONE` when no player function exists.
- `containsFunction` only checks functions with non-negative ids and calls `getPetFunctions()`.
- Function emission order is hard-coded in `SM_PET`: warehouse, loot, doping, food. It does not iterate XML order.
- Java comment says pets have only two functions max, but code can write more than two if a template contains more; C# should not assume until static data is audited.
- `PetFunctionType.FOOD` and `PetFunctionType.APPEARANCE` intentionally share id `1`; C# already preserves this enum value overlap.
- `PetDopingBag.MAX_ITEMS` is `8`; Java pads all eight slots even if the stored item array is shorter.
- `PetFeedProgress.getDataForPacket()` bit-packs regular count, quartered total points, loved count, and four unknown low bits.
- `getRefeedDelay()` mutates `refeedTime` to `0` if it has elapsed and uses `System.currentTimeMillis()`.
- `secondsUntilExpiration()` uses epoch seconds from `System.currentTimeMillis() / 1000` and may be negative.
- `getBirthday()` returns epoch seconds from a SQL `Timestamp`, or `0`.
- Full list/adopt support needs template-function facts, common-data facts, feed/doping facts, and explicit current-time inputs to keep tests deterministic.

## Recommended `writePetData` Snapshot Shape

Before implementing `LOAD_PETS` or `ADOPT`, add a packet-facing DTO rather than hydrating live Java-equivalent models directly:

```text
SmPetDataSnapshot
- Name
- TemplateId
- ObjectId
- MasterObjectId
- BirthdayEpochSeconds
- SecondsUntilExpiration
- IReadOnlyList<SmPetFunctionSnapshot> Functions
- Decoration

SmPetFunctionSnapshot
- FunctionType
- DopingItemIds, for DOPING only, padded to 8 during serialization
- FeedProgressData, for FOOD only
- RefeedDelaySeconds, for FOOD only
```

The future resolver from live data should be separate from packet serialization and should record blockers for:

- missing pet template;
- unsupported function count greater than two if static-data audit disproves the Java comment;
- missing feed progress for FOOD;
- missing doping bag for DOPING;
- missing current-time input for deterministic expiration/refeed calculations;
- unsupported Java template mutation side effects.

## Validation

- No executable tests were added in this documentation/audit unit.
- No Java runtime packet capture was executed.
- No live `GameServerConnection` dispatch was enabled.

## Migration Parity Table - UOW-1301

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PET` | `Aion.GameServer.Network.Aion.ServerPackets.SmPet`; this audit doc | Packet / Serializer | Partial | Manual Only | Needs Verification | Audit confirms two candidate expansions should remain blocked: unsafe `SM_PET(int,int)` and unobserved action `15` response. `writePetData`, `LOAD_PETS`, `ADOPT`, `FOOD`, `MOOD`, and `SPECIAL_FUNCTION` remain unsupported. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PET(int, int)` | Not ported | Packet Constructor | Blocked | Manual Only | Needs Verification | No direct in-repo call site found. Constructor does not initialize `commonData`; Java surrender serialization would likely throw if used. Reflection/dynamic server-packet construction appears implausible but external code cannot be ruled out. |
| `com.aionemu.gameserver.model.gameobjects.PetAction.EXTEND_EXPIRATION` | `Aion.GameServer.Model.GameObjects.PetAction.ExtendExpiration` | Enum / Client Action | Partial | Manual Only | Partial Parity | Enum id exists in C#, but C# correctly does not allow action-only `SmPet` response. Java `CM_PET` parses action `15` and performs a silent no-op with no response packet. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_PET` | future C# client parser/runtime | Client Packet / Handler | Not Started | Manual Only | Needs Verification | Audit covers action `15` read/run behavior only. Full `CM_PET` parser/runtime remains unported, including adoption, surrender, spawn, dismiss, food, rename, mood, and special functions. |
| `com.aionemu.gameserver.model.gameobjects.player.PetCommonData` | future `SmPetDataSnapshot` design | Model Projection / DTO | Partial | Manual Only | Needs Verification | `writePetData` needs name, template id, object id, master id, birthday, seconds-to-expire, decoration, feed, doping, and mutable timing facts. Current C# only has spawn/surrender snapshots. |
| `com.aionemu.gameserver.model.templates.pet.PetTemplate` | future pet-function snapshot/resolver | Template / Static Data | Not Started | Manual Only | Needs Verification | Java `getPetFunctions()` mutates template state by adding `NONE`; `containsFunction` uses hard-coded writer order rather than XML order. Static-data max-function assumptions remain unaudited. |
| `com.aionemu.gameserver.services.toypet.PetFeedProgress` | future feed-progress packet projection | Model / Bit Packing | Not Started | Manual Only | Needs Verification | Java bit-packs regular count, total points shifted by two, loved count, and low unknown bits. No C# equivalent exists yet. |
| `com.aionemu.gameserver.model.templates.pet.PetDopingBag` | future doping-bag packet projection | Model / Fixed Slot Data | Not Started | Manual Only | Needs Verification | Java writes exactly 8 integer slots padded with zero for DOPING functions. No C# equivalent exists yet. |
| `com.aionemu.gameserver.model.Expirable` | future deterministic expiration-time projection | Interface / Date-Time Utility | Partial | Manual Only | Needs Verification | `secondsUntilExpiration()` uses Java epoch seconds and can be negative. Future C# packet snapshots should use supplied seconds values or deterministic current-time inputs. |

## Tests Added

No executable tests were added in UOW-1301. This was a read-only audit/design unit based on Java source review and two read-only explorer reports.

## Remaining Risks

- No Java runtime packet vectors exist locally because Maven is unavailable.
- Read-only searches do not exclude external plugins or out-of-tree code.
- `writePetData` static-data assumptions, especially the "two functions max" comment, still need XML/static-data verification.
- Future `writePetData` implementation must avoid accidentally modeling Java template mutation as normal C# packet serialization side effect unless explicitly documented.
- Feed/refeed and expiration behavior depend on wall-clock time and Java mutation side effects.
- Full `CM_PET` parser/runtime, live pet management, persistence, and socket dispatch remain unported.

## Summary Metrics

- Total Java artifacts discovered: 9 grouped artifact rows in this unit
- Total artifacts ported: 0 code artifacts; 1 audit/design document
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 9 grouped rows
- Total blocked artifacts: unsafe Java `SM_PET(int,int)` overload, action `15` response surface, `writePetData`, pet common-data/template/feed/doping projections, full `CM_PET`, live pet management dispatch, and Java runtime vector generation
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Add a deterministic packet-facing `SmPetDataSnapshot` / `SmPetFunctionSnapshot` design implementation and tests for `writePetData` as a private serializer helper, but keep `LOAD_PETS` and `ADOPT` public constructors disabled until static-data function-count assumptions are audited.

Alternative smaller unit: perform a static-data/XML audit of pet function counts and writable function combinations before writing code.
