# Phase 6AEA Completion Handoff

Date: May 27, 2026
Latest Unit of Work: UOW-1295
Status: Phase 6 continues; a Java pet packet golden-vector design now exists, but no Java runtime vectors were generated and live pet dispatch remains disabled.

## Session Summary

UOW-1295 documented the first Java runtime/golden-vector capture plan for known-list pet packet parity.

Files changed:

- `docs/Phase-6-BindPointTeleport-KnownListPetGoldenVectorDesign.md`
- `docs/Phase-6-BindPointTeleport-LiveAdapter-Readiness.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6AEA-Completion.md`

## Validation

- No executable tests were added or run because this was a documentation/design unit.
- `git diff --check` passed with only existing CRLF warnings.
- No Java runtime packet capture was executed.
- No live `GameServerConnection` dispatch was enabled.

## Migration Parity Table - UOW-1295

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PET` | `Aion.GameServer.Network.Aion.ServerPackets.SmPet`; `docs/Phase-6-BindPointTeleport-KnownListPetGoldenVectorDesign.md` | Packet / Serializer / Design | Partial | Manual Only | Needs Verification | Java source reviewed for spawn, dismiss, and broader action branches. First golden-vector batch is scoped to known-list spawn/dismiss only; full action coverage remains unsupported. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PET_EMOTE` | `Aion.GameServer.Network.Aion.ServerPackets.SmPetEmote`; design doc | Packet / Serializer / Design | Partial | Manual Only | Needs Verification | Java source reviewed for fly-start default branch and movement branches. C# still lacks `MOVE_STOP`/`MOVETO` serialization. |
| `com.aionemu.gameserver.controllers.PlayerController.see(Pet)` | `PlayerKnownListPetVisibilityOrderPlanService`; `PlayerKnownListPetVisibilityPacketConstructionService`; design doc | Controller Packet Flow | Partial | Manual Only | Needs Verification | Source order is spawn then optional fly-start when `master.isInFlyingState()`. No live callback execution or runtime packet capture exists. |
| `com.aionemu.gameserver.controllers.PlayerController.notSee(Pet)` | `PlayerKnownListPetVisibilityPacketConstructionService`; design doc | Controller Packet Flow | Partial | Manual Only | Needs Verification | Source review confirms dismiss uses object id plus delete-animation byte. Viewer spawned guard and live socket send remain unported. |
| `com.aionemu.gameserver.model.gameobjects.Pet` | `SmPetSpawnSnapshot`; `PlayerKnownListPetSpawnSnapshotProviderInput` | Model / Snapshot Source | Partial | Manual Only | Needs Verification | Java dependencies are master, common data, template, position, move controller, and heading. Live C# pet model is still missing. |
| `com.aionemu.gameserver.model.gameobjects.player.PetCommonData` | Provider input name/decoration; design doc | Common Data / Snapshot Source | Partial | Manual Only | Needs Verification | Known-list spawn needs name and decoration; full pet-list/feed/mood/doping vectors need birthday, expiration, feed, doping, mood, timers, and scheduler/DAO behavior. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_PET_EMOTE` | future movement-emote vector plan | Client Packet / Movement Source | Not Started | Manual Only | Unknown | Java source reviewed to identify `MOVE_STOP` and `MOVETO` side effects. C# movement-emote serializer and parser/runtime path remain unported. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| None in UOW-1295 | Documentation / Design | Java source review | Defines vector fixture requirements and capture order. | Manual source inspection only. | No executable test or Java runtime comparison. |

## Summary Metrics

- Total Java artifacts discovered: 7 grouped artifact rows in this unit
- Total artifacts ported: 0 code artifacts; 1 golden-vector design document
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 6 grouped rows
- Total blocked artifacts: Java runtime packet harness, live pet model/hydration, movement-emote serializer branches, full `SM_PET` action coverage, static pet data fixture, and live known-list dispatch
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Remaining Risks

- No Java runtime packet captures exist yet, so parity remains unverified.
- Existing C# pet packet tests are source-derived, not golden-vector compared.
- Java fixture setup may need static-data loading or controlled `DataManager.PET_DATA` access for `PetTemplate`.
- Movement-emote vectors require careful current-position and move-controller target setup.
- Full `SM_PET` action vectors depend on mutable timers, static pet functions, scheduled tasks, DAO state, and pet feed/doping/mood systems.
- Live socket dispatch and known-list mutation remain disabled.

## Next Work Options

### Recommended Sequential Task

- Task: Add C# `SmPetEmote` `MOVE_STOP` and `MOVETO` serializer branches with source-derived packet tests, or build the Java packet golden-vector harness if Java tooling/static-data fixture setup is ready.
- Scope:
  - keep live dispatch disabled;
  - use Java `SM_PET_EMOTE.writeImpl` and `CM_PET_EMOTE.runImpl` as the source of truth;
  - document movement target and current-position dependencies explicitly.

### Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | `SmPetEmote` movement serializer branches | `SmPetEmote.cs`; focused packet tests; docs | Medium | Small code slice, but shared packet test file may be busy. |
| B | Java golden-vector harness spike | docs/tooling only unless Java harness path is clear | Medium | Should be isolated from C# serializer work unless one owner integrates. |
| C | Full `SkillTargetSlot` enum mapping audit | docs/read-only or isolated enum/test files | Low/Medium | Separate from pet files. |
| D | Full `SM_PET` action audit part 2 | docs/read-only | Low | Keep separate from movement serializer implementation. |

### Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Orchestrator | `SmPetEmote` movement serializer branches and tests | `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmPetEmote.cs`; focused packet test file; shared docs at integration | Java harness files unless explicitly switching scope |
| Explorer | Java golden-vector harness feasibility notes | Java source read-only; notes only | all writes |

### Do Not Parallelize

- `SmPetEmote.cs` with another serializer agent.
- Shared packet tests across multiple writers.
- Shared progress/handoff docs across multiple agents.
- Live socket dispatch or `GameServerConnection` changes with pet movement serializer work.

## Context Needed By Next Session

- Java source of truth:
  - `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_PET_EMOTE.java`
  - `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_PET_EMOTE.java`
  - `game-server/src/com/aionemu/gameserver/model/gameobjects/PetEmote.java`
  - `game-server/src/com/aionemu/gameserver/model/gameobjects/Pet.java`
- C# surfaces:
  - `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmPetEmote.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`
  - `docs/Phase-6-BindPointTeleport-KnownListPetGoldenVectorDesign.md`
- Latest completed commits:
  - `d171547c6 [Phase 6][UOW-1294] Bridge pet provider diagnostics`
  - UOW-1295 should be committed as `[Phase 6][UOW-1295] Design pet golden vectors`
- Next commit after this handoff should be `[Phase 6][UOW-1296] ...`.

Keep live bind-point behavior disabled until Java-equivalent known-list population, runtime fact hydration, source-first fanout execution, live scheduled callback dispatch, concrete packet serializers, socket dispatch ordering, and Java packet/runtime validation have focused parity slices.

