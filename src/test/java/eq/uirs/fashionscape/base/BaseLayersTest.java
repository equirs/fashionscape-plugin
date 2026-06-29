package eq.uirs.fashionscape.base;

import com.google.inject.Inject;
import eq.uirs.fashionscape.core.layer.Layers;
import eq.uirs.fashionscape.core.model.Kits;
import eq.uirs.fashionscape.core.model.ModelInfo;
import java.util.Map;
import net.runelite.api.kit.KitType;
import org.junit.jupiter.api.AfterEach;

public class BaseLayersTest extends BaseTest
{
	@AfterEach
	public void tearDown()
	{
		resetData();
	}

	@Inject
	protected Layers layers;

	protected void resetData()
	{
		Layers.resetModels(layers.getVirtualModels());
		layers.resetRealInfo();
	}

	protected void setKitsOnModels(Map<KitType, Integer> values, ModelInfo models)
	{
		Kits kits = models.getKits();
		values.forEach(kits::put);
	}
}
