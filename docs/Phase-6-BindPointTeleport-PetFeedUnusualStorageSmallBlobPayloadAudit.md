# Phase 6 - Pet Feed Unusual Storage Small Blob Payload Audit

Date: May 27, 2026
Unit of Work: UOW-1359

## Scope

This unit performs a read-only payload-field audit for the smaller item-blob entries that future unusual-storage warehouse-add artifacts may need.

Java source audited:

- `com.aionemu.gameserver.network.aion.iteminfo.ConditioningInfoBlobEntry`
- `com.aionemu.gameserver.network.aion.iteminfo.PremiumOptionInfoBlobEntry`
- `com.aionemu.gameserver.network.aion.iteminfo.PolishInfoBlobEntry`
- `com.aionemu.gameserver.network.aion.iteminfo.WrapInfoBlobEntry`

C# source audited:

- `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryInfo`

No Java or C# source behavior was changed.

## Java Payload Fields

| Java Entry | Entry Id | Size | Java Payload | C# Status |
|---|---:|---:|---|---|
| `ConditioningInfoBlobEntry` | `0x0F` | 4 | `ownerItem.getChargePoints()` as `D` | Payload shape matches `item.Charge`, but C# entry inclusion uses a template/charge heuristic while Java inclusion requires runtime `item.getConditioningInfo() != null`. |
| `PremiumOptionInfoBlobEntry` | `0x10` | 3 | random bonus stats id or `-1` when unidentified; tune count or `0` when unidentified; trailing `0` | C# shape appears structurally aligned through `RandomBonus`, `TuneCount`, and trailing zero, but runtime source mapping still needs artifact verification. |
| `PolishInfoBlobEntry` | `0x11` | 4 | idian polish charge or `0` when no idian stone | C# shape appears structurally aligned through `item.IdianStone?.PolishCharge ?? 0`, but idian runtime source mapping still needs artifact verification. |
| `WrapInfoBlobEntry` | `0x12` | 1 | `ownerItem.getPackCount()` as `C` | C# shape appears structurally aligned through `item.PackCount`, with inclusion only when pack count is non-zero. |

## Artifact Field Requirements

Future Java artifacts should explicitly include:

- Conditioning: runtime conditioning-info presence and charge points.
- Premium option: identified flag, bonus stats id, tune count, and trailing constant.
- Polish: idian stone presence and polish charge.
- Wrap: pack count and non-zero inclusion reason.

The conditioning entry needs both payload data and inclusion data because payload parity alone is insufficient; Java may omit the entry even when C# heuristics would include it.

## Boundaries Preserved

- No source behavior was changed.
- No Java artifact writer was added.
- No observer install, file output, raw byte retention, or JSON serialization was added.
- No C# serializer behavior changed.
- No parity was promoted to verified.

## Validation

- Ran `git diff --check`.
- Java compile validation was not required for this docs-only audit and remains blocked locally by missing Maven tooling.
- No Java runtime artifacts were generated.
- No .NET tests were required because no C# behavior changed.

## Migration Parity Table - UOW-1359

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.iteminfo.ConditioningInfoBlobEntry` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryInfo.WriteConditioningInfoBlob` | Serialization Entry | Partial | Manual Only | Needs Verification | Payload shape is a single charge-points `D`, but Java entry inclusion depends on runtime conditioning info presence while C# uses template/charge heuristics. |
| `com.aionemu.gameserver.network.aion.iteminfo.PremiumOptionInfoBlobEntry` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryInfo.WritePremiumOptionBlob` | Serialization Entry | Partial | Manual Only | Needs Verification | Shape appears aligned for random bonus/tune count/trailing zero, but runtime source mapping needs Java artifacts before verification. |
| `com.aionemu.gameserver.network.aion.iteminfo.PolishInfoBlobEntry` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryInfo.WritePolishInfoBlob` | Serialization Entry | Partial | Manual Only | Needs Verification | Shape appears aligned for idian polish charge, but idian runtime source mapping needs Java artifacts before verification. |
| `com.aionemu.gameserver.network.aion.iteminfo.WrapInfoBlobEntry` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryInfo.WriteWrapInfoBlob` | Serialization Entry | Partial | Manual Only | Needs Verification | Shape appears aligned for pack count and non-zero inclusion, but runtime artifacts are still missing. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| None | Read-only Audit | Java item-info blob source review | Documents small payload fields and C# inclusion gaps. | Source audit only. | No runtime Java artifact, decoded payload writer, C# serializer verification, or byte comparison validation. |

## Remaining Risks

- Java compile/runtime validation remains blocked locally by missing Maven/Java 25 tooling.
- Runtime conditioning presence is still not represented in C# and cannot be inferred safely from payload fields alone.
- Premium random bonus and idian polish fields need Java artifact samples before source mapping can be verified.
- Warehouse-add byte comparison remains blocked by key blob gaps plus missing raw/canonical Java artifacts.

## Summary Metrics

- Total Java artifacts discovered: 4 grouped artifact rows in this unit
- Total artifacts ported: 0 source artifacts; 1 audit document
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 4 grouped rows
- Total blocked artifacts: Java runtime artifact generation, decoded payload writer, runtime conditioning presence, C# serializer verification, warehouse-add byte comparison
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Add a disabled Java artifact payload DTO shape for the audited key and small blob entries inside the no-output item-blob snapshot. Keep it schema-only and no-output: no JSON writer, no raw byte retention, no observer install, and no C# byte comparison yet.
