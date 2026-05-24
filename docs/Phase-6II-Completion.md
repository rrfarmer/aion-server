# Phase 6II Completion Handoff

Created: May 24, 2026

Status: Phase 6 remains in progress. This handoff follows Phase 6IH and covers Session 731.

## Ground Rules

- Java remains the source of truth.
- Keep C# code documented with Java breadcrumbs.
- Do not mark parity as verified without byte/runtime/client evidence.
- Continue doing one focused unit, validating it, updating `docs/PHASE-6-PROGRESS.md`, committing it, and repeating.
- Keep the Migration Parity Table, remaining risks, summary metrics, and next recommended work current after every completed unit.

## Validation Baseline

- Latest focused validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter GamePacketTests`
  - Result: Passed, 89 tests.
- Latest full GameServer validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj`
  - Result: Passed, 1313 tests.

## Recent Work Completed

### Session 731 - Summon Combat Client Packet Parsers

- Added `CmSummonAttack` for Java `CM_SUMMON_ATTACK.readImpl`.
- Added `CmSummonCastSpell` for Java `CM_SUMMON_CASTSPELL.readImpl`.
- Registered opcodes 203 and 205 in `GameClientPacketFactory` for `InGame`.
- Added packet factory tests for both fixed payloads and invalid-state rejection.
- Left runtime summon/mercenary attack and skill dispatch unwired because summon ownership lookup, known-list targeting, summon skill orders, pet skill validation, controller dispatch, audit logging, and system-message fanout are not ready.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_SUMMON_ATTACK` | `Aion.GameServer.Network.Aion.ClientPackets.CmSummonAttack` | Client Packet | Partial | Unit Tested | Needs Verification | C# parses summon object id, target object id, unknown byte, time, and trailing unknown byte in Java field order. Runtime `runImpl` behavior remains missing. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_SUMMON_CASTSPELL` | `Aion.GameServer.Network.Aion.ClientPackets.CmSummonCastSpell` | Client Packet | Partial | Unit Tested | Needs Verification | C# parses summon object id, skill id, skill level, target object id, and unknown int in Java field order. Runtime `runImpl` behavior remains missing. |
| `com.aionemu.gameserver.network.aion.AionClientPacketFactory` opcodes 203 and 205 registration | `Aion.GameServer.Network.Aion.GameClientPacketFactory` opcodes 203 and 205 registration | Packet Factory | Partial | Unit Tested | Needs Verification | Opcodes 203 and 205 are now registered for `InGame`. Java reflection construction differs intentionally from C# explicit factory lambdas. Live encrypted-frame and Java runtime comparisons remain unverified. |
| `com.aionemu.gameserver.model.gameobjects.player.Player.getSummonOrMercenary` | No C# summon/mercenary packet route wired yet | Summon Ownership Dependency | Not Started | No Tests | Needs Verification | Required dependency for both runtime routes. Ownership, dead/despawn lag cases, and lookup semantics are unmodeled here. |
| `com.aionemu.gameserver.model.gameobjects.KnownList.getObject` / `VisibleObject` / `Creature` target narrowing | C# world/visibility surfaces not wired to summon combat packets | Target Lookup Dependency | Not Started | No Tests | Needs Verification | Java uses summon/mercenary known-list lookup and ignores lagged null targets. Unsupported visible-object audit logging remains missing. |
| `com.aionemu.gameserver.model.gameobjects.Summon` / `com.aionemu.gameserver.model.summons.SkillOrder` | No C# summon skill-order route wired yet | Summon Skill Dependency | Not Started | No Tests | Needs Verification | Java retrieves queued skill orders, verifies target equality, warns on skill mismatch, and dispatches controller skill use. No equivalent handler was added. |
| `com.aionemu.gameserver.dataholders.DataManager.PET_SKILL_DATA` | No C# pet-skill validation wired to summon cast-spell yet | Skill Data Dependency | Not Started | No Tests | Needs Verification | Java validates mercenary skill availability through pet skill data before controller dispatch. This route remains unimplemented in C#. |
| `com.aionemu.gameserver.utils.audit.AuditLogger` and `SM_SYSTEM_MESSAGE.STR_SKILL_NOT_NEED_PET` | Existing C# logging/system-message surfaces, not wired to summon combat packets | Audit / Packet Dependency | Partial | No Tests for this route | Needs Verification | Java audits wrong target/invalid mercenary skill and sends pet-required messages for missing/invalid summon cases. Parser work does not produce those side effects. |

## Tests Added Or Updated

- `GamePacketTests.ClientPacketFactory_ParsesSummonAttack`
  - Validates opcode 203 creates `CmSummonAttack` in `InGame`.
  - Validates Java `CM_SUMMON_ATTACK.readImpl` field order.
  - Validates invalid-state rejection for `Authed`.
- `GamePacketTests.ClientPacketFactory_ParsesSummonCastSpell`
  - Validates opcode 205 creates `CmSummonCastSpell` in `InGame`.
  - Validates Java `CM_SUMMON_CASTSPELL.readImpl` field order.
  - Validates invalid-state rejection for `Authed`.

Existing `GamePacketTests` were rerun as focused validation. The full GameServer test suite was rerun.

These tests are source-derived from Java. They do not compare against Java runtime execution, Java-generated golden packets, live `GameServerConnection`, live summon/mercenary controller behavior, encrypted-frame order, live client behavior, reflection behavior, threading behavior, or date/time behavior.

## Summary Metrics

- Total Java artifacts discovered in this handoff window: 8
- Total artifacts ported or partially modeled in this handoff window: 4 parser/factory slices for `CM_SUMMON_ATTACK` and `CM_SUMMON_CASTSPELL`.
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 8
- Total blocked/not-started artifacts: live summon combat route invocation, summon/mercenary ownership lookup, known-list target parity, summon skill-order/pet-skill controller integration, and Java runtime/golden/live-client comparison.
- Estimated overall migration completion: 65%

## Remaining Risks

- `CmSummonAttack` and `CmSummonCastSpell` are parsed and registered but not handled by `GameServerConnection`; summon combat behavior is not live.
- Full Java summon/mercenary lookup, known-list target lookup, summon skill-order retrieval, mercenary skill validation, controller attack/skill dispatch, audit logging, pet-required system messages, observer dispatch, and combat packet fanout remain missing.
- Java lag-tolerance behavior for missing summon/target objects is source-discovered but not modeled.
- No Java golden bytes, encrypted-frame capture, threading comparison, date/time comparison, or live client validation was run.

## Next Recommended Unit of Work

Move from parser coverage into a narrow represented combat packet handling seam. Prefer `CmCastSpell` early-exit handling as a delegate-backed service so Java ordering can be tested without full `SkillEngine`: dead-player rejection, spell id zero cancel-current-skill hook, missing pet-order rejection, missing/passive template no-op, protection/use-item cancellation hooks, and cooldown not-ready response. If model support is still too thin, add the simpler `CM_USE_CHARGE_SKILL` opcode 234 no-payload parser and document its missing casting-skill/charge-time runtime behavior.

## Resume Checklist

1. Confirm `git status --short --branch` is clean on branch `4.8`.
2. Read:
   - `docs/csharp-port.md`
   - `docs/PHASE-6-PROGRESS.md`
   - `docs/Phase-6IH-Completion.md`
   - this handoff
3. Inspect Java `CM_CASTSPELL.java`, `CM_USE_CHARGE_SKILL.java`, `CM_SUMMON_ATTACK.java`, `CM_SUMMON_CASTSPELL.java`, and `AionClientPacketFactory.java`.
4. Inspect C# `GameClientPacketFactory`, `CmCastSpell`, `CmSummonAttack`, `CmSummonCastSpell`, and `GamePacketTests`.
5. Implement one narrow parser or caller unit with Java breadcrumbs.
6. Add focused tests that state what is source-derived and what remains unverified.
7. Run focused tests, then full GameServer tests.
8. Update `docs/PHASE-6-PROGRESS.md` with the required Migration Parity Table and metrics.
9. Create the next handoff document and commit the unit.
