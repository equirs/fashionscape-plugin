package eq.uirs.fashionscape.colors;

import com.google.common.base.Objects;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import eq.uirs.fashionscape.data.Pet;
import eq.uirs.fashionscape.data.ColorType;
import eq.uirs.fashionscape.data.Colorable;
import eq.uirs.fashionscape.data.kit.JawIcon;
import eq.uirs.fashionscape.data.kit.JawKit;
import eq.uirs.fashionscape.swap.SwapManager;
import java.awt.Color;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.PlayerComposition;
import net.runelite.api.kit.KitType;

@Slf4j
@Singleton
public class ColorScorer
{
	@Value
	private static class Score
	{
		// color similarity (scaled 0-1)
		double match;
		// area percentage (0-1) of the match, relative to item or player
		double percentage;
	}

	private final Client client;
	private final SwapManager swapManager;

	private final Map<Integer, GenderItemColors> allColors;
	// keys are ordinal+1 for kits, 0 for pets
	private final Map<Integer, List<ItemColorInfo>> kitColors = new ConcurrentHashMap<>();
	private final Map<ColorType, Colorable> playerColors = new ConcurrentHashMap<>();

	private boolean isFemale;

	@Inject
	ColorScorer(Client client, SwapManager swapManager)
	{
		this.client = client;
		this.swapManager = swapManager;
		GsonBuilder builder = new GsonBuilder()
			.registerTypeAdapter(ItemColors.class, new ItemColors.Deserializer());
		Gson gson = builder.create();
		InputStream stream = this.getClass().getResourceAsStream("colors.json");
		if (stream != null)
		{
			Reader reader = new BufferedReader(new InputStreamReader(stream));
			Type type = new TypeToken<Map<Integer, GenderItemColors>>()
			{
			}.getType();
			allColors = new ConcurrentHashMap<>(gson.fromJson(reader, type));
		}
		else
		{
			allColors = new ConcurrentHashMap<>();
		}
	}

	// this should be called before scoring if relying on current player swaps
	public void updatePlayerInfo()
	{
		kitColors.clear();
		Player player = client.getLocalPlayer();
		if (player == null)
		{
			return;
		}
		PlayerComposition composition = player.getPlayerComposition();
		if (composition == null)
		{
			return;
		}
		playerColors.clear();
		playerColors.putAll(swapManager.swappedColorsMap());
		isFemale = composition.isFemale();
		for (KitType slot : KitType.values())
		{
			Integer itemId = swapManager.swappedItemIdIn(slot);
			addPlayerInfo(slot, itemId);
		}
		Integer petId = swapManager.swappedPetId();
		if (petId != null)
		{
			Pet pet = SwapManager.PET_MAP.get(petId);
			addPetInfo(pet);
		}
		JawIcon icon = swapManager.swappedIcon();
		if (icon != null)
		{
			Integer iconItemId = JawKit.NO_JAW.getIconItemId(icon);
			if (iconItemId != null)
			{
				kitColors.put(KitType.JAW.ordinal() + 1, colorsFor(iconItemId));
			}
		}
	}

	public void setPlayerInfo(Map<KitType, Integer> itemIds, Map<ColorType, Colorable> colors, Integer petId)
	{
		kitColors.clear();
		Player player = client.getLocalPlayer();
		if (player == null)
		{
			return;
		}
		playerColors.clear();
		playerColors.putAll(colors);
		PlayerComposition composition = player.getPlayerComposition();
		isFemale = composition.isFemale();
		for (Map.Entry<KitType, Integer> entry : itemIds.entrySet())
		{
			addPlayerInfo(entry.getKey(), entry.getValue());
		}
		if (petId != null)
		{
			Pet pet = SwapManager.PET_MAP.get(petId);
			addPetInfo(pet);
		}
	}

	public void addPlayerInfo(KitType slot, Integer itemId)
	{
		if (itemId != null)
		{
			kitColors.put(slot.ordinal() + 1, colorsFor(itemId));
		}
	}

	public void addPlayerColor(ColorType type, Colorable colorable)
	{
		if (colorable != null)
		{
			playerColors.put(type, colorable);
		}
	}

	public void addPetInfo(Pet pet)
	{
		if (pet != null)
		{
			kitColors.put(0, colorsFor(pet.getItemId()));
		}
	}

	public double scorePet(int itemId)
	{
		return score(itemId, 0);
	}

	public double scoreItem(int itemId, KitType slot)
	{
		return score(itemId, slot.ordinal() + 1);
	}

	/**
	 * scores color similarity between an item/pet and the current player's outfit:
	 * 1 is a perfect match, 0 is a complete mismatch
	 */
	private double score(int itemId, Integer ordinal)
	{
		List<ItemColorInfo> colors = colorsFor(itemId);
		return score(colors, ordinal, null);
	}

	/**
	 * scores color similarity between a Colorable and the current player's outfit:
	 * 1 is a perfect match, 0 is a complete mismatch
	 */
	public double score(Colorable colorable, ColorType exclude)
	{
		int rgb = colorable.getColor().getRGB();
		List<ItemColorInfo> colors = Collections.singletonList(new ItemColorInfo(rgb, 1.0));
		return score(colors, null, exclude);
	}

	private double score(List<ItemColorInfo> colors, Integer excludeOrdinal, ColorType excludeColor)
	{
		if (colors.isEmpty())
		{
			return 0;
		}
		Map<Integer, Double> playerInfo = getPlayerRgbInfo(excludeOrdinal, excludeColor);
		if (playerInfo.isEmpty())
		{
			return 0;
		}
		// compute an aggregate score relative to the item/color target
		List<Score> scoresTarget = new ArrayList<>();
		for (ItemColorInfo c : colors)
		{
			playerInfo.entrySet().stream()
				.min(Comparator.comparingDouble(e -> colorDistance(c.rgb, e.getKey())))
				.map(e -> new Score(1.0 - colorDistance(c.rgb, e.getKey()), c.pct))
				.ifPresent(scoresTarget::add);
		}
		double targetScore = scoresTarget.stream()
			.mapToDouble(s -> Math.pow(s.match, 2) * s.percentage)
			.sum();
		// compute an aggregate score relative to the player
		List<Score> scoresPlayer = new ArrayList<>();
		for (Map.Entry<Integer, Double> e : playerInfo.entrySet())
		{
			colors.stream()
				.min(Comparator.comparingDouble(c -> colorDistance(c.rgb, e.getKey())))
				.map(c -> new Score(1.0 - colorDistance(c.rgb, e.getKey()), e.getValue()))
				.ifPresent(scoresPlayer::add);
		}
		double playerScore = scoresPlayer.stream()
			.mapToDouble(s -> Math.pow(s.match, 2) * s.percentage)
			.sum();
		// more weighting in relation to the target itself seems to yield better results
		return (3.0 * targetScore + playerScore) / 4.0;
	}

	// Standard Euclidean color distance scaled from 0 (best) to 1 (worst)
	private double colorDistance(int c1, int c2)
	{
		Color color1 = new Color(c1);
		Color color2 = new Color(c2);
		double deltaR = Math.abs(color1.getRed() - color2.getRed()) / 255f;
		double deltaG = Math.abs(color1.getGreen() - color2.getGreen()) / 255f;
		double deltaB = Math.abs(color1.getBlue() - color2.getBlue()) / 255f;
		return Math.sqrt((Math.pow(deltaR, 2) + Math.pow(deltaG, 2) + Math.pow(deltaB, 2)) / 3.0);
	}

	private List<ItemColorInfo> colorsFor(int itemId)
	{
		GenderItemColors genderColors = allColors.get(itemId);
		if (genderColors != null)
		{
			if (genderColors.any != null)
			{
				return genderColors.any.itemColorInfo;
			}
			else if (isFemale)
			{
				return genderColors.female.itemColorInfo;
			}
			else
			{
				return genderColors.male.itemColorInfo;
			}
		}
		return new ArrayList<>();
	}

	// Maps rgb -> summed percentage in player, excluding the given slot ordinal and color type
	private Map<Integer, Double> getPlayerRgbInfo(Integer excludeOrdinal, ColorType excludeColor)
	{
		Map<Integer, Double> unscaled = kitColors.entrySet().stream()
			.filter(e -> !Objects.equal(e.getKey(), excludeOrdinal))
			.map(Map.Entry::getValue)
			.flatMap(List::stream)
			.collect(Collectors.toMap(ItemColorInfo::getRgb, ItemColorInfo::getPct, Double::sum));
		Map<Integer, Double> unscaledColors = playerColors.entrySet().stream()
			.filter(e -> !Objects.equal(e.getKey(), excludeColor))
			.map(Map.Entry::getValue)
			.collect(Collectors.toMap(c -> c.getColor().getRGB(), c -> 1.0, Double::sum));
		unscaledColors.forEach((rgb, score) -> unscaled.merge(rgb, score, Double::sum));
		final Double scale = unscaled.values().stream().mapToDouble(d -> d).sum();
		return unscaled.entrySet().stream()
			.collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue() / scale));
	}
}
