package eq.uirs.fashionscape.core;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import eq.uirs.fashionscape.core.event.KnownKitChanged;
import eq.uirs.fashionscape.data.kit.ArmsKit;
import eq.uirs.fashionscape.data.kit.BootsKit;
import eq.uirs.fashionscape.data.kit.HairKit;
import eq.uirs.fashionscape.data.kit.HandsKit;
import eq.uirs.fashionscape.data.kit.JawKit;
import eq.uirs.fashionscape.data.kit.Kit;
import eq.uirs.fashionscape.data.kit.LegsKit;
import eq.uirs.fashionscape.data.kit.TorsoKit;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;
import net.runelite.api.kit.KitType;

// when real player kit info is not known, default values should be shown, otherwise models will look broken
public class Fallbacks
{
	private static final Map<KitType, Integer> MASC_KITS = new HashMap<>();
	private static final Map<KitType, Integer> FEM_KITS = new HashMap<>();

	// roughly corresponding kits that don't share the same name
	public static final BiMap<Kit, Kit> GENDER_MIRRORED_KITS;

	static
	{
		MASC_KITS.put(KitType.HAIR, HairKit.BALD.getKitId(0));
		MASC_KITS.put(KitType.JAW, JawKit.GOATEE.getKitId(0));
		MASC_KITS.put(KitType.TORSO, TorsoKit.PLAIN.getKitId(0));
		MASC_KITS.put(KitType.ARMS, ArmsKit.REGULAR.getKitId(0));
		MASC_KITS.put(KitType.LEGS, LegsKit.PLAIN_L.getKitId(0));
		MASC_KITS.put(KitType.HANDS, HandsKit.PLAIN_H.getKitId(0));
		MASC_KITS.put(KitType.BOOTS, BootsKit.SMALL.getKitId(0));

		FEM_KITS.put(KitType.HAIR, HairKit.PIGTAILS.getKitId(1));
		FEM_KITS.put(KitType.JAW, JawKit.CLEAN_SHAVEN.getKitId(1));
		FEM_KITS.put(KitType.TORSO, TorsoKit.SIMPLE.getKitId(1));
		FEM_KITS.put(KitType.ARMS, ArmsKit.SHORT_SLEEVES.getKitId(1));
		FEM_KITS.put(KitType.LEGS, LegsKit.PLAIN_L.getKitId(1));
		FEM_KITS.put(KitType.HANDS, HandsKit.PLAIN_H.getKitId(1));
		FEM_KITS.put(KitType.BOOTS, BootsKit.SMALL.getKitId(1));

		Map<Kit, Kit> mToF = new HashMap<>();
		mToF.put(ArmsKit.REGULAR, ArmsKit.SHORT_SLEEVES);
		mToF.put(ArmsKit.MUSCLEBOUND, ArmsKit.MUSCLEY);
		mToF.put(ArmsKit.LOOSE_SLEEVED, ArmsKit.FRILLY_A);
		mToF.put(ArmsKit.LARGE_CUFFED, ArmsKit.LARGE_CUFFS);
		mToF.put(ArmsKit.THIN, ArmsKit.LONG_SLEEVED);
		mToF.put(ArmsKit.SHOULDER_PADS, ArmsKit.BARE_ARMS);
		mToF.put(ArmsKit.THICK_STRIPE, ArmsKit.SWEATER_A);
		mToF.put(ArmsKit.RIPPED, ArmsKit.BARE_SHOULDERS);

		mToF.put(LegsKit.SHORTS, LegsKit.SHORT_SKIRT);
		mToF.put(LegsKit.RIPPED_L, LegsKit.TORN_SKIRT);
		mToF.put(LegsKit.PATCHED, LegsKit.PATCHED_SKIRT);
		mToF.put(LegsKit.BEACH, LegsKit.BIG_HEM);
		GENDER_MIRRORED_KITS = new ImmutableBiMap.Builder<Kit, Kit>()
			.putAll(mToF)
			.build();
	}

	/**
	 * Produces the fallback kit id for the given slot+gender.
	 * Returns an effective equipment id of 0 if there is no suitable kit id.
	 * If `detectUnknown`, the UI will update, prompting the user to reveal this kit.
	 */
	public static int getKit(KitType slot, Integer gender, boolean detectUnknown)
	{
		if (gender == null || slot == null)
		{
			return -FashionManager.KIT_OFFSET;
		}
		Map<KitType, Integer> map = gender == 1 ? FEM_KITS : MASC_KITS;
		int result = map.getOrDefault(slot, -FashionManager.KIT_OFFSET);
		if (detectUnknown && result > 0)
		{
			Events.fire(new KnownKitChanged(true, slot));
		}
		return result;
	}

	@Nullable
	public static Kit getMirroredKit(Kit kit, int gender)
	{
		BiMap<Kit, Kit> lookup = Objects.equals(gender, 0) ? GENDER_MIRRORED_KITS : GENDER_MIRRORED_KITS.inverse();
		return lookup.get(kit);
	}
}
