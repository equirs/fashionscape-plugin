package eq.uirs.fashionscape.colors;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import eq.uirs.fashionscape.data.ItemColorInfo;
import eq.uirs.fashionscape.data.ItemColors;
import eq.uirs.fashionscape.swap.SwapManager;
import java.awt.Color;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.PlayerComposition;
import net.runelite.api.kit.KitType;

@Singleton
@Slf4j
public class ColorScorer
{
	private final Client client;
	private final SwapManager swapManager;
	private final Gson gson;

	private boolean isFemale;
	private final Map<KitType, List<ItemColorInfo>> playerColors = new HashMap<>();

	@Value
	private static class Score
	{
		// color similarity (scaled 0-1)
		double match;
		// area percentage (0-1) of the match, relative to item or player
		double percentage;
	}

	@Inject
	ColorScorer(Client client, SwapManager swapManager)
	{
		this.client = client;
		this.swapManager = swapManager;
		GsonBuilder builder = new GsonBuilder()
			.registerTypeAdapter(ItemColors.class, new ItemColors.Deserializer());
		gson = builder.create();
	}

	// this should be called before scoring
	public void updatePlayerInfo()
	{
		playerColors.clear();
		Player player = client.getLocalPlayer();
		if (player == null)
		{
			return;
		}
		PlayerComposition composition = player.getPlayerComposition();
		isFemale = composition.isFemale();
		for (KitType slot : KitType.values())
		{
			Integer itemId = swapManager.swappedItemIdIn(slot);
			if (itemId != null)
			{
				playerColors.put(slot, colorsFor(itemId));
			}
		}
	}

	/**
	 * scores color similarity between an item and the current player's outfit:
	 * 1 is a perfect match, 0 is a complete mismatch
	 */
	public double score(int itemId, KitType exclude)
	{
		Map<Integer, Double> playerInfo = getPlayerRgbInfo(exclude);
		List<ItemColorInfo> colors = colorsFor(itemId);
		if (colors.isEmpty() || playerColors.isEmpty())
		{
			return 0;
		}
		// compute an aggregate score relative to the item
		List<Score> scoresItem = new ArrayList<>();
		for (ItemColorInfo c : colors)
		{
			playerInfo.entrySet().stream()
				.min(Comparator.comparingDouble(e -> colorDistance(c.rgb, e.getKey())))
				.map(e -> new Score(1.0 - colorDistance(c.rgb, e.getKey()), c.pct))
				.ifPresent(scoresItem::add);
		}
		double itemScore = scoresItem.stream()
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
		// more weighting in relation to the item itself seems to yield better results
		return (3.0 * itemScore + playerScore) / 4.0;
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
		InputStream stream = this.getClass().getResourceAsStream(itemId + "-a.json");
		if (stream != null)
		{
			Reader reader = new BufferedReader(new InputStreamReader(stream));
			return gson.fromJson(reader, ItemColors.class).getItemColorInfo();
		}
		else
		{
			String suffix = isFemale ? "-f.json" : "-m.json";
			InputStream mfStream = this.getClass().getResourceAsStream(itemId + suffix);
			if (mfStream != null)
			{
				Reader reader = new BufferedReader(new InputStreamReader(mfStream));
				return gson.fromJson(reader, ItemColors.class).getItemColorInfo();
			}
		}
		return new ArrayList<>();
	}

	// Maps rgb -> summed percentage in player, excluding the given slot
	private Map<Integer, Double> getPlayerRgbInfo(KitType exclude)
	{
		Map<Integer, Double> unscaled = playerColors.entrySet().stream()
			.filter(e -> e.getKey() != exclude)
			.map(Map.Entry::getValue)
			.flatMap(List::stream)
			.collect(Collectors.toMap(ItemColorInfo::getRgb, ItemColorInfo::getPct, Double::sum));
		Double scale = unscaled.values().stream().mapToDouble(d -> d).sum();
		return unscaled.entrySet().stream()
			.collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue() / scale));
	}
}
