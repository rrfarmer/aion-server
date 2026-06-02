package com.aionemu.gameserver.services.findgroup;

import java.util.List;

import com.aionemu.gameserver.model.gameobjects.findGroup.GroupApplication;
import com.aionemu.gameserver.model.gameobjects.findGroup.GroupRecruitment;
import com.aionemu.gameserver.model.gameobjects.player.Player;

final class FindGroupMutationPostTraceCaptureHooks {

	static final String CAPTURE_FLAG = "aion.findGroupMutationPost.capture";

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
	}

	static void recordRecruitmentPostedMessageSend(Player player) {
		if (!captureEnabled())
			return;
	}

	static void recordRecruitmentRefreshedListSend(Player player, List<GroupRecruitment> visibleRecruitments) {
		if (!captureEnabled())
			return;
	}

	static void recordApplicationStateMutation(Player player, GroupApplication application) {
		if (!captureEnabled())
			return;
	}

	static void recordApplicationPostedMessageSend(Player player) {
		if (!captureEnabled())
			return;
	}

	static void recordApplicationRefreshedListSend(Player player, List<GroupApplication> visibleApplications) {
		if (!captureEnabled())
			return;
	}
}
