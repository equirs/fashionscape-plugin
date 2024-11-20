package eq.uirs.fashionscape.remote;

import com.google.gson.Gson;
import eq.uirs.fashionscape.colors.ItemColors;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import okhttp3.CacheControl;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.jetbrains.annotations.NotNull;

// handles fetching colors and item slot data from raw GitHub urls
@Singleton
@Slf4j
public class RemoteDataHandler
{
	private static final String BASE_URL = "https://raw.githubusercontent.com/equirs/fashionscape-data/";
	private static final String BRANCH_PROD = "master";
	private static final String BRANCH_DEV = "dev";

	private final OkHttpClient okHttpClient;
	private final Gson gson;
	private final boolean developerMode;
	private final List<BiConsumer<RemoteCategory, Boolean>> onReceiveDataListeners = new ArrayList<>();

	@Inject
	RemoteDataHandler(OkHttpClient okHttpClient, Gson baseGson, @Named("developerMode") boolean developerMode)
	{
		this.okHttpClient = okHttpClient;
		this.developerMode = developerMode;
		this.gson = baseGson.newBuilder()
			.registerTypeAdapter(ItemColors.class, new ItemColors.Deserializer())
			.create();
	}

	final Set<RemoteCategory> failedCategories = new HashSet<>();
	final Set<RemoteCategory> successCategories = new HashSet<>();

	public boolean hasFailed()
	{
		return !failedCategories.isEmpty();
	}

	public boolean hasSucceeded(RemoteCategory category)
	{
		return successCategories.contains(category);
	}

	public void fetch()
	{
		retry(true);
	}

	public void retry()
	{
		retry(false);
	}

	private void retry(boolean forced)
	{
		for (RemoteCategory category : RemoteCategory.values())
		{
			if (forced || failedCategories.contains(category))
			{
				fetch(category);
			}
		}
	}

	private void fetch(RemoteCategory category)
	{
		HttpUrl url = Objects.requireNonNull(HttpUrl.parse(BASE_URL)).newBuilder()
			.addPathSegment(developerMode ? BRANCH_DEV : BRANCH_PROD)
			.addPathSegment(category.jsonFile())
			.build();
		Request.Builder builder = new Request.Builder();
		// forcibly skip the cache in dev-mode
		if (developerMode)
		{
			builder.cacheControl(CacheControl.FORCE_NETWORK);
		}
		Request request = builder.url(url).build();
		okHttpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(@NotNull Call call, @NotNull IOException e)
			{
				onFail();
			}

			@Override
			public void onResponse(@NotNull Call call, @NotNull Response response)
			{
				ResponseBody body = response.body();
				if (body == null)
				{
					onFail();
				}
				else
				{
					successCategories.add(category);
					onSuccess(category, body);
					response.close();
					onReceiveDataListeners.forEach(b -> b.accept(category, true));
				}
			}

			private void onFail()
			{
				failedCategories.add(category);
				onReceiveDataListeners.forEach(b -> b.accept(category, false));
			}
		});
	}

	private void onSuccess(RemoteCategory category, ResponseBody body)
	{
		failedCategories.remove(category);
		Reader reader = new BufferedReader(new InputStreamReader(body.byteStream()));
		Type type = category.deserializedType();
		switch (category)
		{
			case SLOT:
				RemoteData.setItemInfo(gson.fromJson(reader, type));
				break;
			case COLOR:
				RemoteData.setColors(gson.fromJson(reader, type));
				break;
			case ANIMATION:
				RemoteData.setAnimations(gson.fromJson(reader, type));
				break;
			case MISC:
				RemoteData.setMiscData(gson.fromJson(reader, type));
				break;
		}
	}

	/**
	 * Adds a listener that will be called any time a request succeeds/fails for any category.
	 */
	public void addOnReceiveDataListener(BiConsumer<RemoteCategory, Boolean> listener)
	{
		onReceiveDataListeners.add(listener);
	}

	public void removeListeners()
	{
		onReceiveDataListeners.clear();
	}
}
