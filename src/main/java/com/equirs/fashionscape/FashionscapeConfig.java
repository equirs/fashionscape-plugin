package com.equirs.fashionscape;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Keybind;

@ConfigGroup("fashionscape")
public interface FashionscapeConfig extends Config
{

	@ConfigSection(
		name = "Keybinds",
		description = "Various keyboard shortcuts",
		position = 0
	)
	String keybinds = "keybinds";

	@ConfigSection(
		name = "Shuffle slots",
		description = "While checked, the shuffle button will randomly assign new items to the slot",
		position = 1,
		closedByDefault = true
	)
	String shuffleSlots = "shuffleSlots";

	// TODO for v2
//	@ConfigItem(
//		position = 2,
//		keyName = "excludeNonStandardItems",
//		name = "Exclude non-standard items",
//		description = "Filters out items that cannot be normally equipped everywhere"
//	)
//	default boolean excludeNonStandardItems()
//	{
//		return true;
//	}

	@ConfigItem(
		keyName = "idleAnimId",
		name = "Idle ID",
		description = "Debug idle ID",
		position = 2
	)
	default int idleAnimId()
	{
		return 808;
	}

	@ConfigItem(
		keyName = "savedProfile",
		name = "Saved outfit profile",
		description = "Imported/exported outfit (see import/export keys)",
		position = 3
	)
	default String savedProfile()
	{
		return "";
	}

	@ConfigItem(
		keyName = "undoKey",
		name = "Undo key",
		description = "Reverts the most recent equip swap",
		section = keybinds,
		position = 0
	)
	default Keybind undoKey()
	{
		return new Keybind(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK);
	}

	@ConfigItem(
		keyName = "redoKey",
		name = "Redo key",
		description = "Undoes the most recent undo",
		section = keybinds,
		position = 1
	)
	default Keybind redoKey()
	{
		return new Keybind(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK);
	}

	@ConfigItem(
		keyName = "exportKey",
		name = "Export key",
		description = "Exports outfit information to profile",
		section = keybinds,
		position = 2
	)
	default Keybind exportKey()
	{
		return new Keybind(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK);
	}

	@ConfigItem(
		keyName = "importKey",
		name = "Import key",
		description = "Imports outfit information from profile",
		section = keybinds,
		position = 3
	)
	default Keybind importKey()
	{
		return new Keybind(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK);
	}

	@ConfigItem(
		keyName = "revertKey",
		name = "Revert key",
		description = "Clears all swapped equipment slots",
		section = keybinds,
		position = 4
	)
	default Keybind revertKey()
	{
		return new Keybind(KeyEvent.VK_Q, InputEvent.CTRL_DOWN_MASK);
	}

	@ConfigItem(
		keyName = "shuffleKey",
		name = "Shuffle key",
		description = "Randomly reassigns specified equipment slots (see shuffle slots)",
		section = keybinds,
		position = 5
	)
	default Keybind shuffleKey()
	{
		return new Keybind(KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK);
	}

	@ConfigItem(
		keyName = "shuffleHead",
		name = "Head",
		description = "Allow shuffle function to re-assign Head slot item",
		section = shuffleSlots,
		position = 0
	)
	default boolean shuffleHead()
	{
		return true;
	}

	@ConfigItem(
		keyName = "shuffleCape",
		name = "Cape",
		description = "Allow shuffle function to re-assign Cape slot item",
		section = shuffleSlots,
		position = 1
	)
	default boolean shuffleCape()
	{
		return true;
	}

	@ConfigItem(
		keyName = "shuffleAmulet",
		name = "Amulet",
		description = "Allow shuffle function to re-assign Amulet slot item",
		section = shuffleSlots,
		position = 2
	)
	default boolean shuffleAmulet()
	{
		return true;
	}

	@ConfigItem(
		keyName = "shuffleWeapon",
		name = "Weapon",
		description = "Allow shuffle function to re-assign Weapon slot item",
		section = shuffleSlots,
		position = 3
	)
	default boolean shuffleWeapon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "shuffleTorso",
		name = "Torso",
		description = "Allow shuffle function to re-assign Torso slot item",
		section = shuffleSlots,
		position = 4
	)
	default boolean shuffleTorso()
	{
		return true;
	}

	@ConfigItem(
		keyName = "shuffleShield",
		name = "Shield",
		description = "Allow shuffle function to re-assign Shield slot item",
		section = shuffleSlots,
		position = 5
	)
	default boolean shuffleShield()
	{
		return true;
	}

	@ConfigItem(
		keyName = "shuffleLegs",
		name = "Legs",
		description = "Allow shuffle function to re-assign Legs slot item",
		section = shuffleSlots,
		position = 6
	)
	default boolean shuffleLegs()
	{
		return true;
	}

	@ConfigItem(
		keyName = "shuffleHands",
		name = "Hands",
		description = "Allow shuffle function to re-assign Hands slot item",
		section = shuffleSlots,
		position = 7
	)
	default boolean shuffleHands()
	{
		return true;
	}

	@ConfigItem(
		keyName = "shuffleBoots",
		name = "Boots",
		description = "Allow shuffle function to re-assign Boots slot item",
		section = shuffleSlots,
		position = 8
	)
	default boolean shuffleBoots()
	{
		return true;
	}

}
