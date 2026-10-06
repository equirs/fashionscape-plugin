package eq.uirs.fashionscape.core.loadout;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import eq.uirs.fashionscape.core.event.ActiveLoadoutsChanged;
import eq.uirs.fashionscape.core.event.ItemChanged;
import eq.uirs.fashionscape.core.event.LoadoutsChanged;
import eq.uirs.fashionscape.core.layer.ModelType;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import net.runelite.api.Client;
import net.runelite.api.kit.KitType;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.EventBus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ActiveLoadoutsTest
{
	private final ClientThread clientThread = mock(ClientThread.class);
	private final EventBus eventBus = mock(EventBus.class);
	private final LoadoutManager loadoutManager = mock(LoadoutManager.class);
	private final LoadoutStore store = mock(LoadoutStore.class);

	private final Loadout first = named("first");
	private final Loadout second = named("second");
	private List<SavedLoadout> saved = ImmutableList.of(new SavedLoadout("a", first), new SavedLoadout("b", second));
	// loadout that isApplied returns true for
	private Loadout applied;
	private ActiveLoadouts active;

	@BeforeEach
	void setUp()
	{
		doAnswer(i -> {
			i.<Runnable>getArgument(0).run();
			return null;
		}).when(clientThread).invokeLater(any(Runnable.class));
		when(loadoutManager.isApplied(any())).thenAnswer(i -> i.getArgument(0) == applied);
		when(store.getAll()).thenAnswer(i -> saved);
		active = new ActiveLoadouts(mock(Client.class), clientThread, eventBus, loadoutManager, store);
	}

	@Test
	void followsLookChanges()
	{
		lookChanged(first);
		assertEquals(ImmutableSet.of("a"), active.getActiveIds());
		verify(eventBus).post(new ActiveLoadoutsChanged(ImmutableSet.of("a")));

		lookChanged(second);
		assertEquals(ImmutableSet.of("b"), active.getActiveIds());
		lookChanged(null);
		assertTrue(active.getActiveIds().isEmpty());
	}

	@Test
	void ignoresPreviewChanges()
	{
		applied = first;
		active.onItemChanged(new ItemChanged(KitType.HEAD, ModelType.PREVIEW, null));
		assertTrue(active.getActiveIds().isEmpty());
	}

	@Test
	void deletedLoadoutIsInactive()
	{
		lookChanged(first);
		saved = ImmutableList.of(new SavedLoadout("b", second));
		active.onLoadoutsChanged(new LoadoutsChanged());
		assertTrue(active.getActiveIds().isEmpty());
	}

	@Test
	void unchangedResultPostsNothing()
	{
		lookChanged(first);
		clearInvocations(eventBus);
		lookChanged(first);
		verify(eventBus, never()).post(any());
	}

	private void lookChanged(Loadout look)
	{
		applied = look;
		active.onItemChanged(new ItemChanged(KitType.HEAD, ModelType.VIRTUAL, null));
	}

	private static Loadout named(String name)
	{
		return new Loadout().withName(name);
	}
}
