package eq.uirs.fashionscape.core;

import com.google.common.collect.ImmutableList;
import eq.uirs.fashionscape.core.layer.Layers;
import eq.uirs.fashionscape.core.layer.Locks;
import eq.uirs.fashionscape.core.randomizer.Randomizer;
import eq.uirs.fashionscape.data.color.ColorType;
import eq.uirs.fashionscape.data.kit.JawIcon;
import eq.uirs.fashionscape.remote.RemoteCategory;
import eq.uirs.fashionscape.remote.RemoteDataHandler;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.PlayerComposition;
import net.runelite.api.kit.KitType;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;

/**
 * Singleton entry point into fashionscape internals. The "brains" of the plugin.
 * Coordinates with other classes with more specific responsibilities.
 * Most of the UI should be going through this class, especially if undo/redo is desired.
 */
@Singleton
@Slf4j
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class FashionManager
{
	// item-only slots that can safely contain an equipment id of 0
	public static final List<KitType> ALLOWS_NOTHING_ITEMS = ImmutableList.of(KitType.HEAD, KitType.CAPE, KitType.AMULET,
		KitType.WEAPON, KitType.SHIELD);
	// slots which allow an equipment id of 0 when an item obscures it (hands/boots are possible but very rare)
	public static final List<KitType> ALLOWS_NOTHING_KITS = ImmutableList.of(KitType.HAIR, KitType.JAW, KitType.ARMS,
		KitType.HANDS, KitType.BOOTS);
	public static final int ITEM_OFFSET = PlayerComposition.ITEM_OFFSET;
	public static final int KIT_OFFSET = PlayerComposition.KIT_OFFSET;

	private final Client client;
	private final ClientThread clientThread;
	private final ConfigManager configManager;

	@Getter
	private final Exporter exporter;

	@Getter
	private final Layers layers;

	@Getter
	private final Locks locks;

	private final History history;
	private final Randomizer randomizer;
	private final Exclusions exclusions;
	private final ConfigHelper configHelper;
	private final RemoteDataHandler remote;

	private boolean receivedDataAsync = false;
	private String lastKnownRSProfileKey = null;

	public void startUp()
	{
		configHelper.subscribeToEvents();
		if (hasFetchedRequiredData())
		{
			startUpPostFetch();
		}
		else
		{
			remote.addOnReceiveDataListener((c, s) -> {
				// prevents unintended duplicate calls. on subsequent starts, external::hasSucceeded will be true
				if (receivedDataAsync)
				{
					return;
				}
				if (hasFetchedRequiredData())
				{
					receivedDataAsync = true;
					startUpPostFetch();
				}
			});
		}
	}

	private boolean hasFetchedRequiredData()
	{
		return remote.hasSucceeded(RemoteCategory.SLOT) && remote.hasSucceeded(RemoteCategory.MISC);
	}

	private void startUpPostFetch()
	{
		clientThread.invokeLater(() -> {
			if (!exclusions.loadAll())
			{
				return false;
			}
			configHelper.migrateEquipmentInfo();
			doPreRefreshCheck();
			configHelper.loadFromConfig();
			refreshPlayer();
			return true;
		});
	}

	public void shutDown()
	{
		layers.revertToRealModels(client.getLocalPlayer());
	}

	public void onPlayerChanged()
	{
		doPreRefreshCheck();
		configHelper.loadFromConfig();
		refreshPlayer();
	}

	public void loadRSProfile()
	{
		configHelper.loadRSProfileConfig();
	}

	public boolean isSlotLocked(KitType slot)
	{
		return locks.get(slot) != null;
	}

	public boolean isKitLocked(KitType slot)
	{
		return locks.get(slot) == LockStatus.ALL;
	}

	// Note: this isn't a reliable check for whether an item can occupy this slot, due to other slot interactions
	public boolean isItemLocked(KitType slot)
	{
		return isSlotLocked(slot);
	}

	public boolean isColorLocked(ColorType type)
	{
		return locks.getColor(type);
	}

	public boolean isIconLocked()
	{
		return locks.isIcon();
	}

	public void toggleItemLocked(KitType slot)
	{
		locks.toggle(slot, LockStatus.ITEM);
	}

	public void toggleKitLocked(KitType slot)
	{
		locks.toggle(slot, LockStatus.ALL);
	}

	public void toggleColorLocked(ColorType type)
	{
		locks.toggle(type);
	}

	public void toggleIconLocked()
	{
		locks.toggleIcon();
	}

	public void shuffle()
	{
		history.append(randomizer.shuffle());
		refreshPlayer();
	}

	public void importSelf()
	{
		// first clear everything (without messing with layers) so that the imports derive from real models
		clientThread.invokeLater(() -> {
			layers.revertToRealModels(client.getLocalPlayer());
			importPlayer(client.getLocalPlayer());
		});
	}

	public void importPlayer(@Nullable Player player)
	{
		if (player == null)
		{
			return;
		}
		PlayerComposition composition = player.getPlayerComposition();
		if (composition == null)
		{
			return;
		}
		exporter.importPlayer(composition);
		clientThread.invokeLater(this::refreshPlayer);
	}

	// this should only be called from the client thread
	public void refreshPlayer()
	{
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
		int[] equipIds = layers.computeEquipment();
		for (int i = 0; i < equipIds.length; i++)
		{
			composition.getEquipmentIds()[i] = equipIds[i];
		}
		Integer poseAnim = layers.computeIdlePoseAnimation();
		if (poseAnim != null)
		{
			player.setIdlePoseAnimation(poseAnim);
		}
		int[] colors = layers.computeColors();
		for (int i = 0; i < colors.length; i++)
		{
			composition.getColors()[i] = colors[i];
		}
		composition.setHash();
	}

	/**
	 * Undoes the last action performed.
	 * Can only be called from the client thread.
	 */
	public void undo()
	{
		history.undo();
		refreshPlayer();
	}

	public boolean canUndo()
	{
		return history.undoSize() > 0;
	}

	/**
	 * Redoes the last action that was undone.
	 * Can only be called from the client thread.
	 */
	public void redo()
	{
		history.redo();
		refreshPlayer();
	}

	public boolean canRedo()
	{
		return history.redoSize() > 0;
	}

	public void doPreRefreshCheck()
	{
		Player player = client.getLocalPlayer();
		if (player == null)
		{
			return;
		}
		PlayerComposition composition = player.getPlayerComposition();
		layers.deriveNonEquipment(composition, player.getIdlePoseAnimation());
		String profileKey = configManager.getRSProfileKey();
		if (!Objects.equals(lastKnownRSProfileKey, profileKey))
		{
			lastKnownRSProfileKey = profileKey;
			configHelper.loadRSProfileConfig();
		}
		if (remote.hasSucceeded(RemoteCategory.SLOT))
		{
			layers.deriveEquipment(composition);
		}
	}

	@Nullable
	public Integer virtualItemIdFor(KitType slot)
	{
		SlotInfo info = layers.getVirtualModels().getItems().get(slot);
		return info == null ? null : info.getItemId();
	}

	@Nullable
	public Integer virtualKitIdFor(KitType slot)
	{
		return layers.getVirtualModels().getKits().get(slot);
	}

	@Nullable
	public Integer virtualColorIdFor(ColorType type)
	{
		return layers.getVirtualModels().getColors().get(type);
	}

	@Nullable
	public JawIcon virtualIcon()
	{
		return layers.getVirtualModels().getIcon();
	}

	public boolean isNothing(KitType slot)
	{
		SlotInfo info = layers.getVirtualModels().getItems().get(slot);
		return Objects.equals(info, SlotInfo.nothing(slot));
	}

	// this should only be called from the client thread
	public void revert(KitType slot, ColorType type)
	{
		Diff diff = Diff.empty();
		if (slot != null)
		{
			locks.set(slot, null);
			diff = Diff.merge(layers.set(slot, null, false), diff);
		}
		if (type != null)
		{
			locks.set(type, false);
			diff = Diff.merge(layers.setColor(type, null, false), diff);
		}
		history.append(diff);
		refreshPlayer();
	}

	// this should only be called from the client thread
	public void revertSlot(KitType slot)
	{
		locks.set(slot, null);
		Diff diff = layers.set(slot, null, false);
		history.append(diff);
		refreshPlayer();
	}

	// this should only be called from the client thread
	public void revertIcon()
	{
		locks.setIcon(false);
		Diff diff = layers.setIcon(null, false);
		history.append(diff);
		refreshPlayer();
	}

	public void hoverOverItem(KitType slot, int itemId)
	{
		SlotInfo info = itemId < 0 ? SlotInfo.nothing(slot) : SlotInfo.lookUp(itemId + ITEM_OFFSET, slot);
		if (locks.isAllowed(slot, info))
		{
			layers.set(slot, info, true);
			clientThread.invokeLater(this::refreshPlayer);
		}
	}

	public void hoverOverKit(KitType slot, int kitId)
	{
		SlotInfo info = SlotInfo.kit(kitId, slot);
		if (!isKitLocked(slot))
		{
			layers.set(slot, info, true);
			clientThread.invokeLater(this::refreshPlayer);
		}
	}

	public void hoverOverColor(ColorType type, Integer colorId)
	{
		layers.setColor(type, colorId, true);
		clientThread.invokeLater(this::refreshPlayer);
	}

	public void hoverOverIcon(JawIcon icon)
	{
		layers.setIcon(icon, true);
		clientThread.invokeLater(this::refreshPlayer);
	}

	public void hoverSelectItem(KitType slot, Integer itemId)
	{
		SlotInfo info = itemId < 0 ?
			SlotInfo.nothing(slot) : SlotInfo.lookUp(itemId + ITEM_OFFSET, slot);
		if (!locks.isAllowed(slot, info))
		{
			return;
		}
		layers.resetPreview();
		SlotInfo existing = layers.getVirtualModels().getItems().get(slot);
		// if re-selecting, clear the virtual slot
		SlotInfo finalInfo = Objects.equals(info, existing) ? null : info;
		Diff diff = layers.set(slot, finalInfo, false);
		history.append(diff);
		clientThread.invokeLater(this::refreshPlayer);
	}

	public void hoverSelectKit(KitType slot, int kitId)
	{
		SlotInfo info = SlotInfo.kit(kitId, slot);
		if (!locks.isAllowed(slot, info))
		{
			return;
		}
		layers.resetPreview();
		Integer existingId = layers.getVirtualModels().getKits().get(slot);
		// if re-selecting, clear the virtual slot
		SlotInfo finalInfo = Objects.equals(info.getKitId(), existingId) ? null : info;
		Diff diff = layers.set(slot, finalInfo, false);
		history.append(diff);
		clientThread.invokeLater(this::refreshPlayer);
	}

	public void hoverSelectColor(ColorType type, Integer colorId)
	{
		if (locks.getColor(type))
		{
			return;
		}
		layers.resetPreview();
		Integer existingId = layers.getVirtualModels().getColors().get(type);
		// if re-selecting, clear the virtual color
		Integer finalId = Objects.equals(colorId, existingId) ? null : colorId;
		Diff diff = layers.setColor(type, finalId, false);
		history.append(diff);
		clientThread.invokeLater(this::refreshPlayer);
	}

	public void hoverSelectIcon(JawIcon icon)
	{
		if (locks.isIcon())
		{
			return;
		}
		layers.resetPreview();
		JawIcon existingIcon = layers.getVirtualModels().getIcon();
		// if re-selecting, clear the virtual icon
		JawIcon finalIcon = Objects.equals(icon, existingIcon) ? null : icon;
		Diff diff = layers.setIcon(finalIcon, false);
		history.append(diff);
		clientThread.invokeLater(this::refreshPlayer);
	}

	public void hoverAway()
	{
		layers.resetPreview();
		clientThread.invokeLater(this::refreshPlayer);
	}

	public void clear(boolean removeLocks)
	{
		Diff diff = revertVirtualModels(removeLocks);
		history.append(diff);
		refreshPlayer();
	}

	/**
	 * In the event that slot data fails to fetch, users can clear out certain kit models themselves this way.
	 * If the slot is already set to "nothing", reverts to "not set"
	 */
	public void overrideKitWithNothing(KitType slot)
	{
		if (slot == null || !ALLOWS_NOTHING_KITS.contains(slot))
		{
			return;
		}
		SlotInfo oldInfo = layers.getVirtualModels().getItems().get(slot);
		boolean alreadyHasNothing = oldInfo != null && oldInfo.isNothing();
		SlotInfo newInfo = alreadyHasNothing ? null : SlotInfo.nothing(slot);
		Diff diff = layers.set(slot, newInfo, false);
		history.append(diff);
		refreshPlayer();
	}

	/**
	 * Reverts all item/kit slots, colors, and icon.
	 * Unless `removeLocks` is true, locked slots will remain.
	 * This can only be called on the client thread.
	 */
	private Diff revertVirtualModels(boolean removeLocks)
	{
		if (removeLocks)
		{
			locks.clear();
		}
		Diff diff = Diff.empty();
		for (KitType slot : KitType.values())
		{
			if (locks.isAllowed(slot, null))
			{
				diff = Diff.merge(layers.set(slot, null, false), diff);
			}
		}
		for (ColorType type : ColorType.values())
		{
			if (!locks.getColor(type))
			{
				diff = Diff.merge(layers.setColor(type, null, false), diff);
			}
		}
		if (!locks.isIcon())
		{
			diff = Diff.merge(layers.setIcon(null, false), diff);
		}
		return diff;
	}
}
