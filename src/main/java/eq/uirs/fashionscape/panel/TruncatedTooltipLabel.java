package eq.uirs.fashionscape.panel;

import java.awt.Insets;
import java.awt.event.MouseEvent;
import javax.swing.JLabel;
import javax.swing.ToolTipManager;

/**
 * shows full text in a tooltip when the text is truncated
 */
class TruncatedTooltipLabel extends JLabel
{
	TruncatedTooltipLabel()
	{
		ToolTipManager.sharedInstance().registerComponent(this);
	}

	@Override
	public void setToolTipText(String text)
	{
		super.setToolTipText(text);
		// super unregisters on null text, but truncated text still needs a tooltip
		ToolTipManager.sharedInstance().registerComponent(this);
	}

	@Override
	public String getToolTipText(MouseEvent e)
	{
		String tooltip = super.getToolTipText(e);
		if (!isTruncated())
		{
			return tooltip;
		}
		return tooltip == null ? getText() : getText() + " (" + tooltip + ")";
	}

	private boolean isTruncated()
	{
		String text = getText();
		if (text == null || text.isEmpty())
		{
			return false;
		}
		Insets insets = getInsets();
		int available = getWidth() - insets.left - insets.right;
		return getFontMetrics(getFont()).stringWidth(text) > available;
	}
}
