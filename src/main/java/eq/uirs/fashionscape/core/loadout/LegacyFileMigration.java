package eq.uirs.fashionscape.core.loadout;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.util.Filepath;

/**
 * Imports loadouts from the text files that older versions saved (originally in .runelite/outfits).
 * Legacy loadouts were only ever saved locally, so this runs once per device.
 */
@Slf4j
@Singleton
public class LegacyFileMigration
{
	// written to the folder after importing (a config would block imports on other devices)
	private static final String MARKER = "imported-to-loadouts";
	private static final String EXTENSION = ".txt";

	private final LoadoutCodec codec;
	private final LoadoutStore store;

	@Inject
	LegacyFileMigration(LoadoutCodec codec, LoadoutStore store)
	{
		this.codec = codec;
		this.store = store;
	}

	/**
	 * Imports each text file in the folder as a loadout named after the file.
	 * Runs on an executor service. Does blocking disk IO.
	 */
	public void migrate(Filepath dir) throws IOException
	{
		Filepath marker = dir.joinSegment(MARKER);
		if (!dir.isDirectory() || marker.exists())
		{
			return;
		}
		List<Filepath> files;
		try (Stream<Filepath> paths = dir.walk(1))
		{
			files = paths
				.filter(f -> f.isFile() && f.getFileName().endsWith(EXTENSION))
				.sorted()
				.collect(Collectors.toList());
		}
		int imported = 0;
		for (Filepath file : files)
		{
			imported += importFile(file);
		}
		marker.write("");
		log.info("Imported {} loadouts from {} legacy files", imported, files.size());
	}

	private int importFile(Filepath file)
	{
		try (BufferedReader reader = file.openBufferedReader())
		{
			String text = reader.lines().collect(Collectors.joining("\n"));
			int imported = 0;
			for (Loadout loadout : codec.parse(text, LoadoutCodec.nameFromFile(file.getFileName())))
			{
				// another device may have imported the same file already
				if (store.getAll().stream().noneMatch(s -> s.getLoadout().equals(loadout)))
				{
					store.add(loadout);
					imported++;
				}
			}
			return imported;
		}
		catch (IOException | IllegalArgumentException e)
		{
			log.warn("Could not import legacy file {}", file, e);
			return 0;
		}
	}
}
