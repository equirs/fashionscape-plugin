# Fashionscape
Ever spend countless hours on a grind for an untradeable item, only to realize that it doesn't look good with literally 
any other item in the game? This plugin was made for you. It allows the player to preview combinations of equipment to 
plan out the perfect look quickly and efficiently.

## How to use
To swap individual items, first **equip any item** in the slot you're interested in. The right-click option **"Swap"** 
should be added. After choosing an item to preview, it can be reverted with a right-click **"Revert"** option on the 
same slot.

The search box will filter out items that can't be equipped in the slot you've chosen. You can mouse over or use the
arrow keys to highlight an item, which will temporarily show what it will look like on you.

If you're feeling lucky, try using the **Shuffle** hotkey in the plugin config. For every checkbox that you've checked 
in the "Shuffle slots" menu, a random piece of equipment will be assigned to that slot. Other hotkeys such as undo and 
redo work how you might expect.

Outfits can be imported or exported using the "Saved outfit profile" text field. Each line follows the format 
`slot:###`. The slot name is lenient and accepts several variations of the slot name. The numeric portion is the 
in-game item ID, which can be found within the search feature of this plugin, or by using an external tool like 
[OSRSBox](https://www.osrsbox.com/tools/item-search/). Anything entered after the item ID will not be used and is for 
human readability. The export shortcut will write all of swapped items to the profile, while the import shortcut will 
update your character model from the profile.

## FAQ
Q: Isn't this hacking? Am I gonna get banned?

A: Your appearance only changes client-side; other players can't see your swapped items. Using this plugin is not 
functionally affecting the game for you, it's no different from using resource packs to reskin the game's base UI.

Q: Why is it changing my character's arms / (facial) hair?

A: The RuneLite client has no way of telling what your character's base models look like unless you reveal them, so 
it's using some default models. The easiest fix is to un-equip and re-equip your head- and torso-slot items.

Q: The export button cleared what I had entered there, is there any way to get it back?

A: If you were wearing the outfit previously, you could try using the undo shortcut and exporting. Other than that,
not really. You will want to save multiple outfits to an external file for the time being. Multiple saved profiles
might be possible in the future.

Q: Such-and-such item isn't showing up or is not animating correctly, why?

A: Most likely, it's a hidden or broken item. Eventually, these will all be documented, with the ability to
filter them out in search results / shuffles. You will probably also notice many unobtainable items that don't look 
right. These aren't a high priority to fix as the focus of the plugin is to create outfits that you can actually obtain.
