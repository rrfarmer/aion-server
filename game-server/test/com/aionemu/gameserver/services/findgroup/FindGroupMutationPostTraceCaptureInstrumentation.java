package com.aionemu.gameserver.services.findgroup;

import java.util.ArrayList;
import java.util.List;

final class FindGroupMutationPostTraceCaptureInstrumentation {

	static final String CAPTURE_FLAG = FindGroupMutationPostTraceCaptureHooks.CAPTURE_FLAG;
	static final String TRACE_NAME = "cm-find-group-direct-mutation-post-boundary";

	private static final List<HookPoint> HOOK_POINTS = List.of(
		new HookPoint(1, HookKind.CLIENT_PACKET_PAYLOAD_PARSED, 0, "CM_FIND_GROUP.readImpl",
			"client_packet_payload_parsed", "action, playerOrTeamId, message, groupType, classId, level"),
		new HookPoint(2, HookKind.CLIENT_PACKET_RUN_IMPL_ENTERED, 0, "CM_FIND_GROUP.runImpl",
			"client_packet_run_impl_entered", "action, activePlayerObjectId, activePlayerRace, boundaryAccepted"),
		new HookPoint(3, HookKind.RECRUITMENT_STATE_MUTATION_RECORDED, 2, "FindGroupService.addRecruitment after recruitments.put(...)",
			"recruitment_state_mutation_recorded", "mutationKind=Recruitment, mutatedEntryObjectId, stateMutationRecordedBeforeDirectPackets=true"),
		new HookPoint(4, HookKind.RECRUITMENT_POSTED_MESSAGE_SEND_OBSERVED, 2,
			"FindGroupService.addRecruitment before PacketSendUtility.sendPacket(... STR_PARTY_MATCH_OFFER_PARTY_POSTED)",
			"recruitment_posted_message_send_observed",
			"postedSystemMessageRecipientObjectId, postedSystemMessageType=SmSystemMessage, postedSystemMessageId=1400392"),
		new HookPoint(5, HookKind.RECRUITMENT_REFRESHED_LIST_SEND_OBSERVED, 2,
			"FindGroupService.showRecruitments before PacketSendUtility.sendPacket(... new SM_FIND_GROUP(0, recruitments))",
			"recruitment_refreshed_list_send_observed",
			"refreshedListRecipientObjectId, refreshedListPacketType=SmFindGroup, refreshedListAction=0, visibleEntryObjectIdsAfterMutation"),
		new HookPoint(6, HookKind.APPLICATION_STATE_MUTATION_RECORDED, 6, "FindGroupService.addApplication after applications.put(...)",
			"application_state_mutation_recorded", "mutationKind=Application, mutatedEntryObjectId, stateMutationRecordedBeforeDirectPackets=true"),
		new HookPoint(7, HookKind.APPLICATION_POSTED_MESSAGE_SEND_OBSERVED, 6,
			"FindGroupService.addApplication before PacketSendUtility.sendPacket(... STR_PARTY_MATCH_SEEK_PARTY_POSTED)",
			"application_posted_message_send_observed",
			"postedSystemMessageRecipientObjectId, postedSystemMessageType=SmSystemMessage, postedSystemMessageId=1400393"),
		new HookPoint(8, HookKind.APPLICATION_REFRESHED_LIST_SEND_OBSERVED, 6,
			"FindGroupService.showApplications before PacketSendUtility.sendPacket(... new SM_FIND_GROUP(4, applications))",
			"application_refreshed_list_send_observed",
			"refreshedListRecipientObjectId, refreshedListPacketType=SmFindGroup, refreshedListAction=4, visibleEntryObjectIdsAfterMutation"),
		new HookPoint(9, HookKind.TRACE_ARTIFACT_ROW_SERIALIZED, 0, "future Java trace serializer",
			"trace_artifact_row_serialized", "schemaVersion, traceName, traceSource, action, mutationKind"));

	private FindGroupMutationPostTraceCaptureInstrumentation() {
	}

	static List<HookPoint> hookPoints() {
		return HOOK_POINTS;
	}

	static boolean captureEnabled() {
		return Boolean.getBoolean(CAPTURE_FLAG);
	}

	static boolean supportsActionsTwoAndSix() {
		return HOOK_POINTS.stream().anyMatch(point -> point.action() == 2)
			&& HOOK_POINTS.stream().anyMatch(point -> point.action() == 6);
	}

	static boolean preservesMutationBeforeSendOrdering(int action) {
		List<HookPoint> actionHooks = HOOK_POINTS.stream().filter(point -> point.action() == action).toList();
		if (actionHooks.size() != 3)
			return false;
		return actionHooks.get(0).order() < actionHooks.get(1).order()
			&& actionHooks.get(1).order() < actionHooks.get(2).order();
	}

	static TraceRecorder newRecorder() {
		return new TraceRecorder(captureEnabled());
	}

	enum HookKind {
		CLIENT_PACKET_PAYLOAD_PARSED,
		CLIENT_PACKET_RUN_IMPL_ENTERED,
		RECRUITMENT_STATE_MUTATION_RECORDED,
		RECRUITMENT_POSTED_MESSAGE_SEND_OBSERVED,
		RECRUITMENT_REFRESHED_LIST_SEND_OBSERVED,
		APPLICATION_STATE_MUTATION_RECORDED,
		APPLICATION_POSTED_MESSAGE_SEND_OBSERVED,
		APPLICATION_REFRESHED_LIST_SEND_OBSERVED,
		TRACE_ARTIFACT_ROW_SERIALIZED
	}

	record HookPoint(int order, HookKind kind, int action, String javaSource, String eventName, String requiredFields) {
	}

	static final class TraceRecorder {

		private final boolean enabled;
		private final List<String> events = new ArrayList<>();

		private TraceRecorder(boolean enabled) {
			this.enabled = enabled;
		}

		void record(HookPoint point) {
			if (enabled)
				events.add(point.eventName());
		}

		boolean enabled() {
			return enabled;
		}

		List<String> events() {
			return List.copyOf(events);
		}
	}
}
