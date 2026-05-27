# Phase 6AEG Completion Handoff

Date: May 27, 2026
Completed Unit of Work: UOW-1301
Latest Commit: included in the UOW-1301 unit commit
Status: Dead-branch and `writePetData` prerequisite audit complete; no serializer behavior added.

## What Changed

- Added `docs/Phase-6-BindPointTeleport-KnownListPetDeadBranchesAndWritePetDataAudit.md`.
- Updated `docs/Phase-6-BindPointTeleport-LiveAdapter-Readiness.md`.
- Updated `docs/PHASE-6-PROGRESS.md`.

No C# production code or tests were changed.

## Parallel Work Completed

Two read-only explorers completed and were closed:

- `SM_PET(int petId, int petObjectId)` call-site audit:
  - No direct in-repo call site found.
  - Reflective server-packet construction appears implausible.
  - Constructor ignores `petId`, does not initialize `commonData`, and would likely throw during `SURRENDER` serialization.
  - Recommendation: do not port as a working constructor.
- `PetAction.EXTEND_EXPIRATION` audit:
  - Java `CM_PET` parses item object id and pet object id.
  - Java `runImpl` then explicitly does nothing.
  - No Java `SM_PET` response for action `15` was found.
  - Recommendation: keep enum value but do not add to `SmPet(PetAction)` action-only allow-list.

The orchestrator inspected `writePetData` prerequisites and documented a future packet-facing snapshot shape.

## Validation Completed

- No executable tests were added because this was a documentation/audit unit.
- No Java runtime packet capture was executed.
- No live `GameServerConnection` dispatch was enabled.

Run `git diff --check` before committing; expected line-ending warnings may appear on edited docs.

## Migration Parity Table - UOW-1301

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PET` | `Aion.GameServer.Network.Aion.ServerPackets.SmPet`; `docs/Phase-6-BindPointTeleport-KnownListPetDeadBranchesAndWritePetDataAudit.md` | Packet / Serializer | Partial | Manual Only | Needs Verification | Audit confirms two candidate expansions should remain blocked: unsafe `SM_PET(int,int)` and unobserved action `15` response. `writePetData`, `LOAD_PETS`, `ADOPT`, `FOOD`, `MOOD`, and `SPECIAL_FUNCTION` remain unsupported. |
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

Perform a static-data/XML audit of pet function counts and writable function combinations before implementing `writePetData`.

If that audit confirms Java data never exceeds two writable player functions, implement a deterministic packet-facing `SmPetDataSnapshot` / `SmPetFunctionSnapshot` serializer helper and tests next. Keep `LOAD_PETS` and `ADOPT` public constructors disabled until the helper is tested.

## Suggested Parallel Batch For Next Session

| Candidate | Scope | Files | Risk | Parallel Safe? | Notes |
|---|---|---|---|---|---|
| A | Pet XML function-count audit | Java XML/static-data files and docs | Low/Medium | Yes | Verify the Java "two functions max" assumption before serializer code. |
| B | `PetFeedProgress` bit-pack C# helper/test | new isolated helper/test or serializer nested helper | Medium | Yes if not editing `SmPet.cs` | Can be isolated if implemented outside `SmPet.cs`; otherwise sequential. |
| C | `SmPetDataSnapshot` DTO design implementation | `SmPet.cs`, `GamePacketTests.cs` | Medium/High | Writer only | Should wait for A if possible. |
| D | `CM_PET` parser audit/design | Java/C# read-only docs | Low | Yes | Separate from server packet serializer work. |
| E | Java pet vector generator planning | docs/read-only | Low | Yes | Useful while Maven remains unavailable. |

Recommended next batch: run Candidate A as read-only first; optionally run Candidate D or E in parallel. Do not parallelize two writers on `SmPet.cs` or `GamePacketTests.cs`.

## Context Files

- Java source:
  - `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_PET.java`
  - `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_PET.java`
  - `game-server/src/com/aionemu/gameserver/model/gameobjects/PetAction.java`
  - `game-server/src/com/aionemu/gameserver/model/gameobjects/player/PetCommonData.java`
  - `game-server/src/com/aionemu/gameserver/model/templates/pet/PetFunctionType.java`
  - `game-server/src/com/aionemu/gameserver/model/templates/pet/PetFunction.java`
  - `game-server/src/com/aionemu/gameserver/model/templates/pet/PetTemplate.java`
  - `game-server/src/com/aionemu/gameserver/model/templates/pet/PetDopingBag.java`
  - `game-server/src/com/aionemu/gameserver/services/toypet/PetFeedProgress.java`
  - `game-server/src/com/aionemu/gameserver/model/Expirable.java`
- C# source/tests:
  - `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmPet.cs`
  - `dotnetConversion/src/Aion.GameServer/Model/GameObjects/PetAction.cs`
  - `dotnetConversion/src/Aion.GameServer/Model/Templates/Pet/PetFunctionType.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`
- Docs:
  - `docs/csharp-port.md`
  - `docs/orchestration-rules.md`
  - `docs/parallelization-strategy.md`
  - `docs/parity-verification.md`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6-BindPointTeleport-KnownListPetDeadBranchesAndWritePetDataAudit.md`
  - `docs/Phase-6-BindPointTeleport-KnownListPetSurrenderPacket.md`
  - `docs/Phase-6-BindPointTeleport-KnownListPetRenamePacket.md`
  - `docs/Phase-6-BindPointTeleport-LiveAdapter-Readiness.md`
