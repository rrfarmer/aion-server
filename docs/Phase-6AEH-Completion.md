# Phase 6AEH Completion Handoff

Date: May 27, 2026
Completed Unit of Work: UOW-1302
Latest Commit: included in the UOW-1302 unit commit
Status: Pet static-data function-count audit complete; no serializer behavior added.

## What Changed

- Added `docs/Phase-6-BindPointTeleport-KnownListPetFunctionStaticDataAudit.md`.
- Updated `docs/Phase-6-BindPointTeleport-LiveAdapter-Readiness.md`.
- Updated `docs/PHASE-6-PROGRESS.md`.

No C# production code or tests were changed.

## Audit Evidence

Parsed `game-server/data/static_data/pets/pets.xml` structurally with PowerShell XML APIs.

Key results:

- Pet templates: 218.
- Max XML `petfunction` count: 4.
- Max packet-writable function count among `WAREHOUSE`, `FOOD`, `DOPING`, `LOOT`: 2.
- Pets with more than 2 packet-writable functions: 0.
- Pets with no packet-writable functions: 73.

Java packet serialization order remains hard-coded in `SM_PET.writePetData`: warehouse, loot, doping, food. XML order should not drive C# packet order.

## Validation Completed

- No executable tests were added because this was a documentation/static-data audit unit.
- No Java runtime packet capture was executed.
- No live `GameServerConnection` dispatch was enabled.

Run `git diff --check` before committing; expected line-ending warnings may appear on edited docs.

## Migration Parity Table - UOW-1302

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `game-server/data/static_data/pets/pets.xml` | `docs/Phase-6-BindPointTeleport-KnownListPetFunctionStaticDataAudit.md` | Static Data / XML | Partial | Manual Only | Needs Verification | Structural XML audit found 218 pets, max two packet-writable functions, and no pet with more than two writable functions. This is current-data evidence, not runtime serializer parity. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PET.writePetData` | future `SmPetDataSnapshot` / serializer helper | Packet Serializer | Not Started | Manual Only | Needs Verification | Audit confirms static data currently satisfies Java's two writable function assumption. Serialization still unimplemented in C# for `LOAD_PETS`/`ADOPT`. |
| `com.aionemu.gameserver.model.templates.pet.PetTemplate` | future pet-function snapshot/resolver | Template / Static Data | Not Started | Manual Only | Needs Verification | Java mutates `petFunctions` by adding `NONE` only when no player function exists. Packet writer ignores XML order and ignores `BUFF`, `MERCHANT`, `BAG`, and `WING`. |
| `com.aionemu.gameserver.model.templates.pet.PetFunctionType` | `Aion.GameServer.Model.Templates.Pet.PetFunctionType` | Enum | Partial | Manual Only | Partial Parity | Current C# preserves function ids including overlapping `FOOD`/`APPEARANCE`. This unit did not add serialization tests for function records. |
| `com.aionemu.gameserver.model.templates.pet.PetFunction` | future C# packet function snapshot | DTO / Static Data | Not Started | Manual Only | Needs Verification | XML attributes `type`, `id`, `slots`, and `rate_price` were inspected for function count only. Future packet DTO should include only packet-facing function kind and payload facts. |

## Tests Added

No executable tests were added in UOW-1302. This was a documentation/static-data audit unit.

## Remaining Risks

- The audit used the checked-in XML file, not a Java `DataManager` runtime load.
- XML order varies for some two-function combinations, while Java packet order is hard-coded.
- Future static-data changes could introduce more than two packet-writable functions.
- `PetTemplate.getPetFunctions()` mutation side effects are not modeled in C#.
- `writePetData`, `LOAD_PETS`, `ADOPT`, feed progress, doping slots, and expiration timing remain unimplemented.
- No Java runtime packet vector validates byte output.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 0 code artifacts; 1 static-data audit document
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: `writePetData`, `LOAD_PETS`, `ADOPT`, feed-progress projection, doping-bag projection, deterministic expiration/refeed timing, and Java runtime vector generation
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Add a deterministic packet-facing `SmPetDataSnapshot` / `SmPetFunctionSnapshot` serializer helper and source-derived tests for:

- no writable functions -> two `NONE` pads plus appearance;
- one writable function -> one function record, one `NONE` pad, plus appearance;
- two writable functions -> Java hard-coded function order, no `NONE` pad, plus appearance.

Keep public `LOAD_PETS` and `ADOPT` constructors disabled until the helper is covered.

## Suggested Parallel Batch For Next Session

Use one writer for `SmPet.cs` and `GamePacketTests.cs`.

| Candidate | Scope | Files | Risk | Parallel Safe? | Notes |
|---|---|---|---|---|---|
| A | `SmPetDataSnapshot` / `SmPetFunctionSnapshot` helper and tests | `SmPet.cs`, `GamePacketTests.cs`, docs | Medium | Writer only | Main next implementation. |
| B | `PetFeedProgress` bit-pack helper design | docs/read-only or isolated new helper/test | Medium | Yes if no `SmPet.cs` edits | Useful if separated from serializer writer. |
| C | `CM_PET` parser audit/design | Java/C# read-only docs | Low | Yes | Separate from server packet serializer work. |
| D | Java pet vector generator planning | docs/read-only | Low | Yes | Useful while Maven remains unavailable. |

Recommended next batch: Orchestrator implements Candidate A; optionally run a read-only explorer for Candidate C or D. Do not parallelize multiple writers on `SmPet.cs` or `GamePacketTests.cs`.

## Context Files

- Java source/data:
  - `game-server/data/static_data/pets/pets.xml`
  - `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_PET.java`
  - `game-server/src/com/aionemu/gameserver/model/templates/pet/PetFunctionType.java`
  - `game-server/src/com/aionemu/gameserver/model/templates/pet/PetFunction.java`
  - `game-server/src/com/aionemu/gameserver/model/templates/pet/PetTemplate.java`
  - `game-server/src/com/aionemu/gameserver/model/templates/pet/PetDopingBag.java`
  - `game-server/src/com/aionemu/gameserver/services/toypet/PetFeedProgress.java`
- C# source/tests:
  - `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmPet.cs`
  - `dotnetConversion/src/Aion.GameServer/Model/Templates/Pet/PetFunctionType.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`
- Docs:
  - `docs/csharp-port.md`
  - `docs/orchestration-rules.md`
  - `docs/parallelization-strategy.md`
  - `docs/parity-verification.md`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6-BindPointTeleport-KnownListPetFunctionStaticDataAudit.md`
  - `docs/Phase-6-BindPointTeleport-KnownListPetDeadBranchesAndWritePetDataAudit.md`
  - `docs/Phase-6-BindPointTeleport-LiveAdapter-Readiness.md`
