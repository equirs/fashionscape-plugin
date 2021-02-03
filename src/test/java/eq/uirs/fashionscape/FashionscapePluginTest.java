package eq.uirs.fashionscape;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class FashionscapePluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(FashionscapePlugin.class);
		RuneLite.main(args);
	}
}
