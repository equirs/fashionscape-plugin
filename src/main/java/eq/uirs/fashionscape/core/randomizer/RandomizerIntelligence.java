package eq.uirs.fashionscape.core.randomizer;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Determines the size of the queue for scoring the "best" matched item via randomizer request
 */
@Getter
@RequiredArgsConstructor
public enum RandomizerIntelligence
{
	NONE(1),
	LOW(10),
	MODERATE(50),
	HIGH(100),
	CURSED(50); // prefers mismatched colors

	private final int depth;
}
