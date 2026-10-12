package eq.uirs.fashionscape.panel.search;

import java.awt.Color;
import java.awt.GridLayout;
import java.text.ParseException;
import java.util.function.BiConsumer;
import javax.annotation.Nullable;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.util.QuantityFormatter;

/**
 * Min and max value inputs. Accepts shorthand such as 10k or 1.5m.
 */
class ValueRangeFields extends JPanel
{
	private final JTextField minField = createField("Min");
	private final JTextField maxField = createField("Max");
	private final Color validColor = minField.getForeground();
	private final BiConsumer<Integer, Integer> onChange;

	ValueRangeFields(BiConsumer<Integer, Integer> onChange)
	{
		super(new GridLayout(1, 2, 5, 0));
		this.onChange = onChange;
		setOpaque(false);
		add(minField);
		add(maxField);
	}

	void setRange(@Nullable Integer min, @Nullable Integer max)
	{
		minField.setText(min != null ? String.valueOf(min) : "");
		maxField.setText(max != null ? String.valueOf(max) : "");
	}

	private JTextField createField(String placeholder)
	{
		JTextField field = new JTextField();
		field.setToolTipText(placeholder + " value (e.g. 10k or 1.5m)");
		field.putClientProperty("JTextField.placeholderText", placeholder);
		field.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent e)
			{
				onTextChanged();
			}

			@Override
			public void removeUpdate(DocumentEvent e)
			{
				onTextChanged();
			}

			@Override
			public void changedUpdate(DocumentEvent e)
			{
				onTextChanged();
			}
		});
		return field;
	}

	private void onTextChanged()
	{
		onChange.accept(parse(minField), parse(maxField));
	}

	// invalid text turns red and leaves that bound unset
	@Nullable
	private Integer parse(JTextField field)
	{
		String text = field.getText().trim();
		field.setForeground(validColor);
		if (text.isEmpty())
		{
			return null;
		}
		try
		{
			long value = QuantityFormatter.parseQuantity(text);
			return (int) Math.max(0, Math.min(value, Integer.MAX_VALUE));
		}
		catch (ParseException e)
		{
			field.setForeground(ColorScheme.PROGRESS_ERROR_COLOR);
			return null;
		}
	}
}
