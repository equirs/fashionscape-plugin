package eq.uirs.fashionscape.core;

import eq.uirs.fashionscape.data.kit.ArmsKit;
import eq.uirs.fashionscape.data.kit.BootsKit;
import eq.uirs.fashionscape.data.kit.HairKit;
import eq.uirs.fashionscape.data.kit.HandsKit;
import eq.uirs.fashionscape.data.kit.JawKit;
import eq.uirs.fashionscape.data.kit.LegsKit;
import eq.uirs.fashionscape.data.kit.TorsoKit;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.kit.KitType;

public class TestData
{
	// items (if adding items that hide slots, update BaseTest::populate)

	public static final SlotInfo redPartyHat = SlotInfo.item(ItemID.RED_PARTYHAT, KitType.HEAD);
	public static final SlotInfo runeMedHelm = SlotInfo.item(ItemID.RUNE_MED_HELM, KitType.HEAD, KitType.HAIR);
	public static final SlotInfo faceMask = SlotInfo.item(ItemID.SLAYER_FACEMASK, KitType.HEAD, KitType.JAW);
	public static final SlotInfo ironFullHelm = SlotInfo.item(ItemID.IRON_FULL_HELM, KitType.HEAD, KitType.HAIR, KitType.JAW);

	public static final SlotInfo blueCape = SlotInfo.item(ItemID.BLUE_CAPE, KitType.CAPE);
	// this model was changed so it's not hooded anymore, but it's a nice edge case test
	public static final SlotInfo hoodedCloak = SlotInfo.item(ItemID.CASTLEWARS_CLOAK_SARADOMIN, KitType.CAPE, KitType.HEAD, KitType.HAIR);

	public static final SlotInfo camulet = SlotInfo.item(ItemID.CAMULET, KitType.AMULET);

	public static final SlotInfo blackMace = SlotInfo.item(ItemID.BLACK_MACE, KitType.WEAPON);
	public static final SlotInfo white2hSword = SlotInfo.item(ItemID.WHITE_2H_SWORD, KitType.WEAPON, KitType.SHIELD);
	public static final SlotInfo beachBoxingGloves = SlotInfo.item(ItemID.BEACHPARTY_BOXINGGLOVES_YELLOW, KitType.WEAPON, KitType.SHIELD, KitType.HANDS);

	public static final SlotInfo studdedBody = SlotInfo.item(ItemID.STUDDED_BODY, KitType.TORSO);
	public static final SlotInfo bronzePlatebody = SlotInfo.item(ItemID.BRONZE_PLATEBODY, KitType.TORSO, KitType.ARMS);
	public static final SlotInfo plagueJacket = SlotInfo.item(ItemID.PLAGUE_JACKET, KitType.TORSO, KitType.ARMS, KitType.HANDS);

	public static final SlotInfo gildedKiteshield = SlotInfo.item(ItemID.RUNE_KITESHIELD_GOLDPLATE, KitType.SHIELD);

	public static final SlotInfo mimeLegs = SlotInfo.item(ItemID.MACRO_MIME_LEGS, KitType.LEGS);
	public static final SlotInfo corruptedLegs = SlotInfo.item(ItemID.GAUNTLET_PLATELEGS_T3_HM, KitType.LEGS, KitType.BOOTS);

	public static final SlotInfo leatherGloves = SlotInfo.item(ItemID.LEATHER_GLOVES, KitType.HANDS);

	public static final SlotInfo pinkBoots = SlotInfo.item(ItemID.GNOME_BOOTS_PINK, KitType.BOOTS);

	// kits

	public static final SlotInfo plainTorso = SlotInfo.kit(TorsoKit.PLAIN, 0);
	public static final SlotInfo thinStripeArms = SlotInfo.kit(ArmsKit.THIN_STRIPE, 0);
	public static final SlotInfo flaresLegs = SlotInfo.kit(LegsKit.FLARES, 0);
	public static final SlotInfo curlsHair = SlotInfo.kit(HairKit.CURLS, 0);
	public static final SlotInfo smallBoots = SlotInfo.kit(BootsKit.SMALL, 0);
	public static final SlotInfo bracersHands = SlotInfo.kit(HandsKit.BRACERS, 0);
	public static final SlotInfo daliJaw = SlotInfo.kit(JawKit.DALI, 0);

	// equipment ids

	public static final int emptyJawDefenderIcon = 10558 + FashionManager.ITEM_OFFSET;

	// mostly kits, some items
	public static int[] comp1 = new int[]{
		0,
		blueCape.getEquipmentId(),
		0,
		white2hSword.getEquipmentId(),
		plainTorso.getEquipmentId(),
		0,
		thinStripeArms.getEquipmentId(),
		flaresLegs.getEquipmentId(),
		curlsHair.getEquipmentId(),
		bracersHands.getEquipmentId(),
		smallBoots.getEquipmentId(),
		daliJaw.getEquipmentId()
	};

	// mostly items + jaw icon
	public static int[] comp2 = new int[]{
		ironFullHelm.getEquipmentId(),
		0,
		camulet.getEquipmentId(),
		blackMace.getEquipmentId(),
		bronzePlatebody.getEquipmentId(),
		gildedKiteshield.getEquipmentId(),
		0,
		mimeLegs.getEquipmentId(),
		0,
		leatherGloves.getEquipmentId(),
		pinkBoots.getEquipmentId(),
		emptyJawDefenderIcon
	};
}
