# Phase 6 Session 2669 Completion

## UOW

[Phase 6] UOW-2669: Send Atreian Passport snapshot.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: CM_ATREIAN_PASSPORT no longer stops at parser-only handling; active in-game players receive SM_ATREIAN_PASSPORT from the live handler.
- Java source/runtime path: CM_ATREIAN_PASSPORT.runImpl -> AtreianPassportService.takeReward -> onLogin/sendPassport -> SM_ATREIAN_PASSPORT.writeImpl.
- C# runtime artifact wired: GameServerConnection.HandleInfrastructurePacketAsync, Player passport snapshot fields, PlayerPassport, and SmAtreianPassport.
- Client-visible/state/persistence effect: an in-game client requesting Atreian Passport data receives a real passport snapshot packet with creation date, stamp count, and modeled passport rows.
- Why this is runtime progress: this wires a deferred client packet path to send a real server packet from live code; it is not a preview, planner, or documentation-only slice.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_ATREIAN_PASSPORT.java`
  - Reads passport id/timestamp pairs and calls `AtreianPassportService.takeReward(player, passports)` when an active player exists.
- `game-server/src/com/aionemu/gameserver/services/AtreianPassportService.java`
  - `takeReward` eventually calls `onLogin(player)`, which calls `sendPassport(player)`.
  - `sendPassport` sends `new SM_ATREIAN_PASSPORT(pa.getPassportsList(), pa.getPassportStamps(), playerCreationDate)`.
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_ATREIAN_PASSPORT.java`
  - Writes character creation date, passport count, then passport id, current stamps, reward status id, and arrival epoch seconds.
- `game-server/src/com/aionemu/gameserver/model/account/Passport.java`
  - Reward status ids are `UPCOMING=0`, `AVAILABLE=1`, `TAKEN=2`, `EXPIRED=3`.
- `game-server/src/com/aionemu/gameserver/network/aion/ServerPacketsOpcodes.java`
  - Registers `SM_ATREIAN_PASSPORT` as server opcode `299`.

## C# Changes

- Added `PlayerPassport` and `PlayerPassportRewardStatus` for the Java serialized passport row shape.
- Added `Player.CreationDate`, `Player.PassportStamps`, and `Player.Passports`.
- Loaded `players.creation_date` in `MySqlPlayerEnterWorldRepository.LoadPlayerAsync`.
- Added `SmAtreianPassport` with Java opcode `299` and Java field order.
- Wired live `CmAtreianPassport` handling to send `SmAtreianPassport` for the active player.

Reward claiming, item granting, passport mutation, and account-passport DB persistence are still intentionally not claimed as ported in this UOW.

## Tests Added / Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `SmAtreianPassport_WritePayload_WritesJavaSnapshotFields` | Unit / packet serialization | `SM_ATREIAN_PASSPORT.writeImpl`, `Passport.getRewardStatus` | Creation date, count, id, stamps, reward status id, and arrival epoch seconds are written in Java order. | Focused byte assertions against reviewed Java write order. | Does not use a Java golden fixture. |
| `HandleInfrastructurePacketAsync_AtreianPassportSendsLiveSnapshotForActivePlayer` | Unit / live packet path | `CM_ATREIAN_PASSPORT.runImpl -> AtreianPassportService.sendPassport` | The live handler sends `SmAtreianPassport` for an active player and serializes modeled player passport state. | Invokes the real connection handler with an active player and inspects the sent server packet. | Does not validate reward mutation, item grants, or DB persistence. |

## Validation Decision

```text
- Changed surface: live in-game packet dispatch plus one new server packet serializer and player snapshot fields.
- Specific behavior/contract: Java CM_ATREIAN_PASSPORT ultimately sends SM_ATREIAN_PASSPORT; the C# live handler now sends the Java-shaped snapshot packet.
- Focused C# command: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmAtreianPassportTests" --no-restore
- Focused Java/Maven command: not run; no narrow Java fixture exists for SM_ATREIAN_PASSPORT in this checkout, and Java source was reviewed directly.
- Broad-validation trigger: live handler wiring touched, but the change is isolated to one packet branch and one packet serializer.
- Broad .NET decision: skipped; the filtered test compiles the affected project and covers parser registration, parser reads, packet bytes, and live handler send.
- Why this scope is sufficient: the focused class validates both the Java packet shape and the live dispatch behavior introduced by this UOW.
```

Results:

- Initial focused run failed at compile due a missing `WorldPosition` namespace in the test; the import was corrected.
- Final focused C# validation passed: 5/5 tests.
- `git diff --check` passed.
- Existing nullable/analyzer warnings were emitted in unrelated surfaces.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `CM_ATREIAN_PASSPORT.runImpl` | `GameServerConnection.HandleInfrastructurePacketAsync` | Live client packet handler | Partial | Unit Tested | Partial Parity | Active players now receive a live passport snapshot packet; reward claiming and item grants remain unported. |
| `SM_ATREIAN_PASSPORT.writeImpl` | `SmAtreianPassport.WritePayload` | Server packet | Partial | Unit Tested | Partial Parity | Java field order and reward status ids covered for modeled rows; full parity needs DB-loaded passport lists and Java golden/runtime comparison. |
| `Passport.getRewardStatus` | `PlayerPassport.RewardStatus` | Model | Partial | Unit Tested | Partial Parity | Available/taken statuses covered; fake-stamp upcoming/taken and expired handling still need broader Passport runtime coverage. |
| `Player.getCreationDate` use in Passport snapshot | `Player.CreationDate` plus `MySqlPlayerEnterWorldRepository.LoadPlayerAsync` | Model / Repository | Partial | Compile Tested | Needs Verification | `players.creation_date` is now loaded for live players; no DB integration test was run for this field in this UOW. |

## Known Gaps

- `account_passports` and `account_stamps` are not yet restored into `Player.Passports`/`Player.PassportStamps`.
- `AtreianPassportService.takeReward` reward mutation, inventory item grant, level checks, expired reward removal, and persistence are not ported.
- `SM_ATREIAN_PASSPORT` has no Java golden fixture.
- Timestamp timezone behavior for DB-loaded passport arrival times remains a future verification risk.

## Summary Metrics

- Total Java artifacts discovered in this UOW: 5
- Total artifacts ported or extended in this UOW: 5
- Total artifacts with verified parity: 0
- Total artifacts needing verification or partial parity: 4
- Total blocked artifacts: 0
- Estimated overall Phase 6 migration completion: 42%

## Next Runtime UOW Candidates

1. Restore `account_passports` and `account_stamps` into the live `Player` during enter-world so `SM_ATREIAN_PASSPORT` sends existing DB-backed account state.
2. Add a narrow `CM_ATREIAN_PASSPORT -> takeReward` mutation slice for already-present, unclaimed passports once DB restore/store shape is in place.
3. Continue discovery over deferred live packet branches that can send deterministic packets or mutate already-modeled state.
