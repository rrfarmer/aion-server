# Phase 6IH Completion Handoff

Created: May 24, 2026

Status: Phase 6 remains in progress. This handoff follows Phase 6IG and covers Session 730.

## Ground Rules

- Java remains the source of truth.
- Keep C# code documented with Java breadcrumbs.
- Do not mark parity as verified without byte/runtime/client evidence.
- Continue doing one focused unit, validating it, updating `docs/PHASE-6-PROGRESS.md`, committing it, and repeating.
- Keep the Migration Parity Table, remaining risks, summary metrics, and next recommended work current after every completed unit.

## Validation Baseline

- Latest focused validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter GamePacketTests`
  - Result: Passed, 87 tests.
- Latest full GameServer validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj`
  - Result: Passed, 1311 tests.

## Recent Work Completed

### Session 730 - CM_CASTSPELL Client Packet Parser

- Added `CmCastSpell` as the C# parser for Java `CM_CASTSPELL.readImpl`.
- Registered opcode 33 in `GameClientPacketFactory` for `InGame`.
- Covered Java object-target, point-target, and extended-point-target payload branches.
- Captured construction-time receive milliseconds to preserve the Java packet-level `receiveTime` concept for later cooldown auditing.
- Left runtime skill dispatch unwired because C# still lacks full skill-template lookup, pet-order validation, cooldown audit behavior, and `PlayerController.useSkill` parity.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_CASTSPELL` | `Aion.GameServer.Network.Aion.ClientPackets.CmCastSpell` | Client Packet | Partial | Unit Tested | Needs Verification | C# parses spell id, level, target type, target object id or coordinates, extended target-type-2 floats, hit time, and unknown int in Java field order. Runtime `runImpl` behavior remains missing. |
| `com.aionemu.gameserver.network.aion.AionClientPacketFactory` opcode 33 registration | `Aion.GameServer.Network.Aion.GameClientPacketFactory` opcode 33 registration | Packet Factory | Partial | Unit Tested | Needs Verification | Opcode 33 is now registered for `InGame`. Java reflection construction differs intentionally from C# explicit factory lambdas. Live encrypted-frame and Java runtime comparisons remain unverified. |
| `com.aionemu.gameserver.skillengine.model.SkillTemplate` / `DataManager.SKILL_DATA` | No complete C# skill-template route wired to `CmCastSpell` yet | Skill Data Dependency | Not Started | No Tests | Needs Verification | Template lookup, passive-skill filtering, skill level validation, and dispatch into the full skill/effect engine remain missing. |
| `com.aionemu.gameserver.dataholders.DataManager.PET_SKILL_DATA` | No C# pet-order skill validation wired to `CmCastSpell` yet | Skill Data Dependency | Not Started | No Tests | Needs Verification | Java rejects pet-order skills when no pet summon exists. C# pet skill data and summon checks are not connected to this packet route. |
| `com.aionemu.gameserver.controllers.PlayerController.useSkill` / `cancelCurrentSkill` / `cancelUseItem` | No complete C# controller equivalent wired to `CmCastSpell` yet | Skill Controller Dependency | Not Started | No Tests | Needs Verification | Live skill dispatch, cancel behavior, target/result-list handling, skill damage, effects, observer firing, charge/power-shard/idian burn hooks, and packet fanout remain missing. |
| `com.aionemu.gameserver.utils.audit.AuditLogger` cooldown audit path | No C# cooldown audit wired to `CmCastSpell` yet | Audit / Timing Dependency | Not Started | No Tests | Needs Verification | C# captures `ReceiveTimeMilliseconds` at packet construction like Java's `System.currentTimeMillis()`, but cooldown comparison and not-ready behavior are not implemented. Date/time behavior remains unverified. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` skill failure responses | Existing C# system-message packet helpers, not wired to `CmCastSpell` | Packet Dependency | Partial | No Tests for this route | Needs Verification | Java sends skill-cannot-cast, pet-required, and skill-not-ready messages in `runImpl`. This unit does not wire those responses to a live packet handler. |

## Tests Added Or Updated

- `GamePacketTests.ClientPacketFactory_ParsesCastSpellObjectTarget`
  - Validates opcode 33 object-target parsing for target type 3, receive-time capture, hit time, unknown int, and invalid-state rejection.
- `GamePacketTests.ClientPacketFactory_ParsesCastSpellPointTarget`
  - Validates target type 1 coordinate parsing and trailing fields.
- `GamePacketTests.ClientPacketFactory_ParsesCastSpellExtendedPointTarget`
  - Validates target type 2 coordinate parsing plus eight extra float reads before hit time.

Existing `GamePacketTests` were rerun as focused validation. The full GameServer test suite was rerun.

These tests are source-derived from Java. They do not compare against Java runtime execution, Java-generated golden packets, live `GameServerConnection`, live `SkillEngine`, live player controller `useSkill`, encrypted-frame order, live client behavior, reflection behavior, threading behavior, or date/time behavior.

## Summary Metrics

- Total Java artifacts discovered in this handoff window: 7
- Total artifacts ported or partially modeled in this handoff window: 2 parser/factory slices for `CM_CASTSPELL`.
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 7
- Total blocked/not-started artifacts: live skill-use route invocation, full `SkillEngine`/player-controller integration, pet/order/template validation, cooldown/date-time audit parity, and Java runtime/golden/live-client comparison.
- Estimated overall migration completion: 65%

## Remaining Risks

- `CmCastSpell` is parsed and registered but not handled by `GameServerConnection`; real skill use is not live.
- Full Java skill behavior, template lookup, pet-order validation, passive-skill rejection, cooldown auditing, item-use cancellation, target/result-list handling, PvP/death behavior, effect scheduling, observer dispatch, charge/power-shard/idian burns, and packet fanout remain missing.
- Java receive-time behavior is only represented at construction time; no cooldown comparison or Java clock-semantics validation was performed.
- C# retains target-type-2 extra floats for diagnostics even though Java discards them after reading. Treat that as parser metadata only unless Java behavior is proven.
- No Java golden bytes, encrypted-frame capture, threading comparison, date/time comparison, or live client validation was run.

## Next Recommended Unit of Work

Add a represented `CmCastSpell` infrastructure handling seam that validates Java's early-exit order without invoking the full skill engine: dead-player rejection, spell id zero cancel-current-skill hook, missing pet-order rejection, missing/passive template no-op, protection/use-item cancellation hooks, and cooldown not-ready response as delegates or a narrow service. If that proves too broad for the current model surface, continue parser coverage with summon combat packets (`CM_SUMMON_ATTACK` / `CM_SUMMON_CASTSPELL`) while keeping runtime gaps explicit.

## Resume Checklist

1. Confirm `git status --short --branch` is clean on branch `4.8`.
2. Read:
   - `docs/csharp-port.md`
   - `docs/PHASE-6-PROGRESS.md`
   - `docs/Phase-6IG-Completion.md`
   - this handoff
3. Inspect Java `CM_CASTSPELL.java`, `CM_SUMMON_ATTACK.java`, `CM_SUMMON_CASTSPELL.java`, and `AionClientPacketFactory.java`.
4. Inspect C# `GameClientPacketFactory`, `CmAttack`, `CmCastSpell`, and `GamePacketTests`.
5. Implement one narrow parser or caller unit with Java breadcrumbs.
6. Add focused tests that state what is source-derived and what remains unverified.
7. Run focused tests, then full GameServer tests.
8. Update `docs/PHASE-6-PROGRESS.md` with the required Migration Parity Table and metrics.
9. Create the next handoff document and commit the unit.
