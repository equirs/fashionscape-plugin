package eq.uirs.fashionscape.data.anim;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import lombok.Value;

@Value
public class AllAnimationData
{
	@SerializedName("anims")
	public List<AnimationData> values;
}
