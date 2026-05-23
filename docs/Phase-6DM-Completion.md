# Phase 6DM Completion Handoff

**Created**: May 23, 2026  
**Status**: Phase 6 remains in progress; this handoff follows 6DL and covers Sessions 572-573.  
**Project rule**: This is a 1:1 parity rewrite from the Java project to C#. Java remains the source of truth for packet layouts, guard order, runtime side effects, static-data semantics, persistence behavior, scheduling, and combat math.  
**Workflow rule**: Do one focused unit of work, validate it, update the migration parity table, commit it, then repeat for as long as useful work remains.  
**Code trace rule**: New C# GameServer parity methods should include a short `Java parity: path::method` comment pointing at the Java source behavior being mirrored.  
**Current validation baseline**: `dotnet test dotnetConversion\AionServer.slnx` passes with 1193 tests.

---

## Recent Work Completed

- Added `SmGroupInfo`, the first focused C# packet serializer for Java `SM_GROUP_INFO.writeImpl`.
- `SmGroupInfo` uses Java server opcode `90` from `ServerPacketsOpcodes`.
- Added a payload test for exact source-derived Java field order: group id, leader id, active-player map id, loot-rule id, loot metadata, constant marker `0x02`, unknown byte `0`, raw team type/subtype, message id `0`, and empty UTF-16 name terminator.
- Kept `SmGroupInfo` live sends disabled; payload order is source-derived, not Java-golden verified.
- Extended reconnect packet planning so `PlayerGroupReconnectPacketPlan` carries a `PlayerGroupInfoPacketPlan` and can instantiate a non-sending `SmGroupInfo`.
- `PlayerGroupRuntime.ReconnectMember` now sources the group-info map id from the reconnecting player's current `WorldPosition.WorldId`, matching the shape of Java's active-player map-id dependency while real connection serialization is still absent.
- Updated `docs/PHASE-6-PROGRESS.md` Sessions 572-573 with required migration parity tables, tests, risks, metrics, and next recommended work.

---

## Commits In This Handoff

- `0437be4fc` - `Add group info packet serializer`
- `aede96cc6` - `Bridge reconnect group info packet intent`

---

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|
| `SM_GROUP_INFO` | `SmGroupInfo` | Partial | Unit Tested | Needs Verification | Payload field order is source-derived and tested. No Java golden vector, encoded opcode/frame comparison, client capture, or live send path has verified end-to-end parity. |
| `ServerPacketsOpcodes` entry for `SM_GROUP_INFO` | `SmGroupInfo.PacketOpCode` | Partial | Unit Tested Around Payload | Needs Verification | C# uses Java opcode `90`, but current focused tests slice off frame headers. |
| `PlayerConnectedEvent` | `PlayerGroupRuntime.ReconnectMember` / `PlayerGroupReconnectPacketPlan` | Partial | Regression Tested | Needs Verification | Reconnect intent can now produce a non-sending `SmGroupInfo`. Java still sends packets, handles member fanout, leader checks, and possible leader recovery. |
| `AionConnection.getActivePlayer` map id dependency | `Player.Position.WorldId` feeding `PlayerGroupInfoPacketPlan.ActivePlayerMapId` | Refactored | Unit Tested | Intentional Difference | C# uses explicit planning input/current player position until the real connection send path exists. |
| `LootGroupRules` | `PlayerGroupLootRules` consumed by `SmGroupInfo` | Partial | Regression Tested | Needs Verification | Packet-facing metadata flows into bytes. Mutable loot-rule changes, roll/bid queues, distribution logic, and client settings packet remain missing. |
| `TeamType.getType/getSubType` | `PlayerGroupType.ToJavaPacketFields` / `PlayerGroupInfoPacketPlan` | Partial | Regression Tested | Needs Verification | Covers `GROUP` and `AUTO_GROUP` only. Alliance/offence/defence variants remain missing. |
| `SM_GROUP_MEMBER_INFO` | `PlayerGroupMemberInfoIntent` only | Not Started | Unit Tested Around Intent | Unknown | Serialization remains deferred because dependencies include life stats, common data, position, fly/mentor state, names, abnormal effects, and slot timers. |
| `ChangeGroupLeaderEvent` | No C# equivalent | Not Started | No Tests | Unknown | Java can recover leadership and broadcast group info; C# does not implement that lifecycle. |

Metrics from this handoff window:

- Total focused sessions covered: 2
- Total commits covered: 2
- Current full validation baseline: 1193 tests passing
- Total artifacts with verified parity newly promoted in this window: 0
- Total blocked artifacts: live reconnect send path, `SM_GROUP_MEMBER_INFO` serialization, member-info fanout, leader recovery, `ChangeGroupLeaderEvent`, group-enter packet sends, loot-rule-change packet sends, encoded opcode/frame golden validation, active connection map-id lookup, mutable loot-rule updates, Java member iteration/order comparison, full team event ordering, and live client/runtime comparison
- Estimated overall migration completion: Phase 6 remains about 63% complete; the first group-info packet payload and reconnect packet object planning exist, but live Java group packet behavior is still incomplete.

---

## Important Limits

- No group packet is sent to a live client.
- No reconnect fanout is wired into `GameServerConnection`.
- `SM_GROUP_MEMBER_INFO` is still not serialized.
- `SmGroupInfo` payload order is tested from source reading, not from a generated Java golden vector.
- Frame/header/opcode encryption parity for `SmGroupInfo` is not verified.
- Active-player map id is read from `Player.Position.WorldId` during C# planning, while Java reads from `AionConnection.getActivePlayer` during packet serialization.
- Leader recovery, group-enter packet sends, leader-change packet sends, and loot-rule-change packet sends remain missing.
- Threading still uses the C# runtime lock, not Java's full team event lock/concurrent-map semantics.

---

## Next Unit Of Work

Recommended next unit: stay on non-sending group packet caller planning and defer `SM_GROUP_MEMBER_INFO` bytes.

Suggested safe scopes:

1. Add non-sending packet-call intent for Java `PlayerGroupEnteredEvent`:
   - Source-read `PlayerGroupEnteredEvent`.
   - Reuse `PlayerGroupInfoPacketPlan` and `SmGroupInfo`.
   - Record that the entering player should receive `SM_GROUP_INFO`.
   - Keep live sends disabled.
2. Or add the first `ChangeGroupLootRulesEvent` planning slice:
   - Source-read `ChangeGroupLootRulesEvent`.
   - Model only the future `SM_GROUP_INFO` broadcast intent after loot-rule changes.
   - Do not implement mutable loot-rule client handling unless `CM_DISTRIBUTION_SETTINGS` is also inspected and scoped.
3. Avoid `SM_GROUP_MEMBER_INFO` serialization until the missing player/stat/effect dependencies are modeled.

Useful validation commands:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerGroupRuntimeTests|FullyQualifiedName~GamePacketTests"
dotnet test dotnetConversion\AionServer.slnx
```

---

## Resume Checklist

- Start from branch `4.8`.
- Confirm `git status --short` is clean.
- Read `docs/csharp-port.md`, latest `docs/PHASE-6-PROGRESS.md` Sessions 572-573, `docs/Phase-6DL-Completion.md`, and this handoff.
- Continue with one focused Java-parity unit.
- Update the migration parity table before committing.
- Commit the unit, then repeat until the next handoff is needed.
