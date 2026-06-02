package com.aionemu.gameserver.services.findgroup;

import java.util.List;

final class FindGroupMutationPostTraceCaptureScenarioBuilder {

	private FindGroupMutationPostTraceCaptureScenarioBuilder() {
	}

	static Scenario recruitmentScenario() {
		return new Scenario(
			2,
			2002,
			"Recruitment fixture message",
			3,
			0,
			0,
			1001,
			"ELYOS",
			123456,
			2002,
			List.of(2002),
			"Recruitment",
			1400392,
			0,
			"cm-find-group-direct-mutation-post-boundary-action-2-java.json",
			"CM_FIND_GROUP.readImpl action 2 reads playerOrTeamId, message, groupType; runImpl calls FindGroupService.addRecruitment.");
	}

	static Scenario applicationScenario() {
		return new Scenario(
			6,
			4004,
			"Application fixture message",
			5,
			7,
			45,
			4004,
			"ASMODIANS",
			456789,
			4004,
			List.of(4004),
			"Application",
			1400393,
			4,
			"cm-find-group-direct-mutation-post-boundary-action-6-java.json",
			"CM_FIND_GROUP.readImpl action 6 reads playerOrTeamId, message, groupType, classId, level; runImpl calls FindGroupService.addApplication.");
	}

	record Scenario(
		int action,
		int playerOrTeamId,
		String message,
		int groupType,
		int classId,
		int level,
		int activePlayerObjectId,
		String activePlayerRace,
		int serverEpochSeconds,
		int mutatedEntryObjectId,
		List<Integer> visibleEntryObjectIdsAfterMutation,
		String mutationKind,
		int postedSystemMessageId,
		int refreshedListAction,
		String artifactFileName,
		String javaSource) {

		FindGroupMutationPostTraceCaptureSerializer.TraceRow traceRow() {
			return traceRowWithVisibleEntryObjectIds(visibleEntryObjectIdsAfterMutation);
		}

		FindGroupMutationPostTraceCaptureSerializer.TraceRow traceRowWithVisibleEntryObjectIds(List<Integer> visibleEntryObjectIds) {
			return FindGroupMutationPostTraceCaptureSerializer.sampleRow(
				action,
				activePlayerObjectId,
				activePlayerRace,
				serverEpochSeconds,
				mutatedEntryObjectId,
				visibleEntryObjectIds);
		}
	}
}
