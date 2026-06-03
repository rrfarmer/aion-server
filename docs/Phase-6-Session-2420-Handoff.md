# Phase 6 Session 2420 Handoff

## Current Phase

- Phase 6 Java-to-C# parity migration continues on branch `4.8`.
- Latest completed UOW: UOW-2420 legion logout cleanup audit.

## Last Completed UOW

- UOW-2420 documented Java legion logout cleanup and confirmed no exact C# target surface currently exists.
- This was a docs-only unit; no production or test code changed.

## Commits Made

- Pending commit for this handoff: `[Phase 6][UOW-2420] Audit legion logout cleanup`.

## Files Changed

- `docs/Phase-6-Session-2420-Completion.md`
- `docs/Phase-6-Session-2420-Handoff.md`

## Java Artifacts Reviewed

- `game-server/src/com/aionemu/gameserver/services/player/PlayerLeaveWorldService.java`
- `game-server/src/com/aionemu/gameserver/services/LegionService.java`
- `game-server/src/com/aionemu/gameserver/model/team/legion/LegionWarehouse.java`
- `game-server/src/com/aionemu/gameserver/model/team/legion/Legion.java`
- `game-server/src/com/aionemu/gameserver/dao/LegionDAO.java`
- `game-server/src/com/aionemu/gameserver/dao/LegionMemberDAO.java`
- `game-server/src/com/aionemu/gameserver/services/PeriodicSaveService.java`

## C# Artifacts Reviewed

- `dotnetConversion/src/Aion.GameServer/Services/PlayerEnterWorldService.cs`
- `dotnetConversion/src/Aion.GameServer/Data/PlayerEnterWorldRepository.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmLegionEdit.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ClientPackets/CmLegionWarehouseKinah.cs`
- `dotnetConversion/src/Aion.GameServer/Services/ItemStorageRestrictionPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/ToyPet/PetFeedPacketMetadataBridge.cs`

## What Changed

- Added a completion document with Java source facts, C# gap analysis, parity table, validation decision, and risks.
- Added this handoff document.
- No C# code was changed because the existing C# legion surfaces are packet/restriction metadata, not logout runtime or persistence surfaces.

## Tests Run

- `git diff --check`
- Result: passed.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.player.PlayerLeaveWorldService.leaveWorld` legion calls | `Aion.GameServer.Services.PlayerEnterWorldService.LeaveWorldAsync` | Logout Lifecycle | Partial | No Tests | Partial Parity | C# logout has no legion warehouse persistence or legion member cleanup hook. Group/alliance timestamp slices exist from prior UOWs. Java ordering is known: `LegionWhUpdate` before effect removal and `onLogout` after online/last-online update. |
| `com.aionemu.gameserver.services.LegionService.LegionWhUpdate` | No exact C# equivalent | Service | Not Started | No Tests | Needs Verification | Missing legion warehouse runtime, deleted-item list, warehouse kinah item aggregation, `InventoryDAO.store` legion-owner parameters, and `ItemStoneListDAO.save` equivalent for legion warehouse logout. |
| `com.aionemu.gameserver.services.LegionService.onLogout` | No exact C# equivalent | Service | Not Started | No Tests | Needs Verification | Missing `LegionMember`, legion runtime, warehouse in-use CAS, `SM_LEGION_UPDATE_MEMBER` fanout, legion/member DAO persistence, and bonus removal packet behavior. |
| `com.aionemu.gameserver.model.team.legion.LegionWarehouse.unsetInUse` | No exact C# equivalent | Runtime Model | Not Started | No Tests | Needs Verification | Java compare-and-sets current user from player object id to zero. C# has no modeled legion warehouse current-user state. |
| `com.aionemu.gameserver.model.team.legion.Legion.removeBonus` | No exact C# equivalent | Runtime Model | Not Started | No Tests | Needs Verification | Java clears `hasBonus` when online members are below 10 and sends `SM_ICON_INFO(1, false)` to remaining online members. |
| `com.aionemu.gameserver.dao.LegionDAO.storeLegion` | No exact C# equivalent | Repository | Not Started | No Tests | Needs Verification | C# repository does not expose legion table update for logout. SQL field list and error behavior need modeling before live wiring. |
| `com.aionemu.gameserver.dao.LegionMemberDAO.storeLegionMember` | No exact C# equivalent | Repository | Not Started | No Tests | Needs Verification | C# repository does not expose legion member nickname/rank/self-intro/challenge score update for logout. |
| `com.aionemu.gameserver.services.PeriodicSaveService.LegionWarehouseSaveTask` | No exact C# equivalent | Scheduled Service | Not Started | No Tests | Needs Verification | Java periodic warehouse save shares the warehouse item/deleted item and item-stone persistence pattern; C# has no periodic legion warehouse save. |

## Known Gaps

- Legion logout warehouse persistence is absent in C#.
- Legion logout member/info persistence is absent in C#.
- Legion warehouse in-use state is absent in C#.
- Legion bonus removal and `SM_ICON_INFO` fanout are absent in C#.
- Legion warehouse periodic save is absent in C#.
- Existing legion packet and item restriction slices are not enough to claim logout parity.

## Remaining Risks

- Java `InventoryDAO.store` parameters differ between logout warehouse save and periodic warehouse save; C# needs a carefully modeled contract before live persistence.
- Java swallows/logs legion warehouse persistence exceptions; repository error semantics need explicit parity decisions.
- Java post-online/last-online `LegionService.onLogout` updates member data and broadcasts packets; current C# aggregate logout ordering cannot represent this without a legion runtime.
- Live legion bonus behavior depends on online member counts and packet fanout that C# does not yet model.

## Next Recommended UOW

UOW-2421: Add a non-live legion logout cleanup readiness plan.

Suggested scope:
- Java:
  - `LegionService.LegionWhUpdate(Player)`
  - `LegionService.onLogout(Player)`
  - `LegionWarehouse.unsetInUse(int)`
  - `Legion.removeBonus()`
  - `LegionDAO.storeLegion`
  - `LegionMemberDAO.storeLegionMember`
- C#:
  - Add a small planner in `Aion.GameServer.Services` that records whether each Java logout responsibility has a modeled C# prerequisite.
  - Add focused tests for member/no-member and warehouse/runtime/repository prerequisite combinations.
  - Do not enable live persistence or packet fanout.

## Focused Validation Recipe

- Specific behavior/contract:
  - The planner should conservatively mark legion logout cleanup not ready until warehouse runtime, member runtime, legion repository, legion member repository, item-stone persistence, and bonus packet fanout prerequisites are all available.
- Focused C# command:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerLegionLogoutCleanupReadinessPlanServiceTests" --no-restore`
- Java/Maven:
  - Not expected unless Java source or fixtures change; Java evidence should come from source review in the test/document notes.
- Broad-validation trigger:
  - none, unless the unit introduces shared repository interfaces or live logout wiring.

## Safe Candidate UOWs

- UOW-2421 non-live legion logout cleanup readiness planner.
- Group/alliance disconnected-event observer planning, still without live packet dispatch.
- Targeted legion repository contract audit only, if constrained to documentation or disabled plan records.

## Context Needed By Next Session

- Read `docs/csharp-port.md`, `docs/orchestration-rules.md`, `docs/parity-verification.md`, this handoff, and the latest completion document before work.
- Do not use `docs/PHASE-6-PROGRESS.md` during normal startup.
- Java remains the source of truth.
- Keep validation narrow; this handoff names the exact focused recipe for UOW-2421.
