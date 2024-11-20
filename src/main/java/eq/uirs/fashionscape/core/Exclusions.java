package eq.uirs.fashionscape.core;

import com.google.common.collect.ImmutableSet;
import eq.uirs.fashionscape.FashionscapeConfig;
import eq.uirs.fashionscape.core.utils.ItemSlotUtil;
import eq.uirs.fashionscape.data.MiscData;
import eq.uirs.fashionscape.data.kit.BootsKit;
import eq.uirs.fashionscape.remote.RemoteData;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.ItemComposition;
import net.runelite.api.kit.KitType;
import net.runelite.client.game.ItemManager;

/**
 * Builds sets of items to filter out (glitched items, duplicates, potentially members-only)
 */
@Singleton
@Slf4j
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class Exclusions
{
	private static final Pattern PAREN_REPLACE = Pattern.compile("\\(.*\\)");
	/**
	 * kit ids of boot-slot equipment that, when detected in-game, will disable the boots, weapon and shield slots
	 */
	public static final Set<Integer> DISABLE_ANIM_BOOTS = ImmutableSet.of(BootsKit.MINECART.getKitId(0),
		BootsKit.MINECART.getKitId(1));

	private final Set<Integer> bad = new HashSet<>();
	private final Set<Integer> dupes = new HashSet<>();
	private final Set<Integer> members = new HashSet<>();

	private final Client client;
	private final ItemManager itemManager;
	private final FashionscapeConfig config;
	private final IdleAnimations idleAnimations;

	@Value
	private static class ItemDupeData
	{
		int modelId;
		short[] colorsToReplace;
		short[] texturesToReplace;
		String strippedName;
	}

	public boolean loadAll()
	{
		// item count will be 0 if cache is not ready yet
		if (client.getItemCount() == 0)
		{
			return false;
		}
		// plugin checks for the presence of this before calling
		MiscData miscData = RemoteData.MISC_DATA;
		bad.addAll(miscData.badItemIds);
		miscData.badItemRanges.forEach(range -> {
			IntStream.range(range.startInclusive, range.endInclusive + 1)
				.forEach(bad::add);
		});
		List<Pattern> badRegexes = miscData.badItemRegexes.stream()
			.map(Pattern::compile)
			.collect(Collectors.toList());
		Set<ItemDupeData> itemUniques = new HashSet<>();
		for (int i = 0; i < client.getItemCount(); i++)
		{
			int canonical = itemManager.canonicalize(i);
			if (i != canonical || bad.contains(canonical))
			{
				continue;
			}
			KitType slot = ItemSlotUtil.getSlot(i, itemManager);
			if (slot != null)
			{
				ItemComposition itemComposition = itemManager.getItemComposition(canonical);
				if (slot == KitType.WEAPON)
				{
					idleAnimations.queue(itemComposition);
				}
				if (itemComposition.isMembers())
				{
					members.add(i);
				}
				String itemName = itemComposition.getMembersName().toLowerCase();
				if (miscData.badItemNames.contains(itemName))
				{
					bad.add(i);
					continue;
				}
				boolean foundBadContains = miscData.badItemContains.stream()
					.anyMatch(itemName::contains);
				if (foundBadContains)
				{
					bad.add(i);
					continue;
				}
				boolean foundBadRegex = badRegexes.stream()
					.anyMatch(regex -> regex.matcher(itemName).find());
				if (foundBadRegex)
				{
					bad.add(i);
					continue;
				}
				ItemDupeData itemDupeData = new ItemDupeData(
					itemComposition.getInventoryModel(),
					itemComposition.getColorToReplaceWith(),
					itemComposition.getTextureToReplaceWith(),
					stripName(itemName)
				);
				if (itemUniques.contains(itemDupeData))
				{
					dupes.add(canonical);
					continue;
				}
				itemUniques.add(itemDupeData);
			}
		}
		return true;
	}

	public Set<Integer> getAll()
	{
		Set<Integer> result = new HashSet<>(bad);
		result.addAll(dupes);
		if (config.excludeMembersItems())
		{
			result.addAll(members);
		}
		return result;
	}

	private String stripName(String name)
	{
		String noParens = PAREN_REPLACE.matcher(name).replaceAll("");
		return noParens.replaceAll("[^A-Za-z]+", "");
	}
}
