package eq.uirs.fashionscape.core.loadout;

import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import eq.uirs.fashionscape.FashionscapeConfig;
import eq.uirs.fashionscape.core.event.LoadoutsChanged;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.ProfileChanged;

/**
 * Persists saved loadouts in global config. Each loadout is keyed separately.
 */
@Slf4j
@Singleton
public class LoadoutStore
{
	private static final String KEY_PREFIX = "loadout_";
	private static final String KEY_ORDER = "loadoutOrder";
	//@formatter:off
	private static final Type ORDER_TYPE = new TypeToken<List<String>>(){}.getType();
	//@formatter:on

	private final ConfigManager configManager;
	private final EventBus eventBus;
	private final Gson gson;

	private List<SavedLoadout> loadouts = ImmutableList.of();

	@Inject
	LoadoutStore(ConfigManager configManager, EventBus eventBus, Gson gson)
	{
		this.configManager = configManager;
		this.eventBus = eventBus;
		this.gson = gson;
	}

	@Subscribe
	public void onProfileChanged(ProfileChanged e)
	{
		load();
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged e)
	{
		if (e.getGroup().equals(FashionscapeConfig.GROUP) &&
			(e.getKey().startsWith(KEY_PREFIX) || e.getKey().equals(KEY_ORDER)))
		{
			load();
		}
	}

	public synchronized List<SavedLoadout> getAll()
	{
		return Collections.unmodifiableList(loadouts);
	}

	/**
	 * Reads all loadouts from config. Posts {@link LoadoutsChanged} if they differ from in-memory values.
	 */
	public void load()
	{
		List<SavedLoadout> fromConfig = readConfig();
		synchronized (this)
		{
			if (fromConfig.equals(loadouts))
			{
				return;
			}
			loadouts = fromConfig;
		}
		eventBus.post(new LoadoutsChanged());
	}

	public SavedLoadout add(Loadout loadout)
	{
		SavedLoadout saved = new SavedLoadout(UUID.randomUUID().toString(), loadout);
		synchronized (this)
		{
			loadouts = ImmutableList.<SavedLoadout>builder().addAll(loadouts).add(saved).build();
			writeLoadout(saved);
			writeOrder();
		}
		eventBus.post(new LoadoutsChanged());
		return saved;
	}

	/**
	 * Replaces contents of an existing loadout (e.g. to rename it). No-op if id is unknown.
	 */
	public void update(String id, Loadout loadout)
	{
		SavedLoadout saved = new SavedLoadout(id, loadout);
		synchronized (this)
		{
			if (find(id) == null)
			{
				return;
			}
			loadouts = loadouts.stream()
				.map(s -> s.getId().equals(id) ? saved : s)
				.collect(Collectors.toList());
			writeLoadout(saved);
		}
		eventBus.post(new LoadoutsChanged());
	}

	public void remove(String id)
	{
		synchronized (this)
		{
			if (find(id) == null)
			{
				return;
			}
			loadouts = loadouts.stream()
				.filter(s -> !s.getId().equals(id))
				.collect(Collectors.toList());
			configManager.unsetConfiguration(FashionscapeConfig.GROUP, KEY_PREFIX + id);
			writeOrder();
		}
		eventBus.post(new LoadoutsChanged());
	}

	@Nullable
	private SavedLoadout find(String id)
	{
		return loadouts.stream().filter(s -> s.getId().equals(id)).findFirst().orElse(null);
	}

	private void writeLoadout(SavedLoadout saved)
	{
		configManager.setConfiguration(FashionscapeConfig.GROUP, KEY_PREFIX + saved.getId(),
			gson.toJson(saved.getLoadout()));
	}

	private void writeOrder()
	{
		List<String> ids = loadouts.stream().map(SavedLoadout::getId).collect(Collectors.toList());
		configManager.setConfiguration(FashionscapeConfig.GROUP, KEY_ORDER, gson.toJson(ids));
	}

	private List<SavedLoadout> readConfig()
	{
		String wholePrefix = ConfigManager.getWholeKey(FashionscapeConfig.GROUP, null, KEY_PREFIX);
		Set<String> ids = configManager.getConfigurationKeys(wholePrefix).stream()
			.map(key -> key.substring(wholePrefix.length()))
			.collect(Collectors.toCollection(LinkedHashSet::new));

		// order key is written separately, so it might not match the actual `ids`
		List<String> orderedIds = new ArrayList<>(readOrder());
		orderedIds.retainAll(ids);
		ids.stream().filter(id -> !orderedIds.contains(id)).sorted().forEach(orderedIds::add);

		List<SavedLoadout> result = new ArrayList<>();
		for (String id : orderedIds)
		{
			Loadout loadout = readLoadout(id);
			if (loadout != null)
			{
				result.add(new SavedLoadout(id, loadout));
			}
		}
		return ImmutableList.copyOf(result);
	}

	private List<String> readOrder()
	{
		String json = configManager.getConfiguration(FashionscapeConfig.GROUP, KEY_ORDER);
		try
		{
			List<String> order = gson.fromJson(json, ORDER_TYPE);
			return order != null ? order : ImmutableList.of();
		}
		catch (JsonParseException e)
		{
			log.warn("Could not read loadout order", e);
			return ImmutableList.of();
		}
	}

	@Nullable
	private Loadout readLoadout(String id)
	{
		String json = configManager.getConfiguration(FashionscapeConfig.GROUP, KEY_PREFIX + id);
		try
		{
			return gson.fromJson(json, Loadout.class);
		}
		catch (JsonParseException e)
		{
			// unreadable loadouts are skipped but left in config
			log.warn("Could not read loadout {}", id, e);
			return null;
		}
	}
}
