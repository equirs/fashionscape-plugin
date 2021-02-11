package eq.uirs.fashionscape.swap;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Determines the size of the queue for scoring the "best" matched item via randomizer request
 */
@RequiredArgsConstructor
public enum RandomizerIntelligence
{
	NONE(1),
	LOW(15),
	MODERATE(30),
	HIGH(50);

	@Getter
	private final int depth;
}
