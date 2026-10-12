package eq.uirs.fashionscape.panel.search;

import eq.uirs.fashionscape.panel.NothingItemComposition;
import java.awt.image.BufferedImage;
import javax.annotation.Nullable;
import lombok.Value;
import lombok.With;
import net.runelite.api.ItemComposition;
import net.runelite.api.kit.KitType;

@Value
class SearchResult
{
	// null for the "Nothing" result
	@Nullable
	ItemComposition itemComposition;
	BufferedImage icon;
	KitType slot;
	// color match score, only set for color sort
	@With
	@Nullable
	Double score;

	int getId()
	{
		return itemComposition != null ? itemComposition.getId() : NothingItemComposition.ID;
	}

	String getName()
	{
		return itemComposition != null ? itemComposition.getMembersName() : NothingItemComposition.NAME;
	}
}
