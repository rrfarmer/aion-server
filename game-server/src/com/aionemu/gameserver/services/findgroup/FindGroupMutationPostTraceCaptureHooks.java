package com.aionemu.gameserver.services.findgroup;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.aionemu.gameserver.model.gameobjects.findGroup.GroupApplication;
import com.aionemu.gameserver.model.gameobjects.findGroup.GroupRecruitment;
import com.aionemu.gameserver.model.gameobjects.player.Player;

final class FindGroupMutationPostTraceCaptureHooks {

	static final String CAPTURE_FLAG = "aion.findGroupMutationPost.capture";
	static final String TRACE_NAME = "cm-find-group-direct-mutation-post-boundary";
	private static final int SCHEMA_VERSION = 1;
	private static final String TRACE_SOURCE = "Java";
	private static final ThreadLocal<CaptureState> CAPTURE_STATE = ThreadLocal.withInitial(CaptureState::new);

	private FindGroupMutationPostTraceCaptureHooks() {
	}

	static boolean captureEnabled() {
		return Boolean.getBoolean(CAPTURE_FLAG);
	}

	static boolean artifactOutputEnabled() {
		return false;
	}

	static void recordRecruitmentStateMutation(Player player, GroupRecruitment recruitment) {
		if (!captureEnabled())
			return;
		if (player == null || recruitment == null)
			return;
		CAPTURE_STATE.get().pendingRecruitment = PendingTrace.recruitment(player, recruitment);
	}

	static void recordRecruitmentPostedMessageSend(Player player) {
		if (!captureEnabled())
			return;
		PendingTrace pending = CAPTURE_STATE.get().pendingRecruitment;
		if (pending != null)
			pending.recordPostedSystemMessage(player);
	}

	static void recordRecruitmentRefreshedListSend(Player player, List<GroupRecruitment> visibleRecruitments) {
		if (!captureEnabled())
			return;
		CaptureState state = CAPTURE_STATE.get();
		PendingTrace pending = state.pendingRecruitment;
		if (pending == null)
			return;
		TraceRow row = pending.completeWithRefreshedList(player, 0, visibleRecruitmentIds(visibleRecruitments));
		if (row != null)
			state.completedRows.add(row);
		state.pendingRecruitment = null;
	}

	static void recordApplicationStateMutation(Player player, GroupApplication application) {
		if (!captureEnabled())
			return;
		if (player == null || application == null)
			return;
		CAPTURE_STATE.get().pendingApplication = PendingTrace.application(player, application);
	}

	static void recordApplicationPostedMessageSend(Player player) {
		if (!captureEnabled())
			return;
		PendingTrace pending = CAPTURE_STATE.get().pendingApplication;
		if (pending != null)
			pending.recordPostedSystemMessage(player);
	}

	static void recordApplicationRefreshedListSend(Player player, List<GroupApplication> visibleApplications) {
		if (!captureEnabled())
			return;
		CaptureState state = CAPTURE_STATE.get();
		PendingTrace pending = state.pendingApplication;
		if (pending == null)
			return;
		TraceRow row = pending.completeWithRefreshedList(player, 4, visibleApplicationIds(visibleApplications));
		if (row != null)
			state.completedRows.add(row);
		state.pendingApplication = null;
	}

	static List<TraceRow> traceRows() {
		return List.copyOf(CAPTURE_STATE.get().completedRows);
	}

	static List<TraceRow> drainTraceRows() {
		CaptureState state = CAPTURE_STATE.get();
		List<TraceRow> rows = List.copyOf(state.completedRows);
		state.clear();
		return rows;
	}

	static void clearInMemoryTraceRows() {
		CAPTURE_STATE.get().clear();
	}

	private static List<Integer> visibleRecruitmentIds(List<GroupRecruitment> visibleRecruitments) {
		if (visibleRecruitments == null)
			return List.of();
		return visibleRecruitments.stream().map(GroupRecruitment::getObjectId).collect(Collectors.toList());
	}

	private static List<Integer> visibleApplicationIds(List<GroupApplication> visibleApplications) {
		if (visibleApplications == null)
			return List.of();
		return visibleApplications.stream().map(application -> application.getPlayer().getObjectId()).collect(Collectors.toList());
	}

	private static String raceName(Player player) {
		return player.getRace() == null ? "" : player.getRace().name();
	}

	private static final class CaptureState {

		private PendingTrace pendingRecruitment;
		private PendingTrace pendingApplication;
		private final List<TraceRow> completedRows = new ArrayList<>();

		private void clear() {
			pendingRecruitment = null;
			pendingApplication = null;
			completedRows.clear();
		}
	}

	private static final class PendingTrace {

		private final int action;
		private final int activePlayerObjectId;
		private final String activePlayerRace;
		private final int serverEpochSeconds;
		private final String mutationKind;
		private final int mutatedEntryObjectId;
		private final int postedSystemMessageId;
		private boolean postedSystemMessageObserved;
		private int postedSystemMessageRecipientObjectId;

		private PendingTrace(
			int action,
			Player player,
			String mutationKind,
			int mutatedEntryObjectId,
			int serverEpochSeconds,
			int postedSystemMessageId) {
			this.action = action;
			this.activePlayerObjectId = player.getObjectId();
			this.activePlayerRace = raceName(player);
			this.serverEpochSeconds = serverEpochSeconds;
			this.mutationKind = mutationKind;
			this.mutatedEntryObjectId = mutatedEntryObjectId;
			this.postedSystemMessageId = postedSystemMessageId;
		}

		private static PendingTrace recruitment(Player player, GroupRecruitment recruitment) {
			return new PendingTrace(2, player, "Recruitment", recruitment.getObjectId(), recruitment.getLastUpdate(), 1400392);
		}

		private static PendingTrace application(Player player, GroupApplication application) {
			return new PendingTrace(6, player, "Application", application.getPlayer().getObjectId(), application.getLastUpdate(), 1400393);
		}

		private void recordPostedSystemMessage(Player player) {
			if (player == null)
				return;
			postedSystemMessageObserved = true;
			postedSystemMessageRecipientObjectId = player.getObjectId();
		}

		private TraceRow completeWithRefreshedList(Player player, int refreshedListAction, List<Integer> visibleEntryObjectIds) {
			if (!postedSystemMessageObserved || player == null)
				return null;
			return new TraceRow(
				SCHEMA_VERSION,
				TRACE_NAME,
				TRACE_SOURCE,
				action,
				true,
				activePlayerObjectId,
				activePlayerRace,
				serverEpochSeconds,
				mutationKind,
				mutatedEntryObjectId,
				true,
				postedSystemMessageRecipientObjectId,
				"SmSystemMessage",
				postedSystemMessageId,
				player.getObjectId(),
				"SmFindGroup",
				refreshedListAction,
				List.copyOf(visibleEntryObjectIds),
				false,
				false,
				0,
				0);
		}
	}

	record TraceRow(
		int schemaVersion,
		String traceName,
		String traceSource,
		int action,
		boolean boundaryAccepted,
		int activePlayerObjectId,
		String activePlayerRace,
		int serverEpochSeconds,
		String mutationKind,
		int mutatedEntryObjectId,
		boolean stateMutationRecordedBeforeDirectPackets,
		int postedSystemMessageRecipientObjectId,
		String postedSystemMessageType,
		int postedSystemMessageId,
		int refreshedListRecipientObjectId,
		String refreshedListPacketType,
		int refreshedListAction,
		List<Integer> visibleEntryObjectIdsAfterMutation,
		boolean executorInvokedFromBoundary,
		boolean registrySendsObservedInOrder,
		int worldBroadcastCount,
		int inviteDispatchCount) {
	}
}
