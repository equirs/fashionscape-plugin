package eq.uirs.fashionscape.core;

import lombok.Value;

// A weapon whose observed idle pose anim disagrees with plugin data
@Value
public class WeaponAnimMismatch
{
	int itemId;
	// idle pose anim actually observed on the player
	int observedAnimId;
	// idle pose anim recorded incorrectly in fashionscape-data
	int dataAnimId;
}
