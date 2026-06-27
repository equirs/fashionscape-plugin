package eq.uirs.fashionscape.core;

import com.google.gson.reflect.TypeToken;
import eq.uirs.fashionscape.FashionscapeConfig;
import eq.uirs.fashionscape.core.event.ColorChanged;
import eq.uirs.fashionscape.core.event.ColorLockChanged;
import eq.uirs.fashionscape.core.event.IconChanged;
import eq.uirs.fashionscape.core.event.IconLockChanged;
import eq.uirs.fashionscape.core.event.ItemChanged;
import eq.uirs.fashionscape.core.event.KitChanged;
import eq.uirs.fashionscape.core.event.LockChanged;
import eq.uirs.fashionscape.core.layer.Layers;
import eq.uirs.fashionscape.core.layer.Locks;
import eq.uirs.fashionscape.core.layer.ModelType;
import eq.uirs.fashionscape.core.model.ModelInfo;
import eq.uirs.fashionscape.core.utils.ConfigSerializer;
import eq.uirs.fashionscape.data.color.ColorType;
import eq.uirs.fashionscape.data.kit.JawIcon;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.kit.KitType;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;

/**
 * Manages the plugin's hidden configs
 */
@Singleton
@Slf4j
public class ConfigHelper
{
	//@formatter:off
	private static final Type EQUIPMENT_INFO_TYPE = new TypeToken<HashMap<KitType, SlotInfo>>(){}.getType();
	private static final Type COLORS_TYPE = new TypeToken<HashMap<Integer, Integer>>(){}.getType();
	private static final Type LOCKS_TYPE = new TypeToken<HashMap<KitType, LockStatus>>(){}.getType();
	private static final Type COLOR_LOCKS_TYPE = new TypeToken<HashMap<ColorType, Boolean>>(){}.getType();
	private static final Type KIT_IDS_TYPE = new TypeToken<HashMap<KitType, Integer>>(){}.getType();
	//@formatter:on
	private static final int DEBOUNCE_DELAY_MS = 500;

	@Inject
	private Client client;

	@Inject
	private FashionscapeConfig config;

	@Inject
	private ConfigSerializer configSerializer;

	@Inject
	private ConfigManager configManager;

	@Inject
	private ScheduledExecutorService executor;

	@Inject
	private Layers layers;

	@Inject
	private Locks locks;

	// should only load once since config might be behind local state
	private boolean hasLoadedConfig;

	private Future<?> colorSaveFuture;
	private Future<?> kitsSaveFuture;
	private Future<?> equipSaveFuture;
	private Future<?> iconSaveFuture;
	private Future<?> locksSaveFuture;

	@Subscribe
	public void onColorChanged(ColorChanged e)
	{
		if (e.getModelType() == ModelType.VIRTUAL)
		{
			saveColorConfigDebounced();
		}
	}

	@Subscribe
	public void onIconChanged(IconChanged e)
	{
		if (e.getModelType() == ModelType.VIRTUAL)
		{
			saveIconDebounced();
		}
	}

	@Subscribe
	public void onItemChanged(ItemChanged e)
	{
		if (e.getModelType() == ModelType.VIRTUAL)
		{
			saveEquipmentConfigDebounced();
		}
	}

	@Subscribe
	public void onKitChanged(KitChanged e)
	{
		if (e.getModelType() == ModelType.VIRTUAL)
		{
			saveEquipmentConfigDebounced();
		}
		// need to check game state: don't handle clearing real models on logout
		else if (e.getModelType() == ModelType.REAL && client.getGameState() == GameState.LOGGED_IN)
		{
			saveRealKitsDebounced();
		}
	}

	@Subscribe
	public void onColorLockChanged(ColorLockChanged e)
	{
		saveLocksDebounced();
	}

	@Subscribe
	public void onIconLockChanged(IconLockChanged e)
	{
		saveLocksDebounced();
	}

	@Subscribe
	public void onLockChanged(LockChanged e)
	{
		saveLocksDebounced();
	}

	/**
	 * Reads from global config. Call after listeners are set (i.e., after panels created).
	 * This only needs to be called once after client restart; layers and locks will remain even when plugin shuts down
	 */
	void loadFromConfig()
	{
		if (hasLoadedConfig)
		{
			return;
		}
		hasLoadedConfig = true;
		HashMap<KitType, SlotInfo> equipInfo = safeDeserialize(config.equipmentInfo(), EQUIPMENT_INFO_TYPE, new HashMap<>());

		ColorType[] allColorTypes = ColorType.values();
		Map<Integer, Integer> rawColorIds = safeDeserialize(config.colors(), COLORS_TYPE, new HashMap<>());
		Map<ColorType, Integer> colorIds = rawColorIds.entrySet().stream()
			.collect(Collectors.toMap(e -> allColorTypes[e.getKey()], Map.Entry::getValue));

		Map<KitType, LockStatus> lockStatuses = safeDeserialize(config.locks(), LOCKS_TYPE, new HashMap<>());
		Map<ColorType, Boolean> colorLocks = safeDeserialize(config.colorLocks(), COLOR_LOCKS_TYPE, new HashMap<>());

		Integer iconId = config.icon();
		JawIcon icon = iconId != null ? JawIcon.fromId(config.icon()) : null;

		layers.restore(equipInfo, colorIds, icon);
		locks.restore(lockStatuses, colorLocks, config.iconLocked());
	}

	private <T> T safeDeserialize(byte[] bytes, Type type, T fallback)
	{
		try
		{
			T result = configSerializer.deserialize(bytes, type);
			if (result != null)
			{
				return result;
			}
		}
		catch (Exception e)
		{
			log.error("could not deserialize bad byte array", e);
		}
		return fallback;
	}

	/**
	 * Loads default kits from config. Called during pre-refresh check, on first login and on rsn change
	 */
	void loadRSProfileConfig()
	{
		byte[] realKits = configManager.getRSProfileConfiguration(FashionscapeConfig.GROUP,
			FashionscapeConfig.KEY_REAL_KITS, byte[].class);
		HashMap<KitType, Integer> info = safeDeserialize(realKits, KIT_IDS_TYPE, new HashMap<>());
		layers.restoreFromRSProfile(info);
	}

	// migrates from less detailed equipment ids to full SlotInfo. must be called after external data is fetched
	void migrateEquipmentInfo()
	{
		HashMap<KitType, Integer> legacyEquipment = safeDeserialize(config.legacyEquipment(), KIT_IDS_TYPE, new HashMap<>());
		if (legacyEquipment.isEmpty())
		{
			return;
		}
		HashMap<KitType, SlotInfo> newEquipment = new HashMap<>();
		// check items separately (will allow removing redundant 0s)
		List<Map.Entry<KitType, Integer>> itemEquipIds = legacyEquipment.entrySet().stream()
			.filter(e -> e.getValue() >= FashionManager.ITEM_OFFSET)
			.collect(Collectors.toList());
		for (Map.Entry<KitType, Integer> entry : itemEquipIds)
		{
			KitType slot = entry.getKey();
			SlotInfo info = SlotInfo.lookUp(entry.getValue(), slot);
			newEquipment.put(slot, info);
			legacyEquipment.remove(slot);
			info.getHidden().forEach(legacyEquipment::remove);
		}
		// only explicit nothing and kits should remain
		for (Map.Entry<KitType, Integer> entry : legacyEquipment.entrySet())
		{
			KitType slot = entry.getKey();
			SlotInfo info = SlotInfo.lookUp(entry.getValue(), slot);
			newEquipment.put(slot, info);
		}
		config.setEquipmentInfo(configSerializer.serialize(newEquipment));
		config.setLegacyEquipment(configSerializer.serialize(new HashMap<KitType, Integer>()));
	}

	private void saveEquipmentConfigDebounced()
	{
		equipSaveFuture = withDebounce(equipSaveFuture, () -> {
			ModelInfo models = layers.getVirtualModels();
			HashMap<KitType, SlotInfo> result = new HashMap<>(models.getItems().getAll());
			Map<KitType, SlotInfo> kits = models.getKits().getAll().entrySet().stream()
				.collect(Collectors.toMap(Map.Entry::getKey, (e) -> SlotInfo.kit(e.getValue(), e.getKey())));
			result.putAll(kits);
			config.setEquipmentInfo(configSerializer.serialize(result));
		});
	}

	private void saveIconDebounced()
	{
		iconSaveFuture = withDebounce(iconSaveFuture, () -> {
			JawIcon icon = layers.getVirtualModels().getIcon();
			Integer iconId = icon != null ? icon.getId() : null;
			config.setIcon(iconId);
		});
	}

	private void saveColorConfigDebounced()
	{
		colorSaveFuture = withDebounce(colorSaveFuture, () -> {
			Map<Integer, Integer> rawColors = layers.getVirtualModels().getColors().getAll().entrySet().stream()
				.collect(Collectors.toMap(e -> e.getKey().ordinal(), Map.Entry::getValue));
			HashMap<Integer, Integer> colors = new HashMap<>(rawColors);
			config.setColors(configSerializer.serialize(colors));
		});
	}

	private void saveRealKitsDebounced()
	{
		kitsSaveFuture = withDebounce(kitsSaveFuture, () -> {
			HashMap<KitType, Integer> result = new HashMap<>(layers.getRealModels().getKits().getAll());
			byte[] bytes = configSerializer.serialize(result);
			configManager.setRSProfileConfiguration(FashionscapeConfig.GROUP, FashionscapeConfig.KEY_REAL_KITS, bytes);
		});
	}

	private void saveLocksDebounced()
	{
		locksSaveFuture = withDebounce(locksSaveFuture, () -> {
			//noinspection DataFlowIssue (filter ensures non-null)
			Map<KitType, LockStatus> lockedSlots = Arrays.stream(KitType.values())
				.filter(slot -> locks.get(slot) != null)
				.collect(Collectors.toMap(slot -> slot, slot -> locks.get(slot)));
			byte[] locksBytes = configSerializer.serialize(new HashMap<>(lockedSlots));
			config.setLocks(locksBytes);
			Map<ColorType, Boolean> colorLocks = Arrays.stream(ColorType.values())
				.collect(Collectors.toMap(type -> type, locks::getColor));
			byte[] colorBytes = configSerializer.serialize(new HashMap<>(colorLocks));
			config.setColorLocks(colorBytes);
			config.setIconLocked(locks.isIcon());
		});
	}

	private Future<?> withDebounce(Future<?> running, @Nonnull Runnable block)
	{
		if (running != null)
		{
			running.cancel(false);
		}
		return executor.schedule(block, DEBOUNCE_DELAY_MS, TimeUnit.MILLISECONDS);
	}
}
