# Phase 6 Bind-Point Teleport Known-List Pet Function Static-Data Audit

Date: May 27, 2026
Unit of Work: UOW-1302
Status: Static-data audit complete; no serializer behavior added.

## Scope

This unit audits `game-server/data/static_data/pets/pets.xml` before implementing Java `SM_PET.writePetData(PetCommonData)`.

The question from UOW-1301 was whether Java's comment, "Pets have only 2 functions max," is true for the packet-writable functions used by `writePetData`.

Result: for the current Java static data, no pet has more than two packet-writable functions among:

- `WAREHOUSE`
- `FOOD`
- `DOPING`
- `LOOT`

Some pets have up to four total XML functions, but the extra functions are non-written or currently ignored by `writePetData`, such as `BAG`, `WING`, `BUFF`, and `MERCHANT`.

No C# production code, tests, live dispatch, or serializer behavior was changed in this unit.

## Command Evidence

The audit parsed `pets.xml` structurally with PowerShell XML APIs.

Summary:

| Metric | Value |
|---|---:|
| Pet templates | 218 |
| Max XML `petfunction` count | 4 |
| Max packet-writable function count | 2 |
| Pets with more than 2 packet-writable functions | 0 |
| Pets with no XML functions | 0 |
| Pets with no packet-writable functions | 73 |

Writable function count distribution:

| Writable Count | Pet Count |
|---:|---:|
| 0 | 73 |
| 1 | 111 |
| 2 | 34 |

Function type distribution:

| Function Type | Count |
|---|---:|
| `BAG` | 69 |
| `BUFF` | 6 |
| `DOPING` | 62 |
| `FOOD` | 35 |
| `LOOT` | 35 |
| `MERCHANT` | 5 |
| `WAREHOUSE` | 47 |
| `WING` | 179 |

Writable combinations observed:

| Writable Combination | Pet Count |
|---|---:|
| none | 73 |
| `DOPING` | 41 |
| `WAREHOUSE` | 32 |
| `FOOD` | 22 |
| `LOOT` | 16 |
| `DOPING,LOOT` | 10 |
| `WAREHOUSE,FOOD` | 5 |
| `DOPING,FOOD` | 4 |
| `LOOT,DOPING` | 3 |
| `WAREHOUSE,LOOT` | 3 |
| `DOPING,WAREHOUSE` | 3 |
| `FOOD,WAREHOUSE` | 2 |
| `LOOT,WAREHOUSE` | 1 |
| `WAREHOUSE,DOPING` | 1 |
| `FOOD,LOOT` | 1 |
| `LOOT,FOOD` | 1 |

The combination table preserves XML order. Java packet serialization does not preserve that order; `SM_PET.writePetData` checks and writes functions in hard-coded order:

1. `WAREHOUSE`
2. `LOOT`
3. `DOPING`
4. `FOOD`

## Java Source Reviewed

- `game-server/data/static_data/pets/pets.xml`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_PET.java`
- `game-server/src/com/aionemu/gameserver/model/templates/pet/PetFunctionType.java`
- `game-server/src/com/aionemu/gameserver/model/templates/pet/PetFunction.java`
- `game-server/src/com/aionemu/gameserver/model/templates/pet/PetTemplate.java`

## Interpretation For C# Port

Current static data supports Java's packet comment if "functions" means packet-writable functions. It does not hold for total XML functions because `BAG` and `WING` often appear with one or two writable functions.

Future C# `writePetData` should:

- write functions in Java hard-coded order, not XML order;
- write at most the four Java-recognized packet functions;
- pad with `NONE` only when zero or one packet-writable function is emitted;
- include a guard/diagnostic if supplied packet-facing function snapshots contain more than two writable functions;
- not serialize `BUFF`, `MERCHANT`, `BAG`, or `WING` in `SM_PET.writePetData`;
- preserve the overlapping numeric id for `FOOD` and `APPEARANCE` separately by context.

The guard is still important because future XML/static-data changes could violate the current observed limit.

## Validation

- No executable tests were added in this documentation/static-data audit unit.
- No Java runtime packet capture was executed.
- No live `GameServerConnection` dispatch was enabled.

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
