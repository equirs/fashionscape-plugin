package eq.uirs.fashionscape.core.loadout;

import lombok.Value;

@Value
public class SavedLoadout
{
	// config key suffix; retained across renames and devices
	String id;
	Loadout loadout;
}
