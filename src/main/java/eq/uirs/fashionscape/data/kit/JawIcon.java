package eq.uirs.fashionscape.data.kit;

import java.util.Arrays;
import javax.annotation.Nullable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum JawIcon
{
	NOTHING("No icon", -1),
	BA_ATTACKER("Attacker icon", 10556),
	BA_DEFENDER("Defender icon", 10558),
	BA_COLLECTOR("Collector icon", 10557),
	BA_HEALER("Healer icon", 10559),
	SW_BLUE("Blue icon", 25212),
	SW_RED("Red icon", 25228);

	@Nullable
	public static JawIcon fromId(int id)
	{
		return Arrays.stream(JawIcon.values())
			.filter(i -> i.id == id)
			.findFirst()
			.orElse(null);
	}

	@Getter
	private final String displayName;

	// representative item id for displaying in panel icon and for color scoring
	@Getter
	private final int id;
}
