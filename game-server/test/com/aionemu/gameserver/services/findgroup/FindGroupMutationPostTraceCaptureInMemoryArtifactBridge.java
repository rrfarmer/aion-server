package com.aionemu.gameserver.services.findgroup;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class FindGroupMutationPostTraceCaptureInMemoryArtifactBridge {

	static final String ARTIFACT_ROOT_PROPERTY = "aion.findGroupMutationPost.artifactRoot";

	private FindGroupMutationPostTraceCaptureInMemoryArtifactBridge() {
	}

	static List<Path> tryWriteDrainedRowsFromArtifactRootProperty() throws IOException {
		String artifactRoot = System.getProperty(ARTIFACT_ROOT_PROPERTY);
		if (artifactRoot == null || artifactRoot.isBlank())
			return List.of();
		return writeDrainedRows(Path.of(artifactRoot));
	}

	static List<Path> writeDrainedRows(Path artifactRoot) throws IOException {
		Map<Integer, List<FindGroupMutationPostTraceCaptureSerializer.TraceRow>> rowsByAction = new LinkedHashMap<>();
		for (FindGroupMutationPostTraceCaptureHooks.TraceRow row : FindGroupMutationPostTraceCaptureHooks.drainTraceRows()) {
			rowsByAction.computeIfAbsent(row.action(), ignored -> new ArrayList<>()).add(toSerializerRow(row));
		}

		List<Path> writtenPaths = new ArrayList<>();
		for (Map.Entry<Integer, List<FindGroupMutationPostTraceCaptureSerializer.TraceRow>> entry : rowsByAction.entrySet()) {
			FindGroupMutationPostTraceCaptureArtifactWriter.tryWriteArtifact(artifactRoot, entry.getKey(), entry.getValue())
				.ifPresent(writtenPaths::add);
		}
		return List.copyOf(writtenPaths);
	}

	private static FindGroupMutationPostTraceCaptureSerializer.TraceRow toSerializerRow(FindGroupMutationPostTraceCaptureHooks.TraceRow row) {
		return new FindGroupMutationPostTraceCaptureSerializer.TraceRow(
			row.schemaVersion(),
			row.traceName(),
			row.traceSource(),
			row.action(),
			row.boundaryAccepted(),
			row.activePlayerObjectId(),
			row.activePlayerRace(),
			row.serverEpochSeconds(),
			row.mutationKind(),
			row.mutatedEntryObjectId(),
			row.stateMutationRecordedBeforeDirectPackets(),
			row.postedSystemMessageRecipientObjectId(),
			row.postedSystemMessageType(),
			row.postedSystemMessageId(),
			row.refreshedListRecipientObjectId(),
			row.refreshedListPacketType(),
			row.refreshedListAction(),
			row.visibleEntryObjectIdsAfterMutation(),
			row.executorInvokedFromBoundary(),
			row.registrySendsObservedInOrder(),
			row.worldBroadcastCount(),
			row.inviteDispatchCount());
	}
}
