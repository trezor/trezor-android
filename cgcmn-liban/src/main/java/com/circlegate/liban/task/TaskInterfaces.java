package com.circlegate.liban.task;

import com.circlegate.liban.base.CommonClasses.IGlobalContext;
import com.circlegate.liban.task.TaskCommon.TaskCache;
import com.circlegate.liban.task.TaskErrors.ITaskError;

import android.os.Bundle;

public class TaskInterfaces {
    public interface ITaskContext extends IGlobalContext {
        ITaskExecutor getTaskExecutor();
        TaskCache getTaskCache();
        String getCurrentLangAbbrev();
        String getCurrentCountryAbbrev();
    }

    public interface ITaskParam {
        static String DEFAULT_SERIAL_EXECUTION_KEY = "";

//	    static int TASK_CLASS_ID_CPP = 1;
//	    static int TASK_CLASS_ID_WS_GOOGLE = 2;
//	    static int TASK_CLASS_ID_WS_CIRCLEGATE = 3;
//	    static int TASK_CLASS_ID_WS_CIRCLEGATE_DWN = 4;
//	    static int TASK_CLASS_ID_GOOGLE_BILLING = 5;
//        static int TASK_CLASS_ID_CD_IPWS = 6;
//        static int TASK_CLASS_ID_CD_CMN = 7;

        String getSerialExecutionKey(ITaskContext context);
        boolean isExecutionInParallelForbidden(ITaskContext context);
        ITaskResult createResult(ITaskContext context, ITask task);
        ITaskResult createErrorResult(ITaskContext context, ITask task, ITaskError error);
    }

    public interface ITaskResult {
        ITaskParam getParam();
        boolean isValidResult();
        boolean isCacheableResult();
        boolean canUseCachedResultNow();
        ITaskError getError();
    }

    public interface ITaskResultListener {
        void onTaskCompleted(String id, ITaskResult result, Bundle bundle);
    }

    public interface ITaskProgressListener {
        static int MAX_PROGRESS = 10000;
        static int INDETERMINATE_PROGRESS = -2;

        /**
         * @param progress - default -1, jinak hodnoty 0 - MAX_PROGRESS
         */
        void onTaskProgress(String id, ITaskParam param, Bundle bundle, int progress, String progressState);
    }

    public interface ITask  {
        public static String PROCESS_BUNDLE_FILE_SIZE = "PROCESS_BUNDLE_FILE_SIZE"; // Pouzito pri stahovani...

        String getId();
        ITaskParam getParam();
        Bundle getBundle();
        Object putProcessObj(String key, Object obj); // vraci pripadne puvodni hodnotu
        <T> T getProcessObj(String key);
        boolean isCanceled();
        int getSkipCount();
        int getProgress();
        String getProgressState();
        ITaskResultListener getListener();
        boolean canCacheReferenceToParamResult(); // povolit jenom v pripade, kdy nemuze dochazek k memory leakum! (reference na aktivity/fragmenty)
        void onTaskProgress(int progress, String progressState);
    }

    public interface ITaskExecutor {
        void executeTask(String id, ITaskParam param, Bundle bundle, boolean canCacheReferenceToParamResult, ITaskResultListener listener, ITaskProgressListener optProgressListener);
        boolean containsTask(String id, ITaskResultListener listener);
        boolean containsAnyTaskByResultHandler(ITaskResultListener listener);
        ITask getTask(String id, ITaskResultListener listener);
        boolean cancelTask(String id, ITaskResultListener listener);
        void cancelTasksByResultHandler(ITaskResultListener listener);
        boolean addSkipCount(String id, ITaskResultListener listener, int skipCount);
    }
}
