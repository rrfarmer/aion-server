# Phase 6 Session 2417 Completion

Status: Phase 6 continues; logout active-effect persistence was audited against Java source and pinned as an unmodeled C# persistence band. No production or test code changed because C# does not yet expose a live `EffectController`/active-effect repository contract to exercise.

## Scope

- UOW: UOW-2417 logout active-effect persistence audit.
- Source of truth: Java `PlayerLeaveWorldService.leaveWorld`, `PlayerEffectsDAO`, `PlayerEffectController`, and `Effect` persistence predicates.
- C# target surface: `PlayerEnterWorldService.LeaveWorldAsync`, `IPlayerEnterWorldRepository`/`PlayerEnterWorldRepository`, player abnormal-state packet DTOs, and existing live-effect-controller gap breadcrumbs.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/player/PlayerLeaveWorldService.java`
  - Calls `player.getEffectController().removeNonStorableEffectsForLogout()`.
  - Calls `PlayerEffectsDAO.storePlayerEffects(player)`.
  - Later calls `player.getEffectController().removeAllEffects(true)`.
- `game-server/src/com/aionemu/gameserver/dao/PlayerEffectsDAO.java`
  - Deletes all existing `player_effects` rows for the player.
  - Re-inserts abnormal effects that satisfy `effect.canSaveOnLogout()` and `effect.getRemainingTimeMillis() > 28000`.
  - Persists `player_id`, `skill_id`, `skill_lvl`, `remaining_time`, `end_time`, and `force_type`.
  - Loads rows during enter-world setup and rehydrates them through `PlayerEffectController.addSavedEffect(...)`.
- `game-server/src/com/aionemu/gameserver/controllers/effect/PlayerEffectController.java`
  - `removeNonStorableEffectsForLogout()` ends effects that cannot save on logout before DAO persistence.
  - `addSavedEffect(...)` rebuilds an `Effect`, starts it, and sends abnormal-state packet data for visible target slots.
- `game-server/src/com/aionemu/gameserver/controllers/effect/EffectController.java`
  - `removeAllEffects(true)` removes all passive and abnormal effects for logout.
  - `getAbnormalEffects()` supplies the DAO input list after non-storable effects are removed.
- `game-server/src/com/aionemu/gameserver/skillengine/model/Effect.java`
  - `canSaveOnLogout()` rejects `noSaveOnLogout`, permanent/toggle/passive duration `0`, and effects with duration `>= 86400000`.
  - `getRemainingTimeMillis()` derives remaining milliseconds from `endTime - System.currentTimeMillis()`.
  - `getEndTime()` and `ForceType.getName()` feed persisted row columns.

## C# Surface Reviewed

- `dotnetConversion/src/Aion.GameServer/Services/PlayerEnterWorldService.cs`
  - `LeaveWorldAsync` currently records find-group cleanup, denies pending question responses, records repurchase cleanup, removes the player from world state, and calls `SavePlayerLogoutAsync`.
  - There is no modeled remove-non-storable-effects, player-effects store, saved-effect reload, or logout remove-all-effects call in this service.
- `dotnetConversion/src/Aion.GameServer/Data/PlayerEnterWorldRepository.cs`
  - `IPlayerEnterWorldRepository` has load/save contracts for player, items, skills, cooldowns, quests, titles, motions, macros, mailbox, houses, life stats, social data, settings, and bind point data.
  - It has no `player_effects` load/save contract and no `SavePlayerLogoutAsync` write for active/storable effects.
- `dotnetConversion/src/Aion.GameServer/Model/GameObjects/Player.cs`
  - Carries represented `PlayerAbnormalState` flags and a few narrow effect-derived booleans, but no live active-effect list with skill id, level, duration, end time, force type, target slot, or storable predicate.
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmAbnormalEffect.cs`
  - Serializes supplied abnormal-effect DTOs, but does not hydrate live `EffectController` state or persist active effects.
- Existing related C# breadcrumbs already describe live `EffectController` hydration as missing for known-list abnormal-effect packet slices and active-stat provider readiness.

## Findings

- Java logout effect persistence is a real persistence band between death/duel handling and cooldown/life-stat persistence.
- Java intentionally filters before persistence: non-storable effects are ended, then only storable abnormal effects with more than 28 seconds remaining are inserted into `player_effects`.
- Java enter-world reload is paired with logout persistence through `PlayerEffectsDAO.loadPlayerEffects(player)` in `PlayerService`.
- C# currently cannot add a meaningful focused regression for this band because there is no active-effect domain object, storable predicate, `player_effects` repository contract, or live effect-controller lifecycle to assert against.
- The current C# abnormal-effect packet DTOs and represented abnormal-state flags are packet/planner inputs only; they are not persisted active effects.

## Implemented

- Added this completion artifact documenting the Java logout effect persistence contract and the C# absence.
- No C# production or test files were changed.

## Migration Parity Table

| Java Artifact | C# Artifact | Status | Validation | Notes |
| --- | --- | --- | --- | --- |
| `PlayerLeaveWorldService.leaveWorld` effect persistence band | `PlayerEnterWorldService.LeaveWorldAsync` | Not Started | Docs-only audit | C# logout has no remove-non-storable-effects, `PlayerEffectsDAO.storePlayerEffects`, or logout `removeAllEffects(true)` equivalent. |
| `PlayerEffectsDAO.storePlayerEffects/loadPlayerEffects` | `IPlayerEnterWorldRepository` / `PlayerEnterWorldRepository` | Not Started | Docs-only audit | No `player_effects` table load/save contract or row model exists in the C# repository surface. |
| `PlayerEffectController.removeNonStorableEffectsForLogout` | No C# equivalent | Not Started | Docs-only audit | Requires live active-effect lifecycle and `Effect.canSaveOnLogout()` metadata. |
| `Effect.canSaveOnLogout/getRemainingTimeMillis/getEndTime/ForceType` | Represented abnormal-state flags and packet DTOs only | Partial support for unrelated packet/state slices | Existing unit tests only for supplied DTO/state behavior | C# has packet serialization and abnormal masks, but not storable active-effect instances or persistence predicates. |

## Validation Decision

- Docs-only audit, so no .NET test run was needed.
- No Java/Maven validation was run because Java sources and fixtures were not modified.
- Hygiene validation: `git diff --check`.

## Known Remaining Gaps

- Live C# `EffectController`/active-effect map hydration.
- Active-effect domain model with skill id, skill level, duration, end time, target slot, force type, and save/remove predicates.
- `player_effects` repository load/save methods and schema-backed tests.
- Enter-world saved-effect rehydration equivalent to `PlayerEffectsDAO.loadPlayerEffects` plus `PlayerEffectController.addSavedEffect`.
- Logout ordering coverage once the effect persistence surface exists.

## Summary Metrics

- Java files reviewed: 5.
- C# files/surfaces reviewed: 4 primary surfaces plus related effect-controller gap breadcrumbs.
- Production code changed: 0 files.
- Test code changed: 0 files.
- Validation commands planned: 1 hygiene command.
