package eq.uirs.fashionscape.panel;

import java.awt.FontMetrics;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicLabelUI;
import sun.awt.SunToolkit;

/**
 * JLabel that handles setting + showing tooltip text only when it is truncated.
 */
public class AutoTooltipLabel extends JLabel
{
	private static class LabelUI extends BasicLabelUI
	{
		private boolean isTruncated = false;

		@Override
		protected String layoutCL(JLabel label, FontMetrics fontMetrics, String text, Icon icon, Rectangle viewR,
								  Rectangle iconR, Rectangle textR)
		{
			String result = super.layoutCL(label, fontMetrics, text, icon, viewR, iconR, textR);
			isTruncated = !result.equals(text);
			return result;
		}
	}

	private final LabelUI labelUI = new LabelUI();

	public AutoTooltipLabel()
	{
		super();
		setUpUI();
	}

	private void setUpUI()
	{
		setUI(labelUI);
		addComponentListener(new ComponentAdapter()
		{
			@Override
			public void componentResized(ComponentEvent e)
			{
				checkTooltip();
			}
		});
		addPropertyChangeListener("text", evt -> checkTooltip());
	}

	private void checkTooltip()
	{
		// double thread switching to ensure that this check is done after revalidating the component
		SunToolkit.executeOnEventHandlerThread(this, () -> SwingUtilities.invokeLater(() -> {
			if (labelUI.isTruncated)
			{
				setToolTipText(getText());
			}
			else
			{
				setToolTipText(null);
			}
		}));
	}
}
