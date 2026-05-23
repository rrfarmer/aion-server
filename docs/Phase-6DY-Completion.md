# Phase 6DY Completion Handoff

Created: May 23, 2026

Status: Phase 6 remains in progress. This handoff follows Phase 6DX and covers Sessions 608-610.

## Ground Rules

- Java remains the source of truth.
- Keep C# code documented with Java breadcrumbs.
- Do not mark parity as verified without byte/runtime/client evidence.
- Continue doing one focused unit, validating it, updating `docs/PHASE-6-PROGRESS.md`, committing it, and repeating.
- Keep the Migration Parity Table, remaining risks, summary metrics, and next recommended work current after every completed unit.

## Validation Baseline

- Latest focused validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "PlayerAllianceRuntimeTests|PlayerAllianceMemberInfoTests|PlayerGroupRuntimeTests"`
  - Result: Passed, 97 tests.
- Latest full GameServer validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj`
  - Result: Passed, 1067 tests.

## Recent Work Completed

### Session 608 - Alliance Ready-Check Runtime Plan

- Source-read Java `CheckAllianceReadyEvent`, `SM_ALLIANCE_READY_CHECK`, `TeamCommand`, `PlayerTeamCommandService`, and `CM_PLAYER_STATUS_INFO`.
- Added `SmAllianceReadyCheck` with opcode `250` and Java payload `D playerObjectId`, `C statusCode`.
- Added `PlayerAllianceReadyCheckCommand`, `PlayerAllianceReadyCheckPlan`, and `PlayerAllianceReadyCheckPacketIntent`.
- Extended `PlayerAllianceRuntime` with Java `allianceReadyStatus` state and ready-check command handling for cancel/start/autocancel/ready/not-ready.
- Commit: `ef76242bd Add alliance ready check runtime plan`

### Session 609 - Alliance Brand Runtime Plan

- Source-read Java `TemporaryPlayerTeam.updateBrand`, `TemporaryPlayerTeam.sendBrands`, `SM_SHOW_BRAND`, `CM_SHOW_BRAND`, `PlayerAllianceEnteredEvent`, and level-ready delayed `sendBrands`.
- Added `PlayerAllianceBrandUpdatePlan` and `PlayerAllianceBrandIntent`.
- Extended `PlayerAllianceRuntime` with alliance brand storage:
  - update-brand broadcast intents;
  - send-current-brands intent;
  - Java empty-map reset behavior through existing `SmShowBrand`.
- Commit: `963bb162e Add alliance brand runtime plan`

### Session 610 - Show Brand Command Plan

- Added `PlayerShowBrandCommandPlanner`, `PlayerShowBrandCommandPlan`, `PlayerShowBrandCommandPlanStatus`, and `PlayerShowBrandIntent`.
- Modeled Java `CM_SHOW_BRAND.runImpl` decision behavior:
  - no current team produces solo echo;
  - group leader updates group brand state;
  - alliance leader or vice-captain updates alliance brand state;
  - unauthorized members no-op;
  - stale C# runtime metadata reports `TeamMissing`.
- Commit: `4823a7ef3 Add show brand command plan`

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.team.alliance.events.CheckAllianceReadyEvent` | `PlayerAllianceRuntime.CheckReady` / `PlayerAllianceReadyCheckPlan` | Event Runtime/Planning Bridge | Partial | Regression Tested | Needs Verification | Ready-status mutation and packet-intent output are modeled. Live `alliance.onEvent`, sockets, and command-service wiring remain missing. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ALLIANCE_READY_CHECK` | `Aion.GameServer.Network.Aion.ServerPackets.SmAllianceReadyCheck` | Server Packet | Partial | Unit Tested | Needs Verification | Opcode `250` and `D/C` payload are modeled. Java golden frames and client validation remain missing. |
| `com.aionemu.gameserver.model.team.common.events.TeamCommand` | `PlayerAllianceReadyCheckCommand` | Enum | Partial | Unit Tested | Needs Verification | Ready-check command ids `20..24` are modeled only; other team commands are outside this enum. |
| `com.aionemu.gameserver.model.team.TemporaryPlayerTeam` brand methods | `PlayerAllianceRuntime.UpdateBrand` / `CreateSendBrandsIntent` | Base Team Runtime Bridge | Partial | Regression Tested | Needs Verification | Alliance brand storage, update broadcast intents, and send-current-brands intent are modeled. Java generic base class and `ConcurrentHashMap` behavior remain unverified. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SHOW_BRAND` | `SmShowBrand` through alliance/group/show-brand intents | Server Packet | Partial | Regression Tested | Needs Verification | Existing packet handles single brand and empty-map reset; new callers are tested. Java golden frames and live socket validation remain missing. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_SHOW_BRAND` | `PlayerShowBrandCommandPlanner` / `PlayerShowBrandCommandPlan` | Client Command Planning Bridge | Partial | Regression Tested | Needs Verification | Run-implementation decisions are modeled. Packet parsing/registration, connection active-player lookup, and live sends remain missing. |
| `com.aionemu.gameserver.model.team.GeneralTeam.isLeader` | `PlayerGroupRuntime.IsLeader` / `PlayerAllianceRuntime.IsLeader` | Authorization Boundary | Partial | Regression Tested | Needs Verification | Group and alliance leader authorization are modeled through runtime descriptors. Java object identity/concurrency remain unverified. |
| `com.aionemu.gameserver.model.team.alliance.PlayerAlliance.isSomeCaptain` | `PlayerAllianceRuntime.IsLeader` + `IsViceCaptain` | Authorization Boundary | Partial | Regression Tested | Needs Verification | Alliance leader and vice-captain brand authorization are modeled. Java mutable vice-captain collection behavior is source-derived only. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_PLAYER_STATUS_INFO` | Deferred ready-check command caller | Client Packet Dependency | Not Started | No Tests | Unknown | Java dispatches ready-check commands through this packet. C# has runtime ready-check planning but no packet/caller wiring yet. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_LEVEL_READY` delayed team brand resend | Deferred scheduler/caller around brand intent | Client Packet / Scheduler Dependency | Not Started | No Tests | Unknown | Java delays `team.sendBrands(activePlayer)` after level ready. C# has brand intent helper but no delayed scheduling. |
| `com.aionemu.gameserver.utils.PacketSendUtility` | Packet intent metadata | Runtime Dependency | Not Started | No Tests | Unknown | Java sends immediately. C# records intents only. |

## Summary Metrics

- Total Java artifacts discovered in this handoff window: 11
- Total artifacts ported or partially modeled in this handoff window: 8
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 8
- Total blocked/not-started artifacts: ready-check command packet caller, show-brand packet parser/handler wiring, connection active-player lookup, delayed level-ready brand resend scheduler, live static/current team object identity, live socket sends, Java runtime/threading comparison, encoded opcode/frame validation, and live client validation.
- Estimated overall migration completion: 64%

The percentage remains conservative. Ready-check, brand runtime state, and show-brand command decisions are now modeled, but live client packet integration and socket fanout are still incomplete.

## Remaining Risks

- `CM_SHOW_BRAND` and `CM_PLAYER_STATUS_INFO` packet parsing/handler wiring are not implemented for these new planners.
- Live `PacketSendUtility` sends are represented as metadata only.
- Java current-team object identity is approximated through C# runtime snapshots/descriptors.
- Java `ConcurrentHashMap`/team lock behavior is source-derived but not runtime-compared.
- Delayed level-ready brand resend remains unmodeled.
- Java golden byte vectors, encrypted opcode/frame validation, and real-client validation remain unavailable.
- Reflection and precision/rounding are not involved; date/time handling remains deferred for delayed brand resend.

## Next Recommended Unit of Work

Continue command integration:

1. Prefer adding a narrow `CM_SHOW_BRAND` client packet parse/handler boundary that reuses `PlayerShowBrandCommandPlanner`, if it can stay small and non-invasive.
2. Otherwise, compose `PlayerAllianceRuntime.CreateSendBrandsIntent` into the existing alliance entered workflow metadata so `PlayerAllianceEnteredPlanner.WouldSendBrands` can become an explicit packet intent.
3. Keep live socket sends and client validation deferred unless the existing connection boundary is already narrow enough.
4. Update `docs/PHASE-6-PROGRESS.md` with the required Migration Parity Table and metrics after the unit.

## Resume Checklist

1. Confirm `git status --short --branch` is clean on branch `4.8`.
2. Read:
   - `docs/csharp-port.md`
   - `docs/PHASE-6-PROGRESS.md`
   - `docs/Phase-6DX-Completion.md`
   - this handoff
3. Start with `CM_SHOW_BRAND` parser/handler wiring or alliance-entered brand intent composition.
4. Implement one narrow unit.
5. Add focused tests.
6. Run focused tests, then at least full GameServer tests.
7. Update `docs/PHASE-6-PROGRESS.md` with the required Migration Parity Table and metrics.
8. Commit the unit.
