# Changelog

## Update 2026 October part two: search filters

The search tab now supports **filters**, so you can narrow results down by more than just slots.

* Two buttons under the search bar expand or collapse the **slots** and **filters** sections.
* Filters include f2p/members, whether the item has combat stats, tradeability (on the GE), and value (GE price or
  high alch, with a min/max range). Filters are saved between sessions.
* There's a new **"GE price"** sort option, which shows each item's price.
* The "all" slot now shows results for an empty search, as long as there aren't too many of them. Results also show
  how many items match your query + filters.
* "Exclude members items" now only affects the randomizer. Search uses the "membership" filter instead. If you had the
  setting on, the filter starts on F2P.
* The jaw icon slot in the "base" tab is always available now.

## Update 2026 October: loadouts

Saved outfits have finally been migrated to **loadouts**. You can manage these directly in the plugin panel instead of 
working with clunky plaintext files that were only stored locally.

* Loadouts are now saved in your RuneLite config, so they can sync to other devices.
* The first time you run version 2.1.0+, files in your old `outfits` folder are imported as loadouts, named after each
  file. RuneLite will move the old folder to `.runelite/plugin-data/fashionscape`. This is where your old setups will
  be. Assuming they import correctly, feel free to delete the old files. 
* You can copy loadouts to the clipboard or export them to a file to share them. Old .txt files can still be imported.
* "Load current equipment" was moved from the load button's right-click menu to the top of the loadouts view.

## Update 2026 June

Fashionscape v2 reworks almost all the logic behind the plugin. The UI changes are minimal, and things should mostly
behave as they did in v1. Here are some new inclusions:

* You can now "copy" your own fashionscape much like you can for other players, only you can't right-click yourself.
  ~~Instead, **right-click the "load" button** in the side panel to clone your items and other models.~~ There's now
  a button in the "loadouts" panel to load your models into the plugin.
* The search panel now allows you to directly search for items by
  [item ID](https://oldschool.runescape.wiki/w/Item_IDs). Even items that don't normally display in search results may
  be equipped this way (as long as they have models/equipment slots).
* The randomizer now has a **"Cursed"** setting, which inverts the colour scoring to give you the most mismatched
  combinations it can.
* The plugin no longer clears all state once it shuts down. You can safely turn it off and your setup will restore the
  next time you turn Fashionscape back on. You can also operate the plugin while you're logged out.
* Equipment slots now rely directly on data derived from the cache instead of the item stats API / the RuneLite wiki
  scraper: what this means for you is that there should be more options in your searches now. Many of these new results
  are unobtainable items, such as the
  [pre-update](https://oldschool.runescape.wiki/w/Torva_full_helm_(unobtainable_item))
  [Torva](https://oldschool.runescape.wiki/w/Torva_platebody_(unobtainable_item))
  [armor models](https://oldschool.runescape.wiki/w/Torva_platelegs_(unobtainable_item)).
* The "exclude non-standard items" setting has been removed. I came up with this definition myself, and I have no idea
  how to consistently apply it anymore. With new items entering the game all the time, I just can't maintain such a
  list. Apologies if you found this filter useful.
* Several long-standing bugs have been fixed, including but not limited to: the plugin conflating "not set" slots with
  "nothing" slots, several items not appearing in results, and incorrect models from weird items like the gauntlet
  legwear (which hides boots).
* A bunch of data that was previously bundled with the plugin now lives in
  a [separate repo](https://github.com/equirs/fashionscape-data). There's a chance the requests retrieving this info
  fail. In some cases, GitHub might be down, in which case, the plugin will still work, but you may have to manually
  clear out some of your base models (you can do this by right-clicking the base model's icon). Idle animations and
  colour matching will simply not work if these requests fail.
