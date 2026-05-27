# Phase 6 AHO Completion - STAT_BONUSES Mapping Audit

Date: 2026-05-27
Unit of Work: UOW-1387
Status: Read-only `STAT_BONUSES` mapping audit complete. No serializer code changed.

## Completed

- Added `docs/Phase-6-BindPointTeleport-PetFeedUnusualStorageStatBonusesMappingAudit.md`.
- Reviewed Java `ItemInfoBlob.getFullBlob`, `BonusInfoBlobEntry`, `StatEnum`, `StatFunction`, and `StatRateFunction`.
- Reviewed C# `ItemTemplateSummary.StatModifiers` and XML modifier parsing in `StaticData`.
- Documented the future C# rule for `STAT_BONUSES`:
  - serialize only `Bonus == true`;
  - skip conditioned modifiers via `ChargeCondition != 0`;
  - require a known nonzero Java item-stone mask;
  - write raw value multiplied by Java stat sign;
  - write rate flag only for C# `Operation == "rate"`.
- Updated live-adapter readiness and progress/handoff notes.

## Validation

- Ran read-only source inspection.
- Ran `git diff --check`.
- No C# tests were required because this unit is docs-only.

## Migration Parity Table - UOW-1387

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.iteminfo.BonusInfoBlobEntry` | future `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryInfo` `STAT_BONUSES` writer | Serialization Entry | Not Started | No Tests | Needs Verification | Audit maps Java payload fields and proposed C# rule. Implementation still pending. |
| `com.aionemu.gameserver.network.aion.iteminfo.ItemInfoBlob` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryInfo.WriteItemInfoBlob` | Serialization Utility | Partial | Manual Only | Needs Verification | Future `STAT_BONUSES` entries must appear after premium option and before general info. No serializer change in this unit. |
| `com.aionemu.gameserver.model.stats.container.StatEnum` | future C# stat-mask/sign lookup | Enum / Mapping | Not Started | No Tests | Needs Verification | Audit records nonzero item-stone masks and the `ATTACK_SPEED` negative sign behavior. Dictionary/helper not implemented yet. |
| `com.aionemu.gameserver.model.stats.calc.functions.StatFunction` | `Aion.GameServer.Dataholders.ItemStatModifier` | DTO / Modifier Model | Partial | Manual Only | Needs Verification | C# has operation/name/value/bonus/charge condition fields. Broader Java conditions are not fully modeled, so implementation must skip known conditioned modifiers conservatively. |
| `com.aionemu.gameserver.model.stats.calc.functions.StatRateFunction` | `Aion.GameServer.Dataholders.ItemStatModifier.Operation == "rate"` | Modifier Function | Partial | Manual Only | Needs Verification | Audit maps Java `instanceof StatRateFunction` to C# operation string `rate`. Runtime artifact comparison still missing. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| None | Docs-only audit | Java stat/iteminfo source review | Documents mapping rules for future C# `STAT_BONUSES` implementation. | Source inspection only. | No C# implementation, no tests, no generated Java artifacts. |

## Remaining Risks

- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- Future implementation still needs tests for entry order, mask/value/rate flag, and condition skipping.
- C# currently models only charge conditions on item-template modifiers; other Java condition types may need parser preservation before safe serialization.
- Unknown or zero-mask stat names should be skipped until Java evidence proves otherwise.
- Warehouse-add byte comparison remains guarded by runtime artifact absence and additional blob gaps.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 0 source artifacts; 1 read-only mapping audit document
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: `STAT_BONUSES` implementation/tests, Java runtime artifact generation, warehouse-add byte comparison, remaining item-blob gaps
- Estimated overall migration completion: Phase 6 remains about 71% complete

# Next Work Options

## Recommended Sequential Task

- Task: Implement focused C# `STAT_BONUSES` serialization with tests.
- Scope:
  - add a private Java stat mask/sign lookup for the audited nonzero masks;
  - emit `STAT_BONUSES` entries after premium option and before general info;
  - add focused tests for `MAXHP` add, `ATTACK_SPEED` rate sign inversion, non-bonus skip, and conditioned skip;
  - keep warehouse-add Java artifact byte comparison guarded.

## Safe Parallel Candidates

- Java tooling task: generate the first unusual-storage runtime artifact in a Maven/JDK environment.
- Read-only model audit: determine where temporary-exchange and cleanup/seal fields should be represented in C# item snapshots.
- Test helper task: add a reusable blob-entry scanner for packet tests before broadening blob-entry coverage.
