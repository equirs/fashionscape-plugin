package eq.uirs.fashionscape.core;

import eq.uirs.fashionscape.data.anim.AnimationData;
import eq.uirs.fashionscape.remote.RemoteData;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ItemComposition;

/**
 * Builds mappings of weapon ids to idle pose animation ids.
 */
@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class IdleAnimations
{
	// standard for unarmed and most equipment
	public static final int DEFAULT = 808;

	private final ScheduledExecutorService executorService;

	private final Map<Integer, Integer> itemIdToAnim = new HashMap<>();

	@Nullable
	public Integer get(Integer itemId)
	{
		return itemId == null ? null : itemIdToAnim.get(itemId);
	}

	/**
	 * Queues a (weapon-slot) item to determine its idle anim id.
	 * Since this process can be quite slow (many regex/string matches), it's done in the background.
	 * This should only be called from the client thread, after the anims json has been fetched.
	 */
	public void queue(ItemComposition composition)
	{
		int itemId = composition.getId();
		String itemName = composition.getMembersName().toLowerCase();
		executorService.execute(() -> findAnimation(itemId, itemName));
	}

	private void findAnimation(int itemId, String itemName)
	{
		RemoteData.ANIM_DATA.stream()
			.filter(info -> isMatch(info, itemId, itemName))
			.findFirst()
			.ifPresent(info -> itemIdToAnim.put(itemId, info.animId));
	}

	private boolean isMatch(AnimationData info, int itemId, String itemName)
	{
		if (info.itemIds != null && info.itemIds.contains(itemId))
		{
			return true;
		}
		if (info.exactMatches != null && info.exactMatches.contains(itemName))
		{
			return true;
		}
		List<String> containsMatches = info.containsMatches;
		if (containsMatches != null && containsMatches.stream().anyMatch(itemName::contains))
		{
			return true;
		}
		List<String> regexMatches = info.regexMatches;
		if (regexMatches != null)
		{
			return regexMatches.stream()
				.map(Pattern::compile)
				.anyMatch(p -> p.matcher(itemName).find());
		}
		return false;
	}
}
