# Phase 6DP Completion Handoff

Created: May 23, 2026

Status: Phase 6 remains in progress. This handoff follows Phase 6DO and covers the completed Phase 6 units for Sessions 578 and 579.

## Ground Rules

- Java is still the source of truth for every behavior decision.
- Keep C# code documented with Java breadcrumbs when porting behavior.
- Do not mark parity as verified unless byte output, deterministic runtime behavior, or direct Java comparison proves it.
- After each completed unit, update `docs/PHASE-6-PROGRESS.md` with a Migration Parity Table, Remaining Risks, Next Recommended Unit of Work, and summary metrics.
- Commit each completed unit separately.

## Validation Baseline

- Focused group/runtime packet tests passed after each unit.
- Full solution baseline after Session 579:
  - `dotnet test dotnetConversion\AionServer.slnx`
  - Result: Passed, 1201 tests.

## Recent Work Completed

### Session 578 - Group Brand Packet Intent

- Source-read Java:
  - `com.aionemu.gameserver.model.team2.group.TemporaryPlayerTeam.updateBrand`
  - `com.aionemu.gameserver.model.team2.group.TemporaryPlayerTeam.sendBrands`
  - `com.aionemu.gameserver.network.aion.serverpackets.SM_SHOW_BRAND`
  - `com.aionemu.gameserver.network.aion.clientpackets.CM_SHOW_BRAND`
- Added C# `Aion.GameServer.Network.Aion.ServerPackets.SmShowBrand`.
  - Packet opcode: `249`.
  - Single-brand constructor writes one brand update.
  - Map constructor writes all known brand mappings.
  - Empty-map reset writes brand ids `0..15` with target object id `0`, matching Java `SM_SHOW_BRAND(Map<Integer, Integer>)`.
- Added non-sending group brand planning:
  - `PlayerGroupBrandIntent`
  - `PlayerGroupBrandUpdatePlan`
  - `PlayerGroupRuntime.UpdateBrand`
- Group-entered packet plans now include a brand replay intent for the entering player.
- Tests added or extended:
  - `GamePacketTests.SmShowBrand_WritesSingleBrandAndEmptyMapResetLikeJava`
  - `PlayerGroupRuntimeTests.UpdateBrand_StoresBrandAndPlansBroadcastLikeJavaTemporaryPlayerTeam`
  - `PlayerGroupRuntimeTests.UpdateBrand_ReturnsNullForUnknownGroup`
  - `PlayerGroupRuntimeTests.CreateEnteredPacketPlan_ReturnsNonSendingGroupInfoPlanLikeJavaPlayerGroupEnteredEvent`
- Commit: `a0b74c2d0 Add group brand packet intent`

### Session 579 - Group Member Info Packet Planning Prerequisite

- Source-read Java:
  - `com.aionemu.gameserver.network.aion.serverpackets.SM_GROUP_MEMBER_INFO.writeImpl`
  - `com.aionemu.gameserver.model.team2.common.events.GroupEvent`
- Added `PlayerGroupMemberInfoPacketPlan`.
  - Models group id, member id, requested event, effective event, slot, online state, and major Java write branches.
  - Converts offline `ENTER` member info into effective `ENTER_OFFLINE`, matching Java `SM_GROUP_MEMBER_INFO`.
  - Tracks stable branch flags for life stats, position, common data, name, abnormal effects, and slot timers.
- Reconnect member-info intents now carry packet plans instead of only destination/subject/event metadata.
- Tests added or extended:
  - `PlayerGroupRuntimeTests.ReconnectMember_ReturnsNonSendingPacketIntentPlanLikeJavaPlayerConnectedEvent`
  - `PlayerGroupRuntimeTests.PlayerGroupMemberInfoPacketPlan_ModelsStableJavaHeaderAndEventBranches`
- Commit: `a54422a71 Add group member info packet plan`

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SHOW_BRAND` | `Aion.GameServer.Network.Aion.ServerPackets.SmShowBrand` | Packet | Partial | Unit Tested | Needs Verification | Opcode and primitive payload order are covered for single-brand and empty reset payloads. Broader runtime fanout and capture against Java packet bytes are still pending. |
| `com.aionemu.gameserver.model.team2.group.TemporaryPlayerTeam.updateBrand` | `Aion.GameServer.Services.PlayerGroupRuntime.UpdateBrand` | Runtime service behavior | Partial | Unit Tested | Needs Verification | Stores brand targets and returns non-sending broadcast intents. Java captain/alliance permission path is not ported. Live send path is deferred. |
| `com.aionemu.gameserver.model.team2.group.TemporaryPlayerTeam.sendBrands` | `Aion.GameServer.Services.PlayerGroupEnteredPacketPlan.BrandIntent` | Runtime planning behavior | Partial | Regression Tested | Needs Verification | Entering player receives a brand replay intent. Full Java team fanout and packet dispatch are still deferred. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_SHOW_BRAND` | Not implemented | Client packet | Not Started | No Tests | Unknown | Java request parsing and leader/alliance permission rules are discovered dependencies. No C# client packet exists yet. |
| `com.aionemu.gameserver.model.team2.alliance.PlayerAlliance.isSomeCaptain` | Not implemented | Permission dependency | Not Started | No Tests | Unknown | Needed before `CM_SHOW_BRAND` can enforce Java-equivalent command permission behavior. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_GROUP_MEMBER_INFO` | `Aion.GameServer.Services.PlayerGroupMemberInfoPacketPlan` | Packet planning model | Partial | Unit Tested | Needs Verification | Stable header/event branch planning is modeled. Actual byte serialization, life/common/position values, effects, slot timers, and equipment/model data are not yet ported. |
| `com.aionemu.gameserver.model.team2.common.events.GroupEvent` | `Aion.GameServer.Services.PlayerGroupEvent` | Enum | Partial | Regression Tested | Needs Verification | Existing enum values are consumed by reconnect/member-info planning. Numeric wire values still need packet serialization verification. |
| `com.aionemu.gameserver.model.team2.group.events.PlayerConnectedEvent` | `Aion.GameServer.Services.PlayerGroupReconnectPacketPlan` | Event planning behavior | Partial | Regression Tested | Needs Verification | Reconnect member-info intent count and effective offline event branch are covered. Live send ordering and byte packets are deferred. |
| `com.aionemu.gameserver.model.gameobjects.player.PlayerLifeStats` | Not implemented for group member packet | Dependency | Not Started | No Tests | Unknown | Required for `SM_GROUP_MEMBER_INFO` fixed prefix values: max/current HP, max/current MP, and DP. |
| `com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData` | Not implemented for group member packet | Dependency | Not Started | No Tests | Unknown | Required for level, class, gender, race, name, online status, and related common-data fields. |
| `com.aionemu.gameserver.model.gameobjects.player.WorldPosition` | Not implemented for group member packet | Dependency | Not Started | No Tests | Unknown | Required for map id and x/y/z coordinates. Java float/integer precision must be preserved when serialized. |
| `com.aionemu.gameserver.model.gameobjects.PersistentState` and effect dependencies used by `SM_GROUP_MEMBER_INFO` | Not implemented for group member packet | Dependency | Not Started | No Tests | Unknown | Abnormal-effect serialization is still missing. Reflection/serialization differences must be treated carefully when effects are ported. |

## Summary Metrics

- Total Java artifacts discovered in this handoff window: 12
- Total artifacts ported or partially modeled in this handoff window: 6
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 6
- Total blocked/not-started artifacts: 6
- Estimated overall migration completion: 63%

The completion estimate is intentionally conservative. It reflects incremental Phase 6 group/team packet progress, not full project parity.

## Remaining Risks

- `SM_SHOW_BRAND` has focused byte tests, but no live Java-vs-C# packet capture comparison yet.
- `CM_SHOW_BRAND` is not ported, so permission checks, request parsing, and leader/alliance validation are still missing.
- Brand update fanout is represented as non-sending intents only; real `PacketSendUtility` integration remains deferred.
- `SM_GROUP_MEMBER_INFO` is still a planning model, not a serializer.
- Life stats, common data, position, effects, slot cooldowns, model/equipment data, and Java-specific null/default behavior are not yet represented.
- Date/time handling is not involved in these two units.
- Precision/rounding risks remain for future position serialization.
- Reflection risks remain for future abnormal-effect serialization.
- Threading risks remain low for the current in-memory planning work, but live group fanout may need concurrency review later.

## Next Recommended Unit of Work

Add the next smallest `SM_GROUP_MEMBER_INFO` dependency model: a packet-facing member snapshot DTO for life stats plus common/position data needed by the fixed prefix after group id/member id.

Keep the next unit intentionally narrow:

- Source-read Java `SM_GROUP_MEMBER_INFO.writeImpl` again before editing.
- Identify the exact fixed-prefix fields after group id and member id.
- Add a C# snapshot record that can carry HP/MP/DP, level, class, race/gender, map id, and position fields without requiring live `Player` internals.
- Wire the snapshot into `PlayerGroupMemberInfoPacketPlan`.
- Add tests for `ENTER`, `ENTER_OFFLINE`, and `UPDATE` plan snapshots.
- Do not serialize the full packet until the fixed prefix can be byte-tested without placeholders.
- Keep abnormal effects, slot timers, equipment/model data, and live sends deferred unless the prefix work requires a small explicit placeholder with documented risk.

## Resume Checklist

1. Run `git status --short --branch` and confirm the current branch/worktree.
2. Read:
   - `docs/csharp-port.md`
   - `docs/PHASE-6-PROGRESS.md`
   - This handoff document
   - The latest previous handoff if extra context is needed
3. Source-read Java `SM_GROUP_MEMBER_INFO.writeImpl` and its immediate player-data dependencies.
4. Implement the next narrow DTO/planning slice.
5. Add or update focused tests.
6. Run focused tests, then full `dotnet test dotnetConversion\AionServer.slnx` if feasible.
7. Update `docs/PHASE-6-PROGRESS.md` with the required Migration Parity Table and summary sections.
8. Commit the unit.
9. Repeat if time/context allows.
