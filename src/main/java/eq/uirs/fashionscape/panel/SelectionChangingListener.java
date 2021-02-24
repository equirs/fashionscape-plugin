package eq.uirs.fashionscape.panel;

import net.runelite.api.kit.KitType;

interface SelectionChangingListener
{
	void slotChanging(KitType slot);

	void petChanging();
}
