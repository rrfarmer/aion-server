# Phase 6ART Completion - Material Zone Serialization Boundary

Date: 2026-05-28
Unit of Work: UOW-1652
Status: Complete after focused unit tests

## Scope

This unit added a non-live serialization-boundary plan for generated material zones at the Java `ZoneData.saveData` boundary.

The C# code records JAXB/schema/output metadata and validates generated shape fields, but it does not marshal XML, validate the XSD, log JAXB exceptions, or write `generated_zones.xml`.

## Completed Work

- Added `WorldMapRegionMaterialZoneSerializationPlanService`.
- Added serializable material-template, serialization-plan, and serialization-entry DTOs.
- Modeled Java JAXB metadata: `zones` root, `zone` child, `zones.xsd`, generated-zone output path, and formatted output.
- Modeled shape-specific generated material-zone attributes for cylinder, sphere, and semisphere.
- Added invalid-template metadata for missing shape geometry and missing cylinder top/bottom bounds.
- Updated `docs/PHASE-6-PROGRESS.md` with discovery, file ownership, parity table, tests, risks, metrics, and next-unit guidance.

## Java Parity Notes

- Java `ZoneData.saveData` creates a `JAXBContext` for `ZoneData`.
- Java applies `XmlUtil.getSchema("./data/static_data/zones/zones.xsd")`.
- Java sets `Marshaller.JAXB_FORMATTED_OUTPUT` to `true`.
- Java marshals `zoneList` to `./data/static_data/zones/generated_zones.xml`.
- Java shape DTOs expose XML attributes through `Cylinder`, `Sphere`, and `Semisphere`; C# records the generated-material attributes that Java material constructors populate.

## Validation

Ran:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~WorldMapRegionMaterialZoneSerializationPlanServiceTests|FullyQualifiedName~WorldMapRegionMaterialZoneSavePlanServiceTests|FullyQualifiedName~WorldMapRegionMaterialZoneConstructionServiceTests"
```

Result: passed 17 tests.

Full game-server suite was not rerun in this unit.

## Parallel Work Discovery

| Candidate | Files / Area | Risk | Selected | Notes |
| --- | --- | --- | --- | --- |
| ZoneData save serialization boundary | new serialization-plan service/tests | Medium | Yes | Next generated-zone boundary after save filtering/sorting. |
| Charge-all DB rollback integration planning | repository integration tests | Medium | No | Independent safe alternative. |
| Nearby packet golden gap audit | packet tests/docs | Low | No | Useful later. |
| Zone-handler source audit | Java zone handler files | Low | No | Useful before live callbacks. |

## File Ownership Map

| Agent | Scope | Allowed Files | Forbidden Files | Expected Output |
| --- | --- | --- | --- | --- |
| Orchestrator | Serialization-boundary helper, tests, docs, commit | `WorldMapRegionMaterialZoneSerializationPlanService.cs`, `WorldMapRegionMaterialZoneSerializationPlanServiceTests.cs`, progress/handoff docs | Java source writes, live generated-zone writes, unrelated services/tests | Implemented and documented UOW-1652. |
| Sub-agents | None | None | All files | Not spawned because selected work was small and Orchestrator-owned. |

No sub-agent was spawned for UOW-1652 because selected work was small, self-contained, and docs remained Orchestrator-owned.

## Tests Added Or Updated

| Test | Change | Validates | Java Comparison |
| --- | --- | --- | --- |
| `CreatePlan_RecordsJavaJaxbSchemaAndOutputBoundary` | Added | Root/child element names, schema path, output path, formatted-output metadata, and required attributes for cylinder/sphere/semisphere. | Static source review of Java `ZoneData.saveData` and JAXB annotations. |
| `CreatePlan_BlocksTemplatesMissingShapeFields` | Added | Missing generated shape fields are surfaced before a live write boundary. | Static source review of Java material shape constructors. |

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.dataholders.ZoneData.saveData` | `WorldMapRegionMaterialZoneSerializationPlanService.CreatePlan` | Serialization Boundary | Partial | Unit Tested Metadata | Partial Parity | C# models JAXB root/child names, schema path, formatted-output flag, generated-zone output path, and invalid-template blocking metadata. It does not create `JAXBContext`, marshal XML, validate XSD, log exceptions, or write files. |
| `com.aionemu.gameserver.model.templates.zone.ZoneTemplate` | `WorldMapRegionMaterialZoneSerializableTemplate` | Zone Template DTO | Partial | Unit Tested Metadata | Needs Verification | C# carries name, map id, area kind, flags, priority, zone type, and geometry metadata. JAXB annotations, siege/town attributes, `ZoneName.createOrGet`, and XML field ordering remain unverified. |
| `com.aionemu.gameserver.model.templates.zone.Cylinder` | `WorldMapRegionMaterialZoneSerializationEntry.RequiredShapeAttributes` | Shape Serialization DTO | Partial | Unit Tested Metadata | Partial Parity | C# records required generated cylinder attributes `x`, `y`, `r`, `top`, and `bottom`, and blocks missing top/bottom. It does not serialize float formatting or instantiate `Cylinder`. |
| `com.aionemu.gameserver.model.templates.zone.Sphere` | `WorldMapRegionMaterialZoneSerializationEntry.RequiredShapeAttributes` | Shape Serialization DTO | Partial | Unit Tested Metadata | Partial Parity | C# records required generated sphere attributes `x`, `y`, `z`, and `r`. It does not serialize float formatting or model Java's `afterUnmarshal` radius `<= 0` skip at save time. |
| `com.aionemu.gameserver.model.templates.zone.Semisphere` | `WorldMapRegionMaterialZoneSerializationEntry.RequiredShapeAttributes` | Shape Serialization DTO | Partial | Unit Tested Metadata | Partial Parity | C# records semisphere attributes inherited from `Sphere`. It does not instantiate or serialize `Semisphere`. |
| `com.aionemu.gameserver.utils.xml.XmlUtil.getSchema` | `WorldMapRegionMaterialZoneSerializationPlan.SchemaPath` | XML Schema Boundary | Not Started | Unit Tested Metadata | Needs Verification | C# records the Java schema path only. Schema loading, validation, and JAXB exception behavior remain unported. |

## Remaining Risks

- Serialization planning is non-live and does not write or compare XML.
- JAXB `JAXBContext`, `Marshaller`, XSD validation, formatted output exactness, exception logging, and filesystem writes remain unported.
- Java XML field/attribute ordering and float formatting remain unverified.
- `ZoneTemplate` siege/town attributes, `ZoneName.createOrGet`, and XML name cache behavior remain unported.
- Live material template generation still does not feed a real XML writer.
- Live `MaterialZoneHandler` behavior, dynamic zone handlers, and live C# `MapRegion`/`ZoneInstance` storage remain disabled.
- Charge-all DB rollback remains a future gap: fake-repository tests prove runtime no-mutation/no-packet behavior, not MySQL transaction rollback.
- `docs/commit-conventions.md` is still missing.

## Summary Metrics

- Total Java artifacts discovered: 6 grouped rows.
- Total artifacts ported: 1 non-live material-zone serialization-boundary helper plus 2 focused tests.
- Total artifacts with verified parity: 0.
- Total artifacts needing verification: 2 grouped rows explicitly marked Needs Verification; remaining rows are Partial Parity or Not Started metadata with known gaps.
- Total blocked artifacts: live JAXB context/marshalling, XSD validation, generated-zone filesystem writes, exact XML ordering/float formatting, `ZoneTemplate` siege/town/XML-name fields, `ZoneName` cache behavior, live material template generation pipeline, dynamic zone handlers, live C# MapRegion/ZoneInstance storage, charge-all MySQL rollback regression.
- Estimated overall migration completion: about 72%.

## Next Recommended Unit Of Work

Next best unit:

| Candidate | Files / Area | Notes |
| --- | --- | --- |
| MaterialZoneHandler behavior model | source audit/helper/tests | Model material skill matching, observer registration/removal, collision debug messaging, and unsupported actor side effects. |
| Charge-all DB rollback integration regression | gated DB integration tests | Prove actual MySQL rollback/no-DB-mutation on partial charge-all save failure. |
| Nearby packet golden gap audit | packet tests/docs | Keep read-only unless adding isolated packet tests. |

## Next Work Options

## Recommended Sequential Task

- Task: add a source-grounded audit/model for live `MaterialZoneHandler` behavior.
- Why: material-zone creation/save/serialization planning is now modeled as metadata; live handler behavior is the next major unported branch.
- Files: likely new helper/tests plus docs.
- Java source to read: `MaterialZoneHandler`, material skill templates/data, observer registration/removal paths, packet/debug side effects.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
| --- | --- | --- | --- | --- |
| A | Charge-all DB rollback integration test planning | repository integration tests/fixtures | Medium | Independent from region lifecycle files. |
| B | Nearby packet golden gap audit | packet tests/docs read-only unless assigned | Low | Avoid live send work. |
| C | Zone handler source audit | read-only Java zone handler files | Low | Useful before live zone callbacks. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
| --- | --- | --- | --- |
| Orchestrator | MaterialZoneHandler behavior model | new helper/tests, docs | Java writes, live actor mutation |
| Read-only Agent | Zone handler source audit | Java source reads only | all writes |

## Do Not Parallelize

- Java source files: read-only only.
- Live generated-zone writes, live actor mutation, and live nearby dispatch: still high risk and intentionally disabled.
- Shared material helper files: one owner only if implementation begins.

## Continuation Context

Current unit commit message:

```text
[Phase 6][UOW-1652] Model material zone serialization boundary
```

Files changed in this unit:

- `dotnetConversion/src/Aion.GameServer/Services/WorldMapRegionMaterialZoneSerializationPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/WorldMapRegionMaterialZoneSerializationPlanServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6ART-Completion.md`

Required startup reading for the next continuation:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parallelization-strategy.md`
- `docs/parity-verification.md`
- `docs/PHASE-6-PROGRESS.md`
- Latest completion handoff, currently this file

Note: `docs/commit-conventions.md` is still missing.
