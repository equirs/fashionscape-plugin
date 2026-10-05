package eq.uirs.fashionscape.core.loadout;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import net.runelite.api.kit.KitType;
import net.runelite.client.util.Filepath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LegacyFileMigrationTest
{
	@TempDir
	Path dir;

	private final List<SavedLoadout> saved = new ArrayList<>();
	private final LoadoutStore store = mock(LoadoutStore.class);
	private LegacyFileMigration migration;

	@BeforeEach
	void setUp()
	{
		when(store.getAll()).thenAnswer(i -> new ArrayList<>(saved));
		when(store.add(any())).thenAnswer(i -> {
			SavedLoadout added = new SavedLoadout(String.valueOf(saved.size()), i.getArgument(0));
			saved.add(added);
			return added;
		});
		migration = new LegacyFileMigration(new LoadoutCodec(new Gson()), store);
	}

	@Test
	void importsTextFilesNamedAfterFile() throws IOException
	{
		write("void.txt", "HEAD:11665 (Void melee helm)");
		write("notes.md", "HEAD:1163");
		write("junk.txt", "nothing useful");
		migration.migrate(Filepath.Unchecked.getRooted(dir));

		assertEquals(1, saved.size());
		Loadout loadout = saved.get(0).getLoadout();
		assertEquals("void", loadout.getName());
		assertEquals(ImmutableMap.of(KitType.HEAD, 11665), loadout.getItems());
	}

	@Test
	void runsOncePerFolder() throws IOException
	{
		write("void.txt", "HEAD:11665");
		migration.migrate(Filepath.Unchecked.getRooted(dir));
		saved.clear();
		migration.migrate(Filepath.Unchecked.getRooted(dir));
		assertTrue(saved.isEmpty());
	}

	@Test
	void skipsLoadoutsAlreadySaved() throws IOException
	{
		write("void.txt", "HEAD:11665");
		Loadout existing = new Loadout().withName("void");
		existing.getItems().put(KitType.HEAD, 11665);
		saved.add(new SavedLoadout("existing", existing));

		migration.migrate(Filepath.Unchecked.getRooted(dir));
		assertEquals(1, saved.size());
	}

	@Test
	void missingFolderDoesNothing() throws IOException
	{
		migration.migrate(Filepath.Unchecked.getRooted(dir.resolve("missing")));
		assertTrue(saved.isEmpty());
	}

	private void write(String name, String contents) throws IOException
	{
		Files.write(dir.resolve(name), contents.getBytes());
	}
}
