package eq.uirs.fashionscape.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.kit.KitType;

// yes this is every player kit id in the game
@RequiredArgsConstructor
public enum Kit
{
	// region Hair

	BALD(KitType.HAIR, "Bald", false, 0),
	DREADLOCKS(KitType.HAIR, "Dreadlocks", false, 1),
	LONG(KitType.HAIR, "Long", false, 2),
	MEDIUM(KitType.HAIR, "Medium", false, 3),
	TONSURE(KitType.HAIR, "Tonsure", false, 4),
	SHORT(KitType.HAIR, "Short", false, 5),
	CROPPED(KitType.HAIR, "Cropped", false, 6),
	WILD_SPIKES(KitType.HAIR, "Wild spikes", false, 7),
	SPIKES(KitType.HAIR, "Spikes", false, 8),
	MOHAWK(KitType.HAIR, "Mohawk", false, 9),
	WIND_BRAIDS(KitType.HAIR, "Wind braids", false, 129),
	QUIFF(KitType.HAIR, "Quiff", false, 130),
	SAMURAI(KitType.HAIR, "Samurai", false, 131),
	PRINCELY(KitType.HAIR, "Princely", false, 132),
	CURTAINS(KitType.HAIR, "Curtains", false, 133),
	LONG_CURTAINS(KitType.HAIR, "Long curtains", false, 134),
	TOUSLED(KitType.HAIR, "Tousled", false, 144),
	SIDE_WEDGE(KitType.HAIR, "Side wedge", false, 145),
	FRONT_WEDGE(KitType.HAIR, "Front wedge", false, 146),
	FRONT_SPIKES(KitType.HAIR, "Front spikes", false, 147),
	FROHAWK(KitType.HAIR, "Frohawk", false, 148),
	REAR_SKIRT(KitType.HAIR, "Rear skirt", false, 149),
	QUEUE(KitType.HAIR, "Queue", false, 150),
	FRONT_SPLIT(KitType.HAIR, "Front split", false, 151),

	BALD_F(KitType.HAIR, "Bald", true, 45),
	BUN(KitType.HAIR, "Bun", true, 46),
	DREADLOCKS_F(KitType.HAIR, "Dreadlocks", true, 47),
	LONG_F(KitType.HAIR, "Long", true, 48),
	MEDIUM_F(KitType.HAIR, "Medium", true, 49),
	PIGTAILS(KitType.HAIR, "Pigtails", true, 50),
	SHORT_F(KitType.HAIR, "Short", true, 51),
	CROPPED_F(KitType.HAIR, "Cropped", true, 52),
	WILD_SPIKES_F(KitType.HAIR, "Wild spikes", true, 53),
	SPIKY(KitType.HAIR, "Spiky", true, 54),
	EARMUFFS(KitType.HAIR, "Earmuffs", true, 55),
	SIDE_PONY(KitType.HAIR, "Side pony", true, 118),
	CURLS(KitType.HAIR, "Curls", true, 119),
	WIND_BRAIDS_F(KitType.HAIR, "Wind braids", true, 120),
	PONYTAIL(KitType.HAIR, "Ponytail", true, 121),
	BRAIDS(KitType.HAIR, "Braids", true, 122),
	BUNCHES(KitType.HAIR, "Bunches", true, 123),
	BOB(KitType.HAIR, "Bob", true, 124),
	LAYERED(KitType.HAIR, "Layered", true, 125),
	STRAIGHT(KitType.HAIR, "Straight", true, 126),
	STRAIGHT_BRAIDS(KitType.HAIR, "Straight braids", true, 127),
	CURTAINS_F(KitType.HAIR, "Curtains", true, 128),
	FRONT_SPLIT_F(KitType.HAIR, "Front split", true, 141),
	TWO_BACK(KitType.HAIR, "Two-back", true, 143),

	// endregion Hair

	// region Jaw

	GOATEE(KitType.JAW, "Goatee", false, 10),
	LONG_J(KitType.JAW, "Long", false, 11),
	MEDIUM_J(KitType.JAW, "Medium", false, 12),
	SMALL_MOUSTACHE(KitType.JAW, "Small moustache", false, 13),
	CLEAN_SHAVEN(KitType.JAW, "Clean-shaven", false, 14),
	SHORT_J(KitType.JAW, "Short", false, 15),
	POINTY(KitType.JAW, "Pointy", false, 16),
	SPLIT(KitType.JAW, "Split", false, 17),
	HANDLEBAR(KitType.JAW, "Handlebar", false, 111),
	MUTTON(KitType.JAW, "Mutton", false, 112),
	FULL_MUTTON(KitType.JAW, "Full mutton", false, 113),
	BIG_MOUSTACHE(KitType.JAW, "Big moustache", false, 114),
	WAXED_MOUSTACHE(KitType.JAW, "Waxed moustache", false, 115),
	DALI(KitType.JAW, "Dali", false, 116),
	VIZIER(KitType.JAW, "Vizier", false, 117),

	// endregion Jaw

	// region Torso

	PLAIN(KitType.TORSO, "Plain", false, 18),
	LIGHT_BUTTONS(KitType.TORSO, "Light buttons", false, 19),
	DARK_BUTTONS(KitType.TORSO, "Dark buttons", false, 20),
	JACKET(KitType.TORSO, "Jacket", false, 21),
	SHIRT(KitType.TORSO, "Shirt", false, 22),
	STITCHING(KitType.TORSO, "Stitching", false, 23),
	TORN(KitType.TORSO, "Torn", false, 24),
	TWO_TONED(KitType.TORSO, "Two-toned", false, 25),
	SWEATER(KitType.TORSO, "Sweater", false, 105),
	BUTTONED_SHIRT(KitType.TORSO, "Buttoned shirt", false, 106),
	VEST(KitType.TORSO, "Vest", false, 107),
	PRINCELY_T(KitType.TORSO, "Princely", false, 108),
	RIPPED_WESKIT(KitType.TORSO, "Ripped weskit", false, 109),
	TORN_WESKIT(KitType.TORSO, "Torn weskit", false, 110),

	PLAIN_F(KitType.TORSO, "Plain", true, 56),
	CROP_TOP(KitType.TORSO, "Crop-top", true, 57),
	POLO_NECK(KitType.TORSO, "Polo-neck", true, 58),
	SIMPLE(KitType.TORSO, "Simple", true, 59),
	TORN_F(KitType.TORSO, "Torn", true, 60),
	SWEATER_F(KitType.TORSO, "Sweater", true, 89),
	SHIRT_F(KitType.TORSO, "Shirt", true, 90),
	VEST_F(KitType.TORSO, "Vest", true, 91),
	FRILLY(KitType.TORSO, "Frilly", true, 92),
	CORSETRY(KitType.TORSO, "Corsetry", true, 93),
	BODICE(KitType.TORSO, "Bodice", true, 94),

	// endregion Torso

	// region Arms

	REGULAR(KitType.ARMS, "Regular", false, 26),
	MUSCLEBOUND(KitType.ARMS, "Musclebound", false, 27),
	LOOSE_SLEEVED(KitType.ARMS, "Loose sleeved", false, 28),
	LARGE_CUFFED(KitType.ARMS, "Large cuffed", false, 29),
	THIN(KitType.ARMS, "Thin", false, 30),
	SHOULDER_PADS(KitType.ARMS, "Shoulder pads", false, 31),
	THIN_STRIPE(KitType.ARMS, "Thin stripe", false, 32),
	THICK_STRIPE(KitType.ARMS, "Thick stripe", false, 84),
	WHITE_CUFFS(KitType.ARMS, "White cuffs", false, 85),
	PRINCELY_A(KitType.ARMS, "Princely", false, 86),
	TATTY(KitType.ARMS, "Tatty", false, 87),
	RIPPED(KitType.ARMS, "Ripped", false, 88),

	SHORT_SLEEVES(KitType.ARMS, "Short sleeves", true, 61),
	BARE_ARMS(KitType.ARMS, "Bare arms", true, 62),
	MUSCLEY(KitType.ARMS, "Muscley", true, 63),
	LONG_SLEEVED(KitType.ARMS, "Long sleeved", true, 64),
	LARGE_CUFFS(KitType.ARMS, "Large cuffs", true, 65),
	FRILLY_A(KitType.ARMS, "Frilly", true, 66),
	SWEATER_A(KitType.ARMS, "Sweater", true, 95),
	WHITE_CUFFS_F(KitType.ARMS, "White cuffs", true, 96),
	THIN_STRIPE_F(KitType.ARMS, "Thin stripe", true, 97),
	TATTY_F(KitType.ARMS, "Tatty", true, 98),
	BARE_SHOULDERS(KitType.ARMS, "Bare shoulders", true, 99),

	// endregion Arms

	// region Legs

	PLAIN_L(KitType.LEGS, "Plain", false, 36),
	SHORTS(KitType.LEGS, "Shorts", false, 37),
	FLARES(KitType.LEGS, "Flares", false, 38),
	TURN_UPS(KitType.LEGS, "Turn-ups", false, 39),
	TATTY_L(KitType.LEGS, "Tatty", false, 40),
	BEACH(KitType.LEGS, "Beach", false, 41),
	PRINCELY_L(KitType.LEGS, "Princely", false, 100),
	LEGGINGS(KitType.LEGS, "Leggings", false, 101),
	SIDE_STRIPES(KitType.LEGS, "Side-stripes", false, 102),
	RIPPED_L(KitType.LEGS, "Ripped", false, 103),
	PATCHED(KitType.LEGS, "Patched", false, 104),

	PLAIN_LF(KitType.LEGS, "Plain", true, 70),
	SKIRT(KitType.LEGS, "Skirt", true, 71),
	FLARES_F(KitType.LEGS, "Flares", true, 72),
	LONG_SKIRT(KitType.LEGS, "Long skirt", true, 73),
	LONG_NARROW_SKIRT(KitType.LEGS, "Long narrow skirt", true, 74),
	TATTY_LF(KitType.LEGS, "Tatty", true, 75),
	TURN_UPS_F(KitType.LEGS, "Turn-ups", true, 76),
	SHORT_SKIRT(KitType.LEGS, "Short skirt", true, 77),
	LAYERED_L(KitType.LEGS, "Layered", true, 78),
	SASH_AND_DOTS(KitType.LEGS, "Sash & dots", true, 135),
	BIG_HEM(KitType.LEGS, "Big hem", true, 136),
	SASH_AND_TROUSERS(KitType.LEGS, "Sash & trousers", true, 137),
	PATTERNED(KitType.LEGS, "Patterned", true, 138),
	TORN_SKIRT(KitType.LEGS, "Torn skirt", true, 139),
	PATCHED_SKIRT(KitType.LEGS, "Patched skirt", true, 140),

	// endregion Legs

	// region Hands

	PLAIN_H(KitType.HANDS, "Plain", false, 34),
	BRACERS(KitType.HANDS, "Bracers", false, 35),

	PLAIN_HF(KitType.HANDS, "Plain", true, 68),
	BRACERS_F(KitType.HANDS, "Bracers", true, 69),

	// endregion Hands

	// region Boots

	SMALL(KitType.BOOTS, "Small", false, 42),
	LARGE(KitType.BOOTS, "Large", false, 43),

	SMALL_F(KitType.BOOTS, "Small", true, 79),
	LARGE_F(KitType.BOOTS, "Large", true, 80);

	// endregion Boots

	@Getter
	private final KitType kitType;

	@Getter
	private final String displayName;

	@Getter
	private final boolean isFemale;

	@Getter
	private final int kitId;

}
