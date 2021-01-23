package com.equirs.fashionscape.chatbox;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.client.game.SpriteOverride;

@RequiredArgsConstructor
enum ChatboxSprites implements SpriteOverride
{
	LEFT_ARROW(-400, "arrow-left.png"),
	RIGHT_ARROW(-401, "arrow-right.png");

	@Getter
	private final int spriteId;

	@Getter
	private final String fileName;
}
