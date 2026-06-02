package com.aionemu.gameserver.services.findgroup;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

final class FindGroupMutationPostTraceCaptureSerializer {

	static final int SCHEMA_VERSION = 1;
	static final String TRACE_SOURCE = "Java";

	private static final List<String> SCHEMA_FIELDS = List.of(
		"schemaVersion",
		"traceName",
		"traceSource",
		"action",
		"boundaryAccepted",
		"activePlayerObjectId",
		"activePlayerRace",
		"serverEpochSeconds",
		"mutationKind",
		"mutatedEntryObjectId",
		"stateMutationRecordedBeforeDirectPackets",
		"postedSystemMessageRecipientObjectId",
		"postedSystemMessageType",
		"postedSystemMessageId",
		"refreshedListRecipientObjectId",
		"refreshedListPacketType",
		"refreshedListAction",
		"visibleEntryObjectIdsAfterMutation",
		"executorInvokedFromBoundary",
		"registrySendsObservedInOrder",
		"worldBroadcastCount",
		"inviteDispatchCount");

	private FindGroupMutationPostTraceCaptureSerializer() {
	}

	static List<String> schemaFields() {
		return SCHEMA_FIELDS;
	}

	static Optional<String> trySerializeArtifact(List<TraceRow> rows) {
		if (!FindGroupMutationPostTraceCaptureInstrumentation.captureEnabled())
			return Optional.empty();
		return Optional.of(serializeArtifact(rows));
	}

	static String serializeArtifact(List<TraceRow> rows) {
		if (rows.isEmpty())
			throw new IllegalArgumentException("At least one mutation-post trace row is required.");

		StringBuilder builder = new StringBuilder();
		builder.append("{\n");
		appendField(builder, 1, "schemaVersion", SCHEMA_VERSION).append(",\n");
		appendField(builder, 1, "traceName", FindGroupMutationPostTraceCaptureInstrumentation.TRACE_NAME).append(",\n");
		indent(builder, 1).append("\"traces\": [\n");
		for (int i = 0; i < rows.size(); i++) {
			appendRow(builder, rows.get(i), 2);
			if (i + 1 < rows.size())
				builder.append(",");
			builder.append("\n");
		}
		indent(builder, 1).append("]\n");
		builder.append("}");
		return builder.toString();
	}

	static TraceRow sampleRow(
		int action,
		int activePlayerObjectId,
		String activePlayerRace,
		int serverEpochSeconds,
		int mutatedEntryObjectId,
		List<Integer> visibleEntryObjectIdsAfterMutation) {
		ActionMapping mapping = mappingFor(action);
		return new TraceRow(
			SCHEMA_VERSION,
			FindGroupMutationPostTraceCaptureInstrumentation.TRACE_NAME,
			TRACE_SOURCE,
			action,
			true,
			activePlayerObjectId,
			activePlayerRace,
			serverEpochSeconds,
			mapping.mutationKind(),
			mutatedEntryObjectId,
			true,
			activePlayerObjectId,
			"SmSystemMessage",
			mapping.postedSystemMessageId(),
			activePlayerObjectId,
			"SmFindGroup",
			mapping.refreshedListAction(),
			List.copyOf(visibleEntryObjectIdsAfterMutation),
			false,
			false,
			0,
			0);
	}

	private static ActionMapping mappingFor(int action) {
		return switch (action) {
			case 2 -> new ActionMapping("Recruitment", 1400392, 0);
			case 6 -> new ActionMapping("Application", 1400393, 4);
			default -> throw new IllegalArgumentException("Unsupported mutation-post action " + action);
		};
	}

	private static void appendRow(StringBuilder builder, TraceRow row, int depth) {
		validate(row);
		indent(builder, depth).append("{\n");
		appendField(builder, depth + 1, "schemaVersion", row.schemaVersion()).append(",\n");
		appendField(builder, depth + 1, "traceName", row.traceName()).append(",\n");
		appendField(builder, depth + 1, "traceSource", row.traceSource()).append(",\n");
		appendField(builder, depth + 1, "action", row.action()).append(",\n");
		appendField(builder, depth + 1, "boundaryAccepted", row.boundaryAccepted()).append(",\n");
		appendField(builder, depth + 1, "activePlayerObjectId", row.activePlayerObjectId()).append(",\n");
		appendField(builder, depth + 1, "activePlayerRace", row.activePlayerRace()).append(",\n");
		appendField(builder, depth + 1, "serverEpochSeconds", row.serverEpochSeconds()).append(",\n");
		appendField(builder, depth + 1, "mutationKind", row.mutationKind()).append(",\n");
		appendField(builder, depth + 1, "mutatedEntryObjectId", row.mutatedEntryObjectId()).append(",\n");
		appendField(builder, depth + 1, "stateMutationRecordedBeforeDirectPackets", row.stateMutationRecordedBeforeDirectPackets()).append(",\n");
		appendField(builder, depth + 1, "postedSystemMessageRecipientObjectId", row.postedSystemMessageRecipientObjectId()).append(",\n");
		appendField(builder, depth + 1, "postedSystemMessageType", row.postedSystemMessageType()).append(",\n");
		appendField(builder, depth + 1, "postedSystemMessageId", row.postedSystemMessageId()).append(",\n");
		appendField(builder, depth + 1, "refreshedListRecipientObjectId", row.refreshedListRecipientObjectId()).append(",\n");
		appendField(builder, depth + 1, "refreshedListPacketType", row.refreshedListPacketType()).append(",\n");
		appendField(builder, depth + 1, "refreshedListAction", row.refreshedListAction()).append(",\n");
		appendArrayField(builder, depth + 1, "visibleEntryObjectIdsAfterMutation", row.visibleEntryObjectIdsAfterMutation()).append(",\n");
		appendField(builder, depth + 1, "executorInvokedFromBoundary", row.executorInvokedFromBoundary()).append(",\n");
		appendField(builder, depth + 1, "registrySendsObservedInOrder", row.registrySendsObservedInOrder()).append(",\n");
		appendField(builder, depth + 1, "worldBroadcastCount", row.worldBroadcastCount()).append(",\n");
		appendField(builder, depth + 1, "inviteDispatchCount", row.inviteDispatchCount()).append("\n");
		indent(builder, depth).append("}");
	}

	private static void validate(TraceRow row) {
		if (row.schemaVersion() != SCHEMA_VERSION)
			throw new IllegalArgumentException("schemaVersion must be 1.");
		if (!FindGroupMutationPostTraceCaptureInstrumentation.TRACE_NAME.equals(row.traceName()))
			throw new IllegalArgumentException("traceName must match the mutation-post boundary trace name.");
		if (!TRACE_SOURCE.equals(row.traceSource()))
			throw new IllegalArgumentException("traceSource must be Java.");

		ActionMapping mapping = mappingFor(row.action());
		if (!mapping.mutationKind().equals(row.mutationKind())
			|| mapping.postedSystemMessageId() != row.postedSystemMessageId()
			|| mapping.refreshedListAction() != row.refreshedListAction())
			throw new IllegalArgumentException("Trace row action mapping does not match Java mutation-post behavior.");
		if (!"SmSystemMessage".equals(row.postedSystemMessageType()) || !"SmFindGroup".equals(row.refreshedListPacketType()))
			throw new IllegalArgumentException("Trace row must record SmSystemMessage followed by SmFindGroup.");
		if (row.worldBroadcastCount() != 0 || row.inviteDispatchCount() != 0)
			throw new IllegalArgumentException("Mutation-post rows must not record world broadcast or invite side effects.");
	}

	private static StringBuilder appendField(StringBuilder builder, int depth, String name, String value) {
		return indent(builder, depth).append("\"").append(name).append("\": \"").append(escape(value)).append("\"");
	}

	private static StringBuilder appendField(StringBuilder builder, int depth, String name, int value) {
		return indent(builder, depth).append("\"").append(name).append("\": ").append(value);
	}

	private static StringBuilder appendField(StringBuilder builder, int depth, String name, boolean value) {
		return indent(builder, depth).append("\"").append(name).append("\": ").append(value);
	}

	private static StringBuilder appendArrayField(StringBuilder builder, int depth, String name, List<Integer> values) {
		return indent(builder, depth)
			.append("\"").append(name).append("\": [")
			.append(values.stream().map(String::valueOf).collect(Collectors.joining(", ")))
			.append("]");
	}

	private static StringBuilder indent(StringBuilder builder, int depth) {
		return builder.append("  ".repeat(depth));
	}

	private static String escape(String value) {
		return value
			.replace("\\", "\\\\")
			.replace("\"", "\\\"")
			.replace("\n", "\\n")
			.replace("\r", "\\r")
			.replace("\t", "\\t");
	}

	private record ActionMapping(String mutationKind, int postedSystemMessageId, int refreshedListAction) {
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
