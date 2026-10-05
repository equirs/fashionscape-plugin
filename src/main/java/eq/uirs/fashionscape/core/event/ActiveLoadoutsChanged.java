package eq.uirs.fashionscape.core.event;

import java.util.Set;
import lombok.Value;

@Value
public class ActiveLoadoutsChanged
{
	// ids of saved loadouts that match the current look
	Set<String> ids;
}
