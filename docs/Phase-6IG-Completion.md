# Phase 6IG Completion Handoff

Created: May 24, 2026

Status: Phase 6 remains in progress. This handoff follows Phase 6IF and covers Session 729.

## Ground Rules

- Java remains the source of truth.
- Keep C# code documented with Java breadcrumbs.
- Do not mark parity as verified without byte/runtime/client evidence.
- Continue doing one focused unit, validating it, updating `docs/PHASE-6-PROGRESS.md`, committing it, and repeating.
- Keep the Migration Parity Table, remaining risks, summary metrics, and next recommended work current after every completed unit.

## Validation Baseline

- Latest focused validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter GamePacketTests`
  - Result: Passed, 84 tests.
- Latest full GameServer validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj`
  - Result: Passed, 1308 tests.

## Recent Work Completed

### Session 729 - CM_ATTACK Client Packet Parser

- Inspected Java combat client packet registrations and C# packet factory coverage.
- Added `CmAttack` as the C# parser for Java `CM_ATTACK.readImpl`.
- Registered opcode 32 in `GameClientPacketFactory` for `InGame`.
- Added packet factory tests proving Java-shaped payload parsing and invalid-state rejection.
- Left runtime attack dispatch unwired because C# still lacks a complete auto-attack controller route, target known-list lookup parity, and full combat observer integration.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_ATTACK` | `Aion.GameServer.Network.Aion.ClientPackets.CmAttack` | Client Packet | Partial | Unit Tested | Needs Verification | C# parses target object id, attack number, time, and type in Java field order. Runtime `runImpl` behavior remains missing: dead guard, protection cancellation, known-list target lookup, unsupported target logging, and `PlayerController.attackTarget`. |
| `com.aionemu.gameserver.network.aion.AionClientPacketFactory` opcode 32 registration | `Aion.GameServer.Network.Aion.GameClientPacketFactory` opcode 32 registration | Packet Factory | Partial | Unit Tested | Needs Verification | Opcode 32 is now registered for `InGame`. Java reflection construction differs intentionally from C# explicit factory lambdas. Live encrypted-frame and Java runtime comparisons remain unverified. |
| `com.aionemu.gameserver.controllers.PlayerController.attackTarget` | No complete C# equivalent wired to `CmAttack` yet | Combat Controller Dependency | Not Started | No Tests | Needs Verification | Required dependency discovered for live auto-attack behavior. Full auto-attack dispatch, timing, `AttackUtil`, result lists, observer firing, charge/power-shard/idian burns, PvP, death, and packet fanout remain missing. |
| `com.aionemu.gameserver.model.gameobjects.player.Player` combat guard behavior | C# `Player` plus no `CmAttack` infrastructure handler yet | Player Model Dependency | Partial | No Tests for this packet route | Needs Verification | Java checks dead state and protection state before attacking. C# has player state elsewhere, but this packet route does not yet execute those guards. Threading behavior is unverified. |
| `com.aionemu.gameserver.model.gameobjects.KnownList.getObject` / `VisibleObject` / `Creature` target narrowing | C# world/visibility surfaces not wired to `CmAttack` | Target Lookup Dependency | Not Started | No Tests | Needs Verification | Java attacks only known `Creature` targets and logs unsupported visible objects. C# target lookup for this route is not implemented. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_CASTSPELL` | Not yet ported | Skill Client Packet Dependency | Not Started | No Tests | Needs Verification | Inspection identified opcode 33 as the next parser candidate. It includes target-type-dependent payloads and receive-time/hit-time skill readiness behavior, so date/time comparison remains a future risk. |

## Tests Added Or Updated

- `GamePacketTests.ClientPacketFactory_ParsesAttack`
  - Validates opcode 32 creates `CmAttack` in `InGame`.
  - Validates Java `CM_ATTACK.readImpl` field order: `readD`, `readUC`, `readUH`, `readUC`.
  - Validates invalid-state rejection for `Authed`.
  - Does not compare against Java runtime bytes or live client behavior.

Existing `GamePacketTests` were rerun as focused validation. The full GameServer test suite was rerun.

These tests are source-derived from Java. They do not compare against Java runtime execution, Java-generated golden packets, live `GameServerConnection`, known-list lookup, real player controller attack path, encrypted frame order, live client behavior, reflection behavior, threading behavior, or date/time behavior.

## Summary Metrics

- Total Java artifacts discovered in this handoff window: 6
- Total artifacts ported or partially modeled in this handoff window: 2 parser/factory slices for `CM_ATTACK`.
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 6
- Total blocked/not-started artifacts: live auto-attack route invocation, full `PlayerController.attackTarget`/`AttackUtil` integration, target known-list lookup parity, Java runtime/golden packet comparison, and live-client validation.
- Estimated overall migration completion: 65%

## Remaining Risks

- `CmAttack` is parsed and registered but not handled by `GameServerConnection`; auto-attack behavior is not live.
- Full Java player combat behavior, `AttackUtil`, observer dispatch, result-list handling, charge/power-shard/idian burn triggers, PvP/death workflow, and packet fanout remain missing.
- Target lookup and unsupported visible-object logging are not implemented for this route.
- Java reflection construction differs intentionally from C# explicit factory lambdas.
- No Java golden bytes, encrypted-frame capture, threading comparison, date/time comparison, or live client validation was run.

## Next Recommended Unit of Work

Port `CM_CASTSPELL` opcode 33 as the next combat client parser. Start with parser/DTO parity and tests for object-target payloads (`targetType` 0/3/4) and point-target payloads (`targetType` 1/2). Preserve Java breadcrumbs for receive-time, spell id, level, target type, coordinates, hit time, and unknown fields. Do not wire runtime `useSkill` dispatch unless the C# skill controller route is ready; if it is not ready, document the missing pet-order, passive-skill, cooldown-audit, cancel-use-item, `SkillEngine`, and represented damage/observer fanout gaps.

## Resume Checklist

1. Confirm `git status --short --branch` is clean on branch `4.8`.
2. Read:
   - `docs/csharp-port.md`
   - `docs/PHASE-6-PROGRESS.md`
   - `docs/Phase-6IF-Completion.md`
   - this handoff
3. Inspect Java `CM_CASTSPELL.java`, `CM_ATTACK.java`, and `AionClientPacketFactory.java`.
4. Inspect C# `GameClientPacketFactory`, `CmAttack`, and `GamePacketTests`.
5. Implement one narrow parser or caller unit with Java breadcrumbs.
6. Add focused tests that state what is source-derived and what remains unverified.
7. Run focused tests, then full GameServer tests.
8. Update `docs/PHASE-6-PROGRESS.md` with the required Migration Parity Table and metrics.
9. Create the next handoff document and commit the unit.
