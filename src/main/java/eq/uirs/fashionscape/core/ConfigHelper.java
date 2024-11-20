package eq.uirs.fashionscape.core;

import eq.uirs.fashionscape.FashionscapeConfig;
import eq.uirs.fashionscape.core.event.ColorChangedListener;
import eq.uirs.fashionscape.core.event.ColorLockChangedListener;
import eq.uirs.fashionscape.core.event.IconChangedListener;
import eq.uirs.fashionscape.core.event.IconLockChangedListener;
import eq.uirs.fashionscape.core.event.ItemChangedListener;
import eq.uirs.fashionscape.core.event.KitChangedListener;
import eq.uirs.fashionscape.core.event.LockChangedListener;
import eq.uirs.fashionscape.core.layer.Layers;
import eq.uirs.fashionscape.core.layer.Locks;
import eq.uirs.fashionscape.core.layer.ModelType;
import eq.uirs.fashionscape.core.model.ModelInfo;
import eq.uirs.fashionscape.data.color.ColorType;
import eq.uirs.fashionscape.data.kit.JawIcon;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.kit.KitType;
import net.runelite.client.config.ConfigManager;
import org.apache.commons.lang3.SerializationUtils;
import org.jetbrains.annotations.NotNull;

/**
 * Wraps persistence layer for hidden configs
 */
@Singleton
@Slf4j
public class ConfigHelper
{
	private static final int DEBOUNCE_DELAY_MS = 500;

	@Inject
	private Client client;

	@Inject
	private FashionscapeConfig config;

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

	void subscribeToEvents()
	{
		Events.addListener(new ColorChangedListener((e) -> {
			if (e.getModelType() == ModelType.VIRTUAL)
			{
				saveColorConfigDebounced();
			}
		}));
		Events.addListener(new IconChangedListener((e) -> {
			if (e.getModelType() == ModelType.VIRTUAL)
			{
				saveIconDebounced();
			}
		}));
		Events.addListener(new ItemChangedListener((e) -> {
			if (e.getModelType() == ModelType.VIRTUAL)
			{
				saveEquipmentConfigDebounced();
			}
		}));
		Events.addListener(new KitChangedListener((e) -> {
			if (e.getModelType() == ModelType.VIRTUAL)
			{
				saveEquipmentConfigDebounced();
			}
			// need to check game state since real models are cleared on logout
			else if (e.getModelType() == ModelType.REAL && client.getGameState() == GameState.LOGGED_IN)
			{
				saveRealKitsDebounced();
			}
		}));
		Events.addListener(new ColorLockChangedListener((e) -> saveLocksDebounced()));
		Events.addListener(new IconLockChangedListener((e) -> saveLocksDebounced()));
		Events.addListener(new LockChangedListener((e) -> saveLocksDebounced()));
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
		HashMap<KitType, SlotInfo> equipInfo = safeDeserialize(config.equipmentInfo(), new HashMap<>());
		HashMap<ColorType, Integer> colorIds = safeDeserialize(config.colors(), new HashMap<>());
		HashMap<KitType, LockStatus> lockStatuses = safeDeserialize(config.locks(), new HashMap<>());
		HashMap<ColorType, Boolean> colorLocks = safeDeserialize(config.colorLocks(), new HashMap<>());

		Integer iconId = config.icon();
		JawIcon icon = iconId != null ? JawIcon.fromId(config.icon()) : null;

		layers.restore(equipInfo, colorIds, icon);
		locks.restore(lockStatuses, colorLocks, config.iconLocked());
	}

	private <T> T safeDeserialize(byte[] bytes, T fallback)
	{
		try
		{
			return SerializationUtils.deserialize(bytes);
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
		HashMap<KitType, Integer> info = safeDeserialize(realKits, new HashMap<>());
		layers.restoreFromRSProfile(info);
	}

	// migrates from less detailed equipment ids to full SlotInfo. must be called after external data is fetched
	void migrateEquipmentInfo()
	{
		HashMap<KitType, Integer> legacyEquipment = safeDeserialize(config.legacyEquipment(), new HashMap<>());
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
		config.setEquipmentInfo(SerializationUtils.serialize(newEquipment));
		config.setLegacyEquipment(SerializationUtils.serialize(new HashMap<KitType, Integer>()));
	}

	private void saveEquipmentConfigDebounced()
	{
		equipSaveFuture = withDebounce(equipSaveFuture, () -> {
			ModelInfo models = layers.getVirtualModels();
			HashMap<KitType, SlotInfo> result = new HashMap<>(models.getItems().getAll());
			Map<KitType, SlotInfo> kits = models.getKits().getAll().entrySet().stream()
				.collect(Collectors.toMap(Map.Entry::getKey, (e) -> SlotInfo.kit(e.getValue(), e.getKey())));
			result.putAll(kits);
			config.setEquipmentInfo(SerializationUtils.serialize(result));
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
			HashMap<ColorType, Integer> colors = new HashMap<>(layers.getVirtualModels().getColors().getAll());
			config.setColors(SerializationUtils.serialize(colors));
		});
	}

	private void saveRealKitsDebounced()
	{
		kitsSaveFuture = withDebounce(kitsSaveFuture, () -> {
			HashMap<KitType, Integer> result = new HashMap<>(layers.getRealModels().getKits().getAll());
			byte[] bytes = SerializationUtils.serialize(result);
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
			byte[] locksBytes = SerializationUtils.serialize(new HashMap<>(lockedSlots));
			config.setLocks(locksBytes);
			Map<ColorType, Boolean> colorLocks = Arrays.stream(ColorType.values())
				.collect(Collectors.toMap(type -> type, locks::getColor));
			byte[] colorBytes = SerializationUtils.serialize(new HashMap<>(colorLocks));
			config.setColorLocks(colorBytes);
			config.setIconLocked(locks.isIcon());
		});
	}

	private Future<?> withDebounce(Future<?> running, @NotNull Runnable block)
	{
		if (running != null)
		{
			running.cancel(false);
		}
		return executor.schedule(block, DEBOUNCE_DELAY_MS, TimeUnit.MILLISECONDS);
	}
}
