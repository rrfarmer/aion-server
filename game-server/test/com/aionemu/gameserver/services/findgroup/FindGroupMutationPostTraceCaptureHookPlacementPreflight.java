package com.aionemu.gameserver.services.findgroup;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

final class FindGroupMutationPostTraceCaptureHookPlacementPreflight {

	private static final Path FIND_GROUP_SERVICE_SOURCE =
		Path.of("src/com/aionemu/gameserver/services/findgroup/FindGroupService.java");
	private static final Path FIND_GROUP_SERVICE_SOURCE_FROM_REPOSITORY_ROOT =
		Path.of("game-server").resolve(FIND_GROUP_SERVICE_SOURCE);

	private static final List<Placement> PLACEMENTS = List.of(
		new Placement(
			2,
			"Recruitment",
			"recruitments.put(playerOrTeam.getObjectId(), recruitment);",
			"PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_PARTY_MATCH_OFFER_PARTY_POSTED());",
			"showRecruitments(player);",
			"PacketSendUtility.sendPacket(player, new SM_FIND_GROUP(0, recruitments));"),
		new Placement(
			6,
			"Application",
			"applications.put(player.getObjectId(), application);",
			"PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_PARTY_MATCH_SEEK_PARTY_POSTED());",
			"showApplications(player);",
			"PacketSendUtility.sendPacket(player, new SM_FIND_GROUP(4, applications));"));

	private FindGroupMutationPostTraceCaptureHookPlacementPreflight() {
	}

	static List<Placement> placements() {
		return PLACEMENTS;
	}

	static Report inspectDefaultSource() throws IOException {
		return inspectSource(resolveDefaultSource());
	}

	static Report inspectSource(Path sourcePath) throws IOException {
		String source = Files.readString(sourcePath);
		List<Row> rows = PLACEMENTS.stream().map(placement -> inspectPlacement(source, placement)).toList();
		return new Report(rows, rows.stream().allMatch(Row::mutationBeforePostedBeforeRefresh), false);
	}

	private static Row inspectPlacement(String source, Placement placement) {
		int mutationIndex = source.indexOf(placement.mutationStatement());
		int postedIndex = source.indexOf(placement.postedSystemMessageStatement());
		int refreshCallIndex = source.indexOf(placement.refreshCallStatement());
		int refreshedSendIndex = source.indexOf(placement.refreshedListSendStatement());
		return new Row(
			placement.action(),
			placement.mutationKind(),
			mutationIndex,
			postedIndex,
			refreshCallIndex,
			refreshedSendIndex,
			mutationIndex >= 0
				&& postedIndex > mutationIndex
				&& refreshCallIndex > postedIndex
				&& refreshedSendIndex >= 0);
	}

	private static Path resolveDefaultSource() {
		if (Files.exists(FIND_GROUP_SERVICE_SOURCE))
			return FIND_GROUP_SERVICE_SOURCE;
		return FIND_GROUP_SERVICE_SOURCE_FROM_REPOSITORY_ROOT;
	}

	record Placement(
		int action,
		String mutationKind,
		String mutationStatement,
		String postedSystemMessageStatement,
		String refreshCallStatement,
		String refreshedListSendStatement) {
	}

	record Row(
		int action,
		String mutationKind,
		int mutationIndex,
		int postedSystemMessageIndex,
		int refreshCallIndex,
		int refreshedListSendIndex,
		boolean mutationBeforePostedBeforeRefresh) {
	}

	record Report(
		List<Row> rows,
		boolean allPlacementsPreserveJavaMutationPostOrdering,
		boolean productionHooksIntegrated) {
	}
}
