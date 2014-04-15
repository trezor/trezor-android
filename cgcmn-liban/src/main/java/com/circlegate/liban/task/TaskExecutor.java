package com.circlegate.liban.task;

import android.os.Bundle;

import com.circlegate.liban.task.TaskInterfaces.ITask;
import com.circlegate.liban.task.TaskInterfaces.ITaskContext;
import com.circlegate.liban.task.TaskInterfaces.ITaskExecutor;
import com.circlegate.liban.task.TaskInterfaces.ITaskParam;
import com.circlegate.liban.task.TaskInterfaces.ITaskProgressListener;
import com.circlegate.liban.task.TaskInterfaces.ITaskResultListener;
import com.circlegate.liban.utils.EqualsUtils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class TaskExecutor implements ITaskExecutor {
	private static final int CORE_POOL_SIZE = 5;
	private static final int MAXIMUM_POOL_SIZE = 128;
	private static final int KEEP_ALIVE = 1;

	private final ThreadFactory threadFactory = new ThreadFactory() {
		private final AtomicInteger mCount = new AtomicInteger(1);

		public Thread newThread(Runnable r) {
			return new Thread(r, "Thread #" + mCount.getAndIncrement());
		}
	};

	private final BlockingQueue<Runnable> poolWorkQueue = new LinkedBlockingQueue<>(10);

	public final Executor threadPoolExecutor = new ThreadPoolExecutor(
			CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE, TimeUnit.SECONDS,
			poolWorkQueue, threadFactory);

	private final HashMap<SerialExecutorKey, TaskSerialExecutor> serialExecutors = new HashMap<>();
	private final ITaskContext context;

	public TaskExecutor(ITaskContext context) {
		this.context = context;
	}

	@Override
	public void executeTask(String id, ITaskParam param, Bundle bundle, boolean canCacheReferenceToParamResult, ITaskResultListener listener, ITaskProgressListener optProgressListener) {
        SerialExecutorKey key = new SerialExecutorKey(param.getSerialExecutionKey(context), param.isExecutionInParallelForbidden(context) ? null : listener);
        TaskSerialExecutor executor = serialExecutors.get(key);

		if (executor == null) {
			executor = new TaskSerialExecutor(this.context, this.threadPoolExecutor);
            serialExecutors.put(key, executor);
		}
		executor.executeTask(id, param, bundle, canCacheReferenceToParamResult, listener, optProgressListener);
	}

	@Override
	public boolean containsTask(String id, ITaskResultListener listener) {
		return getTask(id, listener) != null;
	}

    @Override
    public boolean containsAnyTaskByResultHandler(ITaskResultListener listener) {
        for (TaskSerialExecutor s : serialExecutors.values()) {
            if (s.containsAnyTaskByResultHandler(listener))
                return true;
        }
        return false;
    }

    @Override
	public ITask getTask(String id, ITaskResultListener listener) {
        for (TaskSerialExecutor s : serialExecutors.values()) {
            ITask task = s.getTask(id, listener);
            if (task != null)
                return task;
        }
        return null;
	}

	@Override
	public boolean cancelTask(String id, ITaskResultListener listener) {
        for (TaskSerialExecutor s : serialExecutors.values()) {
            if (s.cancelTask(id, listener))
                return true;
        }
        return false;
	}

	@Override
	public void cancelTasksByResultHandler(ITaskResultListener listener) {
        for (TaskSerialExecutor s : serialExecutors.values()) {
            s.cancelTasksByResultHandler(listener);
        }

        for (Iterator<Entry<SerialExecutorKey, TaskSerialExecutor>> iter = serialExecutors.entrySet().iterator(); iter.hasNext();) {
            TaskSerialExecutor s = iter.next().getValue();

            s.cancelTasksByResultHandler(listener);
            if (s.getTasksCount() == 0) {
                iter.remove();
            }
        }
	}

    @Override
    public boolean addSkipCount(String id, ITaskResultListener listener, int skipCount) {
        for (TaskSerialExecutor s : serialExecutors.values()) {
            if (s.addSkipCount(id, listener, skipCount))
                return true;
        }
        return false;
    }


    private static class SerialExecutorKey {
        public final String serialExecutionKey;
        public final ITaskResultListener optListener; // muze byt null - pokud dany task ma byt spousten seriove pres vsechny listenery

        public SerialExecutorKey(String serialExecutionKey, ITaskResultListener optListener) {
            this.serialExecutionKey = serialExecutionKey;
            this.optListener = optListener;
        }

        @Override
        public int hashCode() {
            int _hash = 17;
            _hash = _hash * 29 + EqualsUtils.hashCodeCheckNull(serialExecutionKey);
            _hash = _hash * 29 + EqualsUtils.hashCodeCheckNull(optListener);
            return _hash;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if (!(o instanceof SerialExecutorKey)) {
                return false;
            }

            SerialExecutorKey lhs = (SerialExecutorKey) o;
            return lhs != null &&
                    EqualsUtils.equalsCheckNull(serialExecutionKey, lhs.serialExecutionKey) &&
                    EqualsUtils.equalsCheckNull(optListener, lhs.optListener);
        }
    }
}
