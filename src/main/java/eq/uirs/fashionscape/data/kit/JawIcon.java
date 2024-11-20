package eq.uirs.fashionscape.data.kit;

import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

@Getter
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

	@NotNull
	public static JawIcon fromId(int id)
	{
		return Arrays.stream(JawIcon.values())
			.filter(i -> i.id == id)
			.findFirst()
			.orElse(JawIcon.NOTHING);
	}

	private final String displayName;

	// representative item id for displaying in panel icon and for color scoring (no facial hair)
	private final int id;
}
