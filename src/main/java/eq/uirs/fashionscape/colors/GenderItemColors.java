package eq.uirs.fashionscape.colors;

import com.google.gson.annotations.SerializedName;
import lombok.Value;

@Value
public class GenderItemColors
{
	@SerializedName("a")
	public ItemColors any;
	@SerializedName("m")
	public ItemColors masc;
	@SerializedName("f")
	public ItemColors fem;
}
