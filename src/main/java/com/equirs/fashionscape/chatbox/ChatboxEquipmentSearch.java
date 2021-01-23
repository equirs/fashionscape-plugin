/*
 * Copyright (c) 2019, Ron Young <https://github.com/raiyni>
 * All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *     list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.equirs.fashionscape.chatbox;

import com.google.inject.Inject;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.inject.Singleton;
import lombok.Getter;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.ItemComposition;
import net.runelite.api.widgets.ItemQuantityMode;
import net.runelite.api.widgets.JavaScriptCallback;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetPositionMode;
import net.runelite.api.widgets.WidgetSizeMode;
import net.runelite.api.widgets.WidgetTextAlignment;
import net.runelite.api.widgets.WidgetType;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.chatbox.ChatboxPanelManager;
import net.runelite.client.game.chatbox.ChatboxTextInput;
import net.runelite.client.ui.JagexColors;

@Singleton
public class ChatboxEquipmentSearch extends ChatboxTextInput
{
	private static final int ICON_HEIGHT = 32;
	private static final int ICON_WIDTH = 36;
	private static final int PADDING = 6;
	private static final int PAGE_SIZE = 24;
	private static final int FONT_SIZE = 16;
	private static final int HOVERED_OPACITY = 128;
	private static final int BUTTON_WIDTH = 20;
	private static final int BUTTON_HEIGHT = 29;
	private static final String PROMPT = "Item Search";

	private final ChatboxPanelManager chatboxPanelManager;
	private final ItemManager itemManager;
	private final Client client;

	private final List<ItemComposition> results = new ArrayList<>();
	private String tooltipText;
	private boolean filterDuplicateIcons = true;
	private boolean allowEmpty;
	private Function<ItemComposition, Boolean> filter;
	private int maxResults = PAGE_SIZE;
	private int currentPage;
	private int index = -1;

	@Getter
	private Consumer<Integer> onItemSelected;
	@Getter
	private Consumer<Integer> onItemHovered;

	@Value
	private static class ItemIcon
	{
		private final int modelId;
		private final short[] colorsToReplace;
		private final short[] texturesToReplace;
	}

	@Inject
	private ChatboxEquipmentSearch(ChatboxPanelManager chatboxPanelManager, ClientThread clientThread,
								   ItemManager itemManager, Client client)
	{
		super(chatboxPanelManager, clientThread);
		this.chatboxPanelManager = chatboxPanelManager;
		this.itemManager = itemManager;
		this.client = client;

		lines(1);
		prompt(PROMPT);
		onChanged(searchString ->
			clientThread.invokeLater(() ->
			{
				filterResults();
				update();
			}));
	}

	@Override
	public String getPrompt()
	{
		if (!results.isEmpty() && maxResults > PAGE_SIZE)
		{
			StringBuilder sb = new StringBuilder()
				.append(results.size())
				.append(" Results");
			if (results.size() > PAGE_SIZE)
			{
				sb.append(" (Page ")
					.append(currentPage + 1)
					.append("/")
					.append((results.size() - 1) / PAGE_SIZE + 1)
					.append(")");
			}
			return sb.toString();
		}
		return super.getPrompt();
	}

	@Override
	protected void update()
	{
		Widget container = chatboxPanelManager.getContainerWidget();
		container.deleteAllChildren();

		Widget promptWidget = container.createChild(-1, WidgetType.TEXT);
		promptWidget.setText(getPrompt());
		promptWidget.setTextColor(0x800000);
		promptWidget.setFontId(getFontID());
		promptWidget.setOriginalX(0);
		promptWidget.setOriginalY(5);
		promptWidget.setXPositionMode(WidgetPositionMode.ABSOLUTE_CENTER);
		promptWidget.setYPositionMode(WidgetPositionMode.ABSOLUTE_TOP);
		promptWidget.setOriginalHeight(FONT_SIZE);
		if (maxResults > PAGE_SIZE)
		{
			promptWidget.setOriginalWidth(BUTTON_WIDTH * 2 + 8);
		}
		promptWidget.setXTextAlignment(WidgetTextAlignment.CENTER);
		promptWidget.setYTextAlignment(WidgetTextAlignment.CENTER);
		promptWidget.setWidthMode(WidgetSizeMode.MINUS);
		promptWidget.revalidate();

		buildEdit(0, 5 + FONT_SIZE, container.getWidth(), FONT_SIZE);

		Widget separator = container.createChild(-1, WidgetType.LINE);
		separator.setOriginalX(0);
		separator.setOriginalY(8 + (FONT_SIZE * 2));
		separator.setXPositionMode(WidgetPositionMode.ABSOLUTE_CENTER);
		separator.setYPositionMode(WidgetPositionMode.ABSOLUTE_TOP);
		separator.setOriginalHeight(0);
		if (maxResults > PAGE_SIZE)
		{
			separator.setOriginalWidth(BUTTON_WIDTH * 2 + 16);
		}
		else
		{
			separator.setOriginalWidth(16);
		}
		separator.setWidthMode(WidgetSizeMode.MINUS);
		separator.setTextColor(0x666666);
		separator.revalidate();

		if (results.size() > PAGE_SIZE)
		{
			Widget leftArrow = container.createChild(-1, WidgetType.GRAPHIC);
			leftArrow.setOriginalWidth(BUTTON_WIDTH);
			leftArrow.setOriginalHeight(BUTTON_HEIGHT);
			leftArrow.setXPositionMode(WidgetPositionMode.ABSOLUTE_LEFT);
			leftArrow.setYPositionMode(WidgetPositionMode.ABSOLUTE_TOP);
			leftArrow.setOriginalX(4);
			leftArrow.setOriginalY(7);
			leftArrow.setSpriteId(ChatboxSprites.LEFT_ARROW.getSpriteId());
			leftArrow.setName("Page");
			leftArrow.setBorderType(1);
			leftArrow.setAction(0, "Prev.");
			leftArrow.setHasListener(true);
			leftArrow.setOnOpListener((JavaScriptCallback) ev -> {
				previousPage();
				clientThread.invokeLater(this::update);
			});
			leftArrow.revalidate();

			Widget rightArrow = container.createChild(-1, WidgetType.GRAPHIC);
			rightArrow.setOriginalWidth(BUTTON_WIDTH);
			rightArrow.setOriginalHeight(BUTTON_HEIGHT);
			rightArrow.setXPositionMode(WidgetPositionMode.ABSOLUTE_RIGHT);
			rightArrow.setYPositionMode(WidgetPositionMode.ABSOLUTE_TOP);
			rightArrow.setOriginalX(4);
			rightArrow.setOriginalY(7);
			rightArrow.setSpriteId(ChatboxSprites.RIGHT_ARROW.getSpriteId());
			rightArrow.setName("Page");
			rightArrow.setBorderType(1);
			rightArrow.setAction(0, "Next");
			rightArrow.setHasListener(true);
			rightArrow.setOnOpListener((JavaScriptCallback) ev -> {
				nextPage();
				clientThread.invokeLater(this::update);
			});
			rightArrow.revalidate();
		}

		int x = PADDING;
		int y = PADDING * 3;
		for (int i = 0; i < Math.min(PAGE_SIZE, results.size()); i++)
		{
			int resultIndex = PAGE_SIZE * currentPage + i;
			if (resultIndex >= results.size())
			{
				break;
			}
			ItemComposition itemComposition = results.get(resultIndex);
			Widget item = container.createChild(-1, WidgetType.GRAPHIC);
			item.setXPositionMode(WidgetPositionMode.ABSOLUTE_LEFT);
			item.setYPositionMode(WidgetPositionMode.ABSOLUTE_TOP);
			item.setOriginalX(x);
			item.setOriginalY(y + FONT_SIZE * 2);
			item.setOriginalHeight(ICON_HEIGHT);
			item.setOriginalWidth(ICON_WIDTH);
			item.setName(JagexColors.MENU_TARGET_TAG + itemComposition.getName());
			item.setItemId(itemComposition.getId());
			item.setItemQuantity(10000);
			item.setItemQuantityMode(ItemQuantityMode.NEVER);
			item.setBorderType(1);
			item.setAction(0, tooltipText);
			item.setHasListener(true);

			if (index == i)
			{
				setItemHovering(item, true);
			}
			else
			{
				item.setOnMouseOverListener((JavaScriptCallback) ev -> setItemHovering(item, true));
				item.setOnMouseLeaveListener((JavaScriptCallback) ev -> setItemHovering(item, false));
			}

			item.setOnOpListener((JavaScriptCallback) ev ->
			{
				if (onItemSelected != null)
				{
					onItemSelected.accept(itemComposition.getId());
				}

				chatboxPanelManager.close();
			});

			x += ICON_WIDTH + PADDING;
			if (x + ICON_WIDTH >= container.getWidth())
			{
				y += ICON_HEIGHT + PADDING;
				x = PADDING;
			}

			item.revalidate();
		}
	}

	@Override
	public void keyPressed(KeyEvent ev)
	{
		if (!chatboxPanelManager.shouldTakeInput())
		{
			return;
		}

		switch (ev.getKeyCode())
		{
			case KeyEvent.VK_ENTER:
				ev.consume();
				if (index > -1)
				{
					if (onItemSelected != null)
					{
						int resultIndex = currentPage * PAGE_SIZE + index;
						onItemSelected.accept(results.get(resultIndex).getId());
					}

					chatboxPanelManager.close();
				}
				break;
			case KeyEvent.VK_TAB:
				ev.consume();
				if (results.size() <= PAGE_SIZE)
				{
					break;
				}
				if ((ev.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) != 0)
				{
					previousPage();
				}
				else
				{
					nextPage();
				}
				clientThread.invokeLater(this::update);
				break;
			case KeyEvent.VK_RIGHT:
				ev.consume();
				if (!results.isEmpty())
				{
					index++;
					if ((currentPage == (results.size() - 1) / PAGE_SIZE && index >= results.size() % PAGE_SIZE) ||
						index >= PAGE_SIZE)
					{
						index = 0;
						nextPage();
					}
					clientThread.invokeLater(this::update);
				}
				break;
			case KeyEvent.VK_LEFT:
				ev.consume();
				if (!results.isEmpty())
				{
					index--;
					if (index < 0)
					{
						index = PAGE_SIZE - 1;
						previousPage();
					}
					clientThread.invokeLater(this::update);
				}
				break;
			case KeyEvent.VK_UP:
				ev.consume();
				if (results.size() >= (PAGE_SIZE / 2))
				{
					if (index == -1)
					{
						index = 0;
					}
					else if (index < PAGE_SIZE / 2)
					{
						index += PAGE_SIZE / 2;
						previousPage();
					}
					else
					{
						index -= PAGE_SIZE / 2;
					}
					clientThread.invokeLater(this::update);
				}
				break;
			case KeyEvent.VK_DOWN:
				ev.consume();
				if (results.size() >= (PAGE_SIZE / 2))
				{
					if (index == -1)
					{
						index = 0;
					}
					else if (index >= PAGE_SIZE / 2)
					{
						index -= PAGE_SIZE / 2;
						nextPage();
					}
					else
					{
						index += PAGE_SIZE / 2;
					}
					clientThread.invokeLater(this::update);
				}
				break;
			default:
				super.keyPressed(ev);
		}
	}

	@Override
	protected void close()
	{
		// Clear search string when closed
		value("");
		results.clear();
		index = -1;
		super.close();
	}

	@Override
	@Deprecated
	public ChatboxTextInput onDone(Consumer<String> onDone)
	{
		throw new UnsupportedOperationException();
	}

	private void nextPage()
	{
		currentPage++;
		if (currentPage > (results.size() - 1) / PAGE_SIZE)
		{
			currentPage = 0;
		}
		// ensure index does not extend beyond results size
		if (index != -1 && currentPage == (results.size() - 1) / PAGE_SIZE && index >= results.size() % PAGE_SIZE)
		{
			index = ((results.size() % PAGE_SIZE) - 1 + PAGE_SIZE) % PAGE_SIZE;
		}
	}

	private void previousPage()
	{
		currentPage--;
		if (currentPage < 0)
		{
			currentPage = (results.size() - 1) / PAGE_SIZE;
		}
		// ensure index does not extend beyond results size
		if (index != -1 && currentPage == (results.size() - 1) / PAGE_SIZE && index >= results.size() % PAGE_SIZE)
		{
			index = ((results.size() % PAGE_SIZE) - 1 + PAGE_SIZE) % PAGE_SIZE;
		}
	}

	private void filterResults()
	{
		results.clear();
		index = -1;
		currentPage = 0;

		String search = getValue().toLowerCase();
		if (!allowEmpty && search.isEmpty())
		{
			return;
		}

		Set<Integer> ids = new HashSet<>();
		Set<ItemIcon> itemIcons = new HashSet<>();
		for (int i = 0; i < client.getItemCount() && results.size() < maxResults; i++)
		{
			ItemComposition itemComposition = itemManager.getItemComposition(itemManager.canonicalize(i));
			// id might already be in results due to canonicalize
			if (!ids.contains(itemComposition.getId()) && isValidSearch(itemComposition, search))
			{
				// Check if the results already contain the same item image
				ItemIcon itemIcon = new ItemIcon(itemComposition.getInventoryModel(),
					itemComposition.getColorToReplaceWith(), itemComposition.getTextureToReplaceWith());
				if (filterDuplicateIcons && itemIcons.contains(itemIcon))
				{
					continue;
				}

				itemIcons.add(itemIcon);
				ids.add(itemComposition.getId());
				results.add(itemComposition);
			}
		}
	}

	private boolean isValidSearch(ItemComposition itemComposition, String query)
	{
		String name = itemComposition.getName().toLowerCase();
		// The client assigns "null" to item names of items it doesn't know about
		if (name.equals("null") || !name.contains(query))
		{
			return false;
		}
		return filter == null || filter.apply(itemComposition);
	}

	private void setItemHovering(Widget item, boolean hovering)
	{
		if (hovering)
		{
			item.setOpacity(HOVERED_OPACITY);
			if (onItemHovered != null)
			{
				onItemHovered.accept(item.getItemId());
			}
		}
		else
		{
			item.setOpacity(0);
		}
	}

	public ChatboxEquipmentSearch onItemSelected(Consumer<Integer> onItemSelected)
	{
		this.onItemSelected = onItemSelected;
		return this;
	}

	public ChatboxEquipmentSearch onItemHovered(Consumer<Integer> onItemHovered)
	{
		this.onItemHovered = onItemHovered;
		return this;
	}

	public ChatboxEquipmentSearch tooltipText(final String text)
	{
		tooltipText = text;
		return this;
	}

	public ChatboxEquipmentSearch filterDuplicateIcons(boolean filterDuplicateIcons)
	{
		this.filterDuplicateIcons = filterDuplicateIcons;
		return this;
	}

	public ChatboxEquipmentSearch filter(Function<ItemComposition, Boolean> filter)
	{
		this.filter = filter;
		return this;
	}

	public ChatboxEquipmentSearch allowEmpty(boolean allowEmpty)
	{
		this.allowEmpty = allowEmpty;
		if (allowEmpty)
		{
			getOnChanged().accept("");
		}
		return this;
	}

	public ChatboxEquipmentSearch maxResults(int maxResults)
	{
		this.maxResults = maxResults;
		return this;
	}
}
