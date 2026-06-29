/*
BSD 2-Clause License

Copyright (c) 2022, Jake Barter
All rights reserved.

Copyright (c) 2022, pajlads

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this
   list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
   this list of conditions and the following disclaimer in the documentation
   and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package eq.uirs.fashionscape.util;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import lombok.SneakyThrows;

public class BlockingExecutor implements ScheduledExecutorService
{
	private final ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();

	@Override
	public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit)
	{
		return schedule(() -> {
			command.run();
			return null;
		}, delay, unit);
	}

	@Override
	@SneakyThrows
	public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit)
	{
		ScheduledFuture<V> fut = exec.schedule(callable, delay, unit);
		fut.get();
		return fut;
	}

	@Override
	public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void shutdown()
	{
		exec.shutdown();
	}

	@Override
	public List<Runnable> shutdownNow()
	{
		return exec.shutdownNow();
	}

	@Override
	public boolean isShutdown()
	{
		return exec.isShutdown();
	}

	@Override
	public boolean isTerminated()
	{
		return exec.isTerminated();
	}

	@Override
	public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException
	{
		return exec.awaitTermination(timeout, unit);
	}

	@Override
	@SneakyThrows
	public <T> Future<T> submit(Callable<T> task)
	{
		Future<T> fut = exec.submit(task);
		fut.get();
		return fut;
	}

	@Override
	public <T> Future<T> submit(Runnable task, T result)
	{
		return submit(() -> {
			task.run();
			return result;
		});
	}

	@Override
	public Future<?> submit(Runnable task)
	{
		return submit(task, null);
	}

	@Override
	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException
	{
		return exec.invokeAll(tasks);
	}

	@Override
	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException
	{
		return exec.invokeAll(tasks, timeout, unit);
	}

	@Override
	public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException
	{
		return exec.invokeAny(tasks);
	}

	@Override
	public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException
	{
		return exec.invokeAny(tasks);
	}

	@Override
	public void execute(Runnable command)
	{
		submit(command);
	}
}
