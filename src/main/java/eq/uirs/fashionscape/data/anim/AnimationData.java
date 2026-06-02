package eq.uirs.fashionscape.data.anim;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.Value;

@Value
public class AnimationData
{
	@SerializedName("id")
	public int animId;

	@Nonnull
	@SerializedName("name")
	public String animName;

	@Nullable
	@SerializedName("exact")
	public List<String> exactMatches;

	@Nullable
	@SerializedName("contains")
	public List<String> containsMatches;

	@Nullable
	@SerializedName("regex")
	public List<String> regexMatches;

	@Nullable
	@SerializedName("item_ids")
	public List<Integer> itemIds;
}
