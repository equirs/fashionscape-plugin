package eq.uirs.fashionscape.data;

import com.google.gson.annotations.SerializedName;
import lombok.Value;

// these are deserialized from json hosted in the data repo
@Value
public class ItemSlotInfo
{
	@SerializedName("w1")
	public int slot;
	@SerializedName("w2")
	public Integer hidden0;
	@SerializedName("w3")
	public Integer hidden1;
}
