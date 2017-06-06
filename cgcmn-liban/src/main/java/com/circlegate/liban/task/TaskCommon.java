package com.circlegate.liban.task;

import android.os.Bundle;

import com.circlegate.liban.base.ApiBase.IApiParcelable;
import com.circlegate.liban.base.CustomCollections.LRUCache;
import com.circlegate.liban.task.TaskErrors.BaseError;
import com.circlegate.liban.task.TaskErrors.ITaskError;
import com.circlegate.liban.task.TaskErrors.TaskException;
import com.circlegate.liban.task.TaskInterfaces.ITask;
import com.circlegate.liban.task.TaskInterfaces.ITaskContext;
import com.circlegate.liban.task.TaskInterfaces.ITaskParam;
import com.circlegate.liban.task.TaskInterfaces.ITaskProgressListener;
import com.circlegate.liban.task.TaskInterfaces.ITaskResult;
import com.circlegate.liban.task.TaskInterfaces.ITaskResultListener;
import com.circlegate.liban.utils.EqualsUtils;
import com.circlegate.liban.utils.LogUtils;
import tinyguava.ImmutableList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Queue;

public class TaskCommon {
    public static class TaskSimple implements ITask, ITaskResultListener {
        private final ITaskParam param;
        private final Bundle bundle;

        private HashMap<String, Object> processObjects; // lazy loaded

        public TaskSimple(ITaskParam param, Bundle bundle) {
            this.param = param;
            this.bundle = bundle;
        }

        public ITaskParam getParam() {
            return this.param;
        }

        public Bundle getBundle() {
            return this.bundle;
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
        public String getId() {
            return "NO_ID";
        }

        @Override
        public boolean isCanceled() {
            return false;
        }

        @Override
        public int getSkipCount() {
            return 0;
        }

        @Override
        public int getProgress() {
            return ITaskProgressListener.INDETERMINATE_PROGRESS;
        }

        @Override
        public String getProgressState() {
            return null;
        }

        @Override
        public ITaskResultListener getListener() {
            return this;
        }

        @Override
        public boolean canCacheReferenceToParamResult() {
            return false;
        }

        @Override
        public void onTaskCompleted(String id, ITaskResult result, Bundle bundle) {
        }

        @Override
        public void onTaskProgress(int progress, String progressState) {
        }
    }


    public static class TaskCache {
        private final LRUCache<ITaskParam, ITaskResult> cache = new LRUCache<>(50);
        private final Queue<IApiParcelable> historyParams = new LinkedList<>();

        public Object getLock() {
            return this;
        }

        public synchronized ITaskResult get(ITaskParam param) {
            return cache.get(param);
        }

        public synchronized void putIfCan(ITask task, ITaskParam param, ITaskResult optResult) {
            // Pozor ma moznost vzniku memory leaku, pokud by treba param obsahoval referenci na aktivity/fragment apod!
            // Kazdy task explicitne musi mit urceno, jestli lze reference na Param/Result ukladat nebo ne...
            if (task.canCacheReferenceToParamResult()) {
                if (optResult != null && optResult.isCacheableResult()) {
                    cache.put(param, optResult);
                }

                if (param instanceof IApiParcelable) {
                    historyParams.add((IApiParcelable)param);

                    while (historyParams.size() > 50) {
                        historyParams.poll();
                    }
                }
            }
        }

        public synchronized void remove(ITaskParam param) {
            cache.remove(param);
        }

        public synchronized void clear() {
            cache.clear();
        }

        public synchronized List<Entry<ITaskParam, ITaskResult>> generateAll() {
            return cache.generateAll();
        }

        public synchronized List<IApiParcelable> generateHistoryParams() {
            return new ArrayList<>(historyParams);
        }
    }


    public static abstract class TaskParam implements ITaskParam {
        private static final String TAG = TaskParam.class.getSimpleName();

        @Override
        public boolean isExecutionInParallelForbidden(ITaskContext context) {
            return false;
        }

        @Override
        public ITaskResult createResult(ITaskContext context, ITask task) {
            TaskCache cache = context.getTaskCache();
            ITaskResult result = cache.get(this);

            if (result == null || !result.canUseCachedResultNow()) {
                try {
                    result = createResultUncached(context, task);
                }
                catch (TaskException ex) {
                    result = createErrorResult(context, task, ex.getTaskError());
                }
                catch (Exception ex) {
                    LogUtils.e(getClass().getSimpleName(), "TaskParam.createResult: createResultUncached thrown an exception", ex);
                    result = createErrorResult(context, task, BaseError.ERR_UNKNOWN_ERROR);
                }
            }
            else {
                LogUtils.d(TAG, "Result taken from cache");
            }

            cache.putIfCan(task, this, result);
            return result;
        }

        public abstract ITaskResult createResultUncached(ITaskContext context, ITask task) throws TaskException;
    }

    public static class TaskResultBase<TParam extends ITaskParam, TError extends ITaskError> implements ITaskResult {
        private final TParam param;
        private final TError error;

        public TaskResultBase(TParam param, TError error) {
            this.param = param;
            this.error = error;
        }

        public TParam getParam() {
            return this.param;
        }

        public TError getError() {
            return this.error;
        }


        @Override
        public final boolean isValidResult() {
            return error.isOk();
        }

        @Override
        public boolean isCacheableResult() {
            return false; // defaultni hodnota je false
        }

        @Override
        public boolean canUseCachedResultNow() {
            return true;
        }
    }

    public static class TaskResult<TParam extends ITaskParam> extends TaskResultBase<TParam, ITaskError> {
        public TaskResult(TParam param, ITaskError error) {
            super(param, error);
        }
    }


    public static class BatchTaskParam extends TaskParam {
        private final ImmutableList<ITaskParam> params; // abstract

        public BatchTaskParam(ImmutableList<ITaskParam> params) {
            this.params = params;
        }

        public ImmutableList<ITaskParam> getParams() {
            return this.params;
        }

        private int _hash = EqualsUtils.HASHCODE_INVALID;

        @Override
        public int hashCode() {
            if (_hash == EqualsUtils.HASHCODE_INVALID) {
                int _hash = 17;
                _hash = _hash * 29 + EqualsUtils.itemsHashCode(params);
                this._hash = _hash == EqualsUtils.HASHCODE_INVALID ? EqualsUtils.HASHCODE_INVALID_REPLACEMENT : _hash;
            }
            return this._hash;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if (!(o instanceof BatchTaskParam)) {
                return false;
            }

            BatchTaskParam lhs = (BatchTaskParam) o;
            return lhs != null &&
                    EqualsUtils.itemsEqual(params, lhs.params);
        }

        @Override
        public String getSerialExecutionKey(ITaskContext context) {
            if (params.size() > 0) {
                String ret = params.get(0).getSerialExecutionKey(context);
                for (int i = 1; i < params.size(); i++) {
                    if (!EqualsUtils.equalsCheckNull(ret, params.get(i).getSerialExecutionKey(context))) {
                        throw new RuntimeException("getSerialExecutionKeys are different!");
                    }
                }
                return ret;
            }
            else
                return DEFAULT_SERIAL_EXECUTION_KEY;
        }

        @Override
        public boolean isExecutionInParallelForbidden(ITaskContext context) {
            if (params.size() > 0) {
                boolean ret = params.get(0).isExecutionInParallelForbidden(context);

                for (int i = 1; i < params.size(); i++) {
                    if (ret != params.get(i).isExecutionInParallelForbidden(context)) {
                        throw new RuntimeException("isExecutionInParallelForbidden results are different!");
                    }
                }
                return ret;
            }
            else
                return false;
        }

        @Override
        public BatchTaskResult createResultUncached(ITaskContext context, ITask task) {
            ImmutableList.Builder<ITaskResult> results = ImmutableList.builder();
            for (ITaskParam p : params) {
                results.add(p.createResult(context, task));
            }
            return new BatchTaskResult(this, BaseError.ERR_OK, results.build());
        }

        @Override
        public BatchTaskResult createErrorResult(ITaskContext context, ITask task, ITaskError error) {
            return new BatchTaskResult(this, error, null);
        }
    }

    public static class BatchTaskResult extends TaskResult<BatchTaskParam> {
        private final ImmutableList<ITaskResult> results; // abstract optional - pokud null -> error result

        public BatchTaskResult(BatchTaskParam param, ITaskError error, ImmutableList<ITaskResult> results) {
            super(param, error);
            this.results = results;
        }

        public ImmutableList<ITaskResult> getResults() {
            return this.results;
        }
    }


    public static abstract class AsyncTaskParam extends TaskParam {
        private final String serialExecutionKey;

        public AsyncTaskParam(String serialExecutionKey) {
            this.serialExecutionKey = serialExecutionKey;
        }

        @Override
        public String getSerialExecutionKey(ITaskContext context) {
            return serialExecutionKey;
        }

        @Override
        public ITaskResult createErrorResult(ITaskContext context, ITask task, ITaskError error) {
            return new TaskResult<>(this, error);
        }

        @Override
        public ITaskResult createResultUncached(ITaskContext context, ITask task) throws TaskException {
            execute(context, task);
            return new TaskResult<>(this, BaseError.ERR_OK);
        }

        public abstract void execute(ITaskContext context, ITask task) throws TaskException;
    }


    public static class EmptyTaskListener implements ITaskResultListener, ITaskProgressListener {
        @Override
        public void onTaskProgress(String id, ITaskParam param, Bundle bundle, int progress, String progressState) {
        }

        @Override
        public void onTaskCompleted(String id, ITaskResult result, Bundle bundle) {
        }
    }
}
