package eq.uirs.fashionscape.core.loadout;

import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import eq.uirs.fashionscape.FashionscapeConfig;
import eq.uirs.fashionscape.core.event.LoadoutsChanged;
import eq.uirs.fashionscape.data.kit.JawIcon;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.runelite.api.kit.KitType;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.events.ConfigChanged;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LoadoutStoreTest
{
	private static final String GROUP = FashionscapeConfig.GROUP;

	// global config keys (without group) to values
	private final Map<String, String> config = new HashMap<>();
	private final ConfigManager configManager = mock(ConfigManager.class);
	private final EventBus eventBus = mock(EventBus.class);
	private LoadoutStore store;

	@BeforeEach
	void setUp()
	{
		when(configManager.getConfiguration(eq(GROUP), anyString())).thenAnswer(i -> config.get(i.<String>getArgument(1)));
		when(configManager.getConfigurationKeys(anyString())).thenAnswer(i -> config.keySet().stream()
			.map(key -> ConfigManager.getWholeKey(GROUP, null, key))
			.filter(key -> key.startsWith(i.getArgument(0)))
			.collect(Collectors.toList()));
		// like ConfigManager, writes post ConfigChanged on the same thread
		doAnswer(i -> {
			config.put(i.getArgument(1), i.getArgument(2));
			store.onConfigChanged(changed(i.getArgument(1)));
			return null;
		}).when(configManager).setConfiguration(eq(GROUP), anyString(), anyString());
		doAnswer(i -> {
			config.remove(i.<String>getArgument(1));
			store.onConfigChanged(changed(i.getArgument(1)));
			return null;
		}).when(configManager).unsetConfiguration(eq(GROUP), anyString());
		store = newStore();
	}

	@Test
	void addedLoadoutsLoadInOrder()
	{
		store.add(loadout("first"));
		store.add(loadout("second"));

		LoadoutStore other = newStore();
		other.load();
		assertEquals(store.getAll(), other.getAll());
		assertEquals("second", other.getAll().get(1).getLoadout().getName());
	}

	@Test
	void savesEnumsByName()
	{
		SavedLoadout saved = store.add(loadout("named"));
		String json = config.get("loadout_" + saved.getId());
		assertTrue(json.contains("\"HEAD\""));
		assertTrue(json.contains("\"BA_ATTACKER\""));
	}

	@Test
	void updateKeepsId()
	{
		SavedLoadout saved = store.add(loadout("old"));
		store.update(saved.getId(), loadout("new"));

		LoadoutStore other = newStore();
		other.load();
		List<SavedLoadout> all = other.getAll();
		assertEquals(1, all.size());
		assertEquals(saved.getId(), all.get(0).getId());
		assertEquals("new", all.get(0).getLoadout().getName());
	}

	@Test
	void removeUnsetsKey()
	{
		SavedLoadout kept = store.add(loadout("kept"));
		SavedLoadout removed = store.add(loadout("removed"));
		store.remove(removed.getId());

		assertFalse(config.containsKey("loadout_" + removed.getId()));
		LoadoutStore other = newStore();
		other.load();
		assertEquals(ImmutableList.of(kept), other.getAll());
	}

	@Test
	void loadsLoadoutMissingFromOrder()
	{
		config.put("loadout_abc", new Gson().toJson(loadout("orphan")));
		store.load();
		assertEquals("abc", store.getAll().get(0).getId());
	}

	@Test
	void skipsUnreadableLoadoutButKeepsIt()
	{
		config.put("loadout_bad", "{not json");
		store.add(loadout("good"));
		store.load();
		assertEquals(1, store.getAll().size());
		assertTrue(config.containsKey("loadout_bad"));
	}

	@Test
	void eachChangePostsOneEvent()
	{
		SavedLoadout saved = store.add(loadout("a"));
		store.update(saved.getId(), loadout("b"));
		store.remove(saved.getId());
		verify(eventBus, times(3)).post(any(LoadoutsChanged.class));
	}

	@Test
	void unchangedLoadPostsNothing()
	{
		store.add(loadout("a"));
		clearInvocations(eventBus);
		store.load();
		verify(eventBus, never()).post(any());
	}

	private LoadoutStore newStore()
	{
		return new LoadoutStore(configManager, eventBus, new Gson());
	}

	private static Loadout loadout(String name)
	{
		Loadout loadout = new Loadout();
		loadout.setName(name);
		loadout.getItems().put(KitType.HEAD, 1163);
		loadout.setIcon(JawIcon.BA_ATTACKER);
		return loadout;
	}

	private static ConfigChanged changed(String key)
	{
		ConfigChanged e = new ConfigChanged();
		e.setGroup(GROUP);
		e.setKey(key);
		return e;
	}
}
