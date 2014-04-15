package com.circlegate.liban.task;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import com.circlegate.liban.task.TaskErrors.BaseError;
import com.circlegate.liban.task.TaskInterfaces.ITask;
import com.circlegate.liban.task.TaskInterfaces.ITaskContext;
import com.circlegate.liban.task.TaskInterfaces.ITaskExecutor;
import com.circlegate.liban.task.TaskInterfaces.ITaskParam;
import com.circlegate.liban.task.TaskInterfaces.ITaskProgressListener;
import com.circlegate.liban.task.TaskInterfaces.ITaskResult;
import com.circlegate.liban.task.TaskInterfaces.ITaskResultListener;
import com.circlegate.liban.utils.LogUtils;

public class TaskSerialExecutor implements ITaskExecutor {
    private static final String TAG = TaskSerialExecutor.class.getSimpleName();

    private final Queue<Task> tasks = new LinkedBlockingQueue<Task>();
    private final Handler handlerUi = new Handler(Looper.getMainLooper());
    private final ITaskContext context;
    private final Executor executor;
    private Task active;

    public TaskSerialExecutor(ITaskContext context, Executor executor) {
        this.context = context;
        this.executor = executor;
    }

    @Override
    public synchronized void executeTask(String id, ITaskParam param, Bundle bundle, boolean canCacheReferenceToParamResult, ITaskResultListener listener, ITaskProgressListener optProgressListener) {
        tasks.offer(new Task(id, param, bundle, canCacheReferenceToParamResult, listener, optProgressListener) {
            public void run() {
                if (headSameAsActive()) {
                    ITaskResult result = null;
                    LogUtils.i(TAG, "Executing task: " + getId());
                    try {
                        result = getParam().createResult(context, this);

                        try {
                            LogUtils.i(TAG, "Finished task: " + getId() + ", "
                                    + (result == null ? "result is null"
                                    : (result.isValidResult() ? "result is valid" : ("error: " + result.getError().getMsg(context).toString()))));
                        }
                        catch (Exception ex) {}
                    }
                    catch (Exception ex) {
                        LogUtils.e(TAG, "Error while processing task", ex);
                        result = getParam().createErrorResult(context, this, BaseError.ERR_UNKNOWN_ERROR);
                    }

                    if (headSameAsActive() && result != null) {
                        final ITaskResult resultFinal = result;
                        handlerUi.post(new Runnable() {
                            @Override
                            public void run() {
                                if (removeHeadIfSameAsActive()) {
                                    getListener().onTaskCompleted(getId(), resultFinal, getBundle());
                                }
                                scheduleNext();
                            }
                        });
                    }
                    else {
                        removeHeadIfSameAsActive();
                        scheduleNext();
                    }
                }
                else {
                    scheduleNext();
                }
            }
        });
        if (active == null) {
            scheduleNext();
        }
    }

    public int getTasksCount() {
        return tasks.size();
    }

    @Override
    public synchronized boolean containsTask(String id, ITaskResultListener listener) {
        return getTask(id, listener) != null;
    }

    @Override
    public boolean containsAnyTaskByResultHandler(ITaskResultListener listener) {
        for (Task t : tasks) {
            if (t.getListener().equals(listener)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public synchronized ITask getTask(String id, ITaskResultListener listener) {
        for (Task t : tasks) {
            if (t.getId().equals(id) && t.getListener().equals(listener)) {
                return t;
            }
        }
        return null;
    }

    @Override
    public synchronized boolean cancelTask(String id, ITaskResultListener listener) {
        for (Iterator<Task> iter = tasks.iterator(); iter.hasNext();) {
            Task t = iter.next();
            if (t.getId().equals(id) && t.getListener().equals(listener)) {
                t.setCanceled();
                iter.remove();
                return true;
            }
        }
        return false;
    }

    @Override
    public synchronized void cancelTasksByResultHandler(ITaskResultListener listener) {
        for (Iterator<Task> iter = tasks.iterator(); iter.hasNext();) {
            Task t = iter.next();
            if (t.getListener().equals(listener)) {
                t.setCanceled();
                iter.remove();
            }
        }
    }

    @Override
    public synchronized boolean addSkipCount(String id, ITaskResultListener listener, int skipCount) {
        for (Iterator<Task> iter = tasks.iterator(); iter.hasNext();) {
            Task t = iter.next();
            if (t.getId().equals(id) && t.getListener().equals(listener)) {
                t.addSkipCount(skipCount);
                return true;
            }
        }
        return false;
    }


    private synchronized boolean headSameAsActive() {
        return active != null && active == tasks.peek();
    }

    private synchronized boolean removeHeadIfSameAsActive() {
        if (headSameAsActive()) {
            tasks.remove();
            return true;
        }
        else
            return false;
    }

    private synchronized void scheduleNext() {
        if ((active = tasks.peek()) != null) {
            executor.execute(active);
        }
    }


    private abstract class Task implements ITask, Runnable {
        private final String id;
        private final ITaskParam param;
        private final Bundle bundle;
        private final boolean canCacheReferenceToParamResult;
        private final ITaskResultListener listener;
        private final ITaskProgressListener optProgressListener;

        private boolean canceled;
        private int skipCount;
        private int progress = ITaskProgressListener.INDETERMINATE_PROGRESS;
        private String progressState;
        private HashMap<String, Object> processObjects; // lazy loaded

        public Task(String id, ITaskParam param, Bundle bundle, boolean canCacheReferenceToParamResult, ITaskResultListener listener, ITaskProgressListener optProgressListener) {
            this.id = id;
            this.param = param;
            this.bundle = bundle;
            this.canCacheReferenceToParamResult = canCacheReferenceToParamResult;
            this.listener = listener;
            this.optProgressListener = optProgressListener;
        }

        public String getId() {
            return this.id;
        }

        public ITaskParam getParam() {
            return this.param;
        }

        public Bundle getBundle() {
            return this.bundle;
        }

        @Override
        public boolean canCacheReferenceToParamResult() {
            return canCacheReferenceToParamResult;
        }

        public ITaskResultListener getListener() {
            return this.listener;
        }

        @Override
        public Object putProcessObj(String key, Object obj) {
            if (processObjects == null)
                processObjects = new HashMap<>();
            return processObjects.put(key, obj);
        }

        @Override
        public <T> T getProcessObj(String key) {
            if (processObjects == null)
                return null;
            else
                return (T)processObjects.get(key);
        }

        @Override
        public void onTaskProgress(final int progress, final String progressState) {
            synchronized (this) {
                this.progress = progress;
                this.progressState = progressState;
            }

            if (optProgressListener != null) {
                handlerUi.post(new Runnable() {
                    @Override
                    public void run() {
                        if (headSameAsActive() && active == Task.this) {
                            optProgressListener.onTaskProgress(id, param, bundle, progress, progressState);
                        }
                    }
                });
            }
        }

        public synchronized boolean isCanceled() {
            return canceled;
        }

        private synchronized void setCanceled() {
            this.canceled = true;
        }

        @Override
        public synchronized int getSkipCount() {
            return this.skipCount;
        }

        private synchronized void addSkipCount(int skipCount) {
            this.skipCount += skipCount;
        }

        @Override
        public synchronized int getProgress() {
            return this.progress;
        }

        @Override
        public synchronized String getProgressState() {
            return this.progressState;
        }
    }


}