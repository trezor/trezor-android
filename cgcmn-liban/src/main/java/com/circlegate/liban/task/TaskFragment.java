package com.circlegate.liban.task;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;

import com.circlegate.liban.base.GlobalContextLib;
import com.circlegate.liban.fragment.BaseRetainFragment;
import com.circlegate.liban.task.TaskInterfaces.ITask;
import com.circlegate.liban.task.TaskInterfaces.ITaskExecutor;
import com.circlegate.liban.task.TaskInterfaces.ITaskParam;
import com.circlegate.liban.task.TaskInterfaces.ITaskProgressListener;
import com.circlegate.liban.task.TaskInterfaces.ITaskResult;
import com.circlegate.liban.task.TaskInterfaces.ITaskResultListener;
import com.circlegate.liban.utils.EqualsUtils;
import com.circlegate.liban.utils.FragmentUtils;

public class TaskFragment extends BaseRetainFragment {
    private static final String FRAGMENT_TAG = "TaskFragment";

    private ITaskExecutor executor;
    private final ArrayList<PendingTask> pendingPausedNewTasks = new ArrayList<PendingTask>();
    private final ArrayList<PendingTask> pendingCompletedTasks = new ArrayList<PendingTask>();
    private final ArrayList<ProgressTask> pendingProgressTasks = new ArrayList<ProgressTask>();
    private boolean readyToCompleteTasks = false;
    private boolean pauseNewTasks = false;


    public static <T extends FragmentActivity & ITaskFragmentActivity> TaskFragment getInstance(T activity) {
        FragmentManager fm = activity.getSupportFragmentManager();
        TaskFragment f = (TaskFragment)fm.findFragmentByTag(FRAGMENT_TAG);
        if (f == null) {
            f = new TaskFragment();
            setupNewFragment(f, activity);
            fm.beginTransaction().add(f, FRAGMENT_TAG).commit();
        }
        return f;
    }

    protected static void setupNewFragment(TaskFragment f, Activity activity) {
        f.executor = GlobalContextLib.get().getTaskExecutor();
    }


    //
    // Lifecycle methods
    //

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.executor = GlobalContextLib.get().getTaskExecutor();
    }

    @Override
    public void onResume() {
        super.onResume();
        this.readyToCompleteTasks = true;

        // musim udelat tak, aby tesne pred spustenim onTaskProgress byl task z pendingProgressTasks odstranen
        while (!this.pendingProgressTasks.isEmpty()) {
            ProgressTask t = this.pendingProgressTasks.get(0);
            this.pendingProgressTasks.remove(0);
            onTaskProgress(t.getId(), t.getParam(), t.getBundle(), t.getProgress(), t.getProgressState(), t.getFragmentTag());
        }

        finishPendingCompletedTasksIfCan();
    }

    @Override
    public void onPause() {
        this.readyToCompleteTasks = false;
        super.onPause();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        this.readyToCompleteTasks = false;
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.executor.cancelTasksByResultHandler(new TaskResultListener(this, true, null));
    }


    //
    // Public methods
    //

    public boolean isPauseNewTasks() {
        return this.pauseNewTasks;
    }

    public void setPauseNewTasks(boolean pauseNewTasks) {
        if (this.pauseNewTasks != pauseNewTasks) {
            this.pauseNewTasks = pauseNewTasks;

            if (!pauseNewTasks) {
                PendingTask[] tmp = this.pendingPausedNewTasks.toArray(new PendingTask[this.pendingPausedNewTasks.size()]);
                this.pendingPausedNewTasks.clear();

                for (PendingTask p : tmp) {
                    executeTask(p.getId(), p.getParam(), p.getBundle(), p.canCacheReferenceToParamResult(), p.getFragmentTag());
                }
            }
        }
    }

    public void executeTask(String id, ITaskParam param, Bundle bundle, boolean canCacheReferenceToParamResult, String fragmentTag) {
        if (pauseNewTasks) {
            pendingPausedNewTasks.add(new PendingTask(this, id, param, bundle, canCacheReferenceToParamResult, fragmentTag, null));
        }
        else {
            TaskResultListener t = new TaskResultListener(this, false, fragmentTag);
            this.executor.executeTask(id, param, bundle, canCacheReferenceToParamResult, t, t);
        }
    }

    public boolean containsTask(String id, String fragmentTag) {
        return getTask(id, fragmentTag) != null;
    }

    public boolean containsAnyTaskByFragmentTag(String fragmentTag) {
        if (getPendingTaskIndByFragmentTag(pendingPausedNewTasks, fragmentTag) >= 0
                || this.executor.containsAnyTaskByResultHandler(new TaskResultListener(this, false, fragmentTag))
                || getPendingTaskIndByFragmentTag(pendingCompletedTasks, fragmentTag) >= 0)
            return true;
        else
            return false;
    }

    public ITask getTask(String id, String fragmentTag) {
        int pausedNewTaskInd = getPendingTaskInd(pendingPausedNewTasks, id, fragmentTag);
        if (pausedNewTaskInd >= 0) {
            return pendingPausedNewTasks.get(pausedNewTaskInd);
        }

        ITask ret = this.executor.getTask(id, new TaskResultListener(this, false, fragmentTag));
        if (ret != null) {
            return ret;
        }

        int completedTaskInd = getPendingTaskInd(pendingCompletedTasks, id, fragmentTag);
        if (completedTaskInd >= 0) {
            return this.pendingCompletedTasks.get(completedTaskInd);
        }
        else
            return  null;
    }

    public boolean cancelTask(String id, String fragmentTag) {
        int pausedNewTaskInd = getPendingTaskInd(pendingPausedNewTasks, id, fragmentTag);
        if (pausedNewTaskInd >= 0) {
            this.pendingPausedNewTasks.remove(pausedNewTaskInd);
            return true;
        }

        // Chci mit opravdu jistotu, ze ruseny task uz nezavola ani onTaskProgress
        for (int i = 0; i < this.pendingProgressTasks.size(); i++) {
            ProgressTask p = this.pendingProgressTasks.get(i);

            if (EqualsUtils.equalsCheckNull(p.getId(), id) && EqualsUtils.equalsCheckNull(p.getFragmentTag(), fragmentTag)) {
                pendingProgressTasks.remove(i);
                i--;
            }
        }

        boolean ret = this.executor.cancelTask(id, new TaskResultListener(this, false, fragmentTag));
        if (ret)
            return ret;

        int completedTaskInd = getPendingTaskInd(pendingCompletedTasks, id, fragmentTag);
        if (completedTaskInd >= 0) {
            this.pendingCompletedTasks.remove(completedTaskInd);
            return true;
        }
        else
            return false;
    }

    public void cancelTasksByFragmentId(String fragmentTag) {
        this.executor.cancelTasksByResultHandler(new TaskResultListener(this, false, fragmentTag));
        removePendingTasksByFragmentId(pendingPausedNewTasks, fragmentTag);
        removePendingTasksByFragmentId(pendingProgressTasks, fragmentTag);
        removePendingTasksByFragmentId(pendingCompletedTasks, fragmentTag);
    }

    public boolean addSkipCount(String id, String fragmentTag, int skipCount) {
        return this.executor.addSkipCount(id, new TaskResultListener(this, false, fragmentTag), skipCount);
    }

    // POZOR Pri pripadnem pridavani dalsich public metod myslet na WearableTaskFragment!!


    //
    // Callbacks
    //

    protected void onPreTaskProgress(String id, ITaskParam param, Bundle bundle, int progress, String progressState, String fragmentTag) {
        if (!this.readyToCompleteTasks)
            pendingProgressTasks.add(new ProgressTask(id, param, bundle, progress, progressState, fragmentTag));
        else
            onTaskProgress(id, param, bundle, progress, progressState, fragmentTag);
    }

    protected void onTaskProgress(String id, ITaskParam param, Bundle bundle, int progress, String progressState, String fragmentTag) {
        final Object listener;
        if (fragmentTag == null) {
            listener = getActivity();
        }
        else {
            listener = findTargetFragment(fragmentTag);
        }

        if (listener instanceof ITaskProgressListener) {
            ((ITaskProgressListener)listener).onTaskProgress(id, param, bundle, progress, progressState);
        }
    }


    protected void onPreTaskCompleted(String id, ITaskResult result, Bundle bundle, String fragmentTag) {
        if (!this.readyToCompleteTasks) {
            addPendingCompletedTask(id, result, bundle, false, fragmentTag);
        }
        else {
            onTaskCompleted(id, result, bundle, fragmentTag);
        }
    }

    protected void onTaskCompleted(String id, ITaskResult result, Bundle bundle, String fragmentTag) {
        final ITaskResultListener listener;
        if (fragmentTag == null) {
            listener = (ITaskResultListener)getActivity();
        }
        else {
            listener = findTargetFragment(fragmentTag);
        }

        if (listener != null) {
            listener.onTaskCompleted(id, result, bundle);
        }
    }

    protected void addPendingCompletedTask(String id, ITaskResult result, Bundle bundle, boolean canCacheReferenceToParamResult, String fragmentTag) {
        pendingCompletedTasks.add(new PendingTask(this, id, result.getParam(), bundle, canCacheReferenceToParamResult, fragmentTag, result));
    }

    protected void finishPendingCompletedTasksIfCan() {
        if (readyToCompleteTasks) {
            // musim udelat tak, aby tesne pred spustenim onTaskCompleted byl task z pendindCompletedTasks odstranen
            while (!this.pendingCompletedTasks.isEmpty()) {
                PendingTask t = this.pendingCompletedTasks.get(0);
                this.pendingCompletedTasks.remove(0);
                onTaskCompleted(t.getId(), t.getResult(), t.getBundle(), t.getFragmentTag());
            }
        }
    }


    //
    // PRIVATE
    //

    private ITaskResultListener findTargetFragment(String fragmentTag) {
        return (ITaskResultListener)FragmentUtils.findFragmentByNestedTag(getActivity(), fragmentTag);
    }

    protected static int getPendingTaskInd(ArrayList<PendingTask> tasks, String id, String fragmentTag) {
        for (int i = 0; i < tasks.size(); i++) {
            PendingTask p = tasks.get(i);
            if (EqualsUtils.equalsCheckNull(p.getId(), id) && EqualsUtils.equalsCheckNull(p.getFragmentTag(), fragmentTag)) {
                return i;
            }
        }
        return -1;
    }

    protected static int getPendingTaskIndByFragmentTag(ArrayList<PendingTask> tasks, String fragmentTag) {
        for (int i = 0; i < tasks.size(); i++) {
            PendingTask p = tasks.get(i);
            if (EqualsUtils.equalsCheckNull(p.getFragmentTag(), fragmentTag)) {
                return i;
            }
        }
        return -1;
    }

    protected static void removePendingTasksByFragmentId(ArrayList<? extends IHasFragmentTag> tasks, String fragmentTag) {
        for (int i = 0; i < tasks.size(); i++) {
            IHasFragmentTag p = tasks.get(i);
            if (EqualsUtils.equalsCheckNull(p.getFragmentTag(), fragmentTag)) {
                tasks.remove(i);
                i--;
            }
        }
    }


    //
    // INNER CLASSES
    //

    private static class TaskResultListener implements ITaskResultListener, ITaskProgressListener {
        private final TaskFragment taskFragment;
        private final boolean ignoreFragmentTagForEquals;
        private final String fragmentTag;

        private TaskResultListener(TaskFragment taskFragment, boolean ignoreFragmentTagForEquals, String fragmentTag) {
            this.taskFragment = taskFragment;
            this.ignoreFragmentTagForEquals = ignoreFragmentTagForEquals;
            this.fragmentTag = fragmentTag;
        }

        @Override
        public void onTaskCompleted(String id, ITaskResult result, Bundle bundle) {
            this.taskFragment.onPreTaskCompleted(id, result, bundle, this.fragmentTag);
        }

        @Override
        public void onTaskProgress(String id, ITaskParam param, Bundle bundle, int progress, String progressState) {
            this.taskFragment.onPreTaskProgress(id, param, bundle, progress, progressState, fragmentTag);
        }


        @Override
        public int hashCode() {
            // Nemuzu brat v potaz fragmentTag kvuli ignoreFragmentTagForEquals
            return taskFragment.hashCode();
        }

        /**
         * Pokud je ignoreFragmentTagForEquals jednoho z obou objektu = true, tak se nebere v potaz fragmentTag
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if (!(o instanceof TaskResultListener)) {
                return false;
            }

            TaskResultListener lhs = (TaskResultListener) o;
            return lhs != null &&
                    taskFragment.equals(lhs.taskFragment) &&
                    (ignoreFragmentTagForEquals || lhs.ignoreFragmentTagForEquals || (EqualsUtils.equalsCheckNull(fragmentTag, lhs.fragmentTag)));
        }
    }


    protected interface IHasFragmentTag {
        String getFragmentTag();
    }

    protected static class PendingTask implements IHasFragmentTag, ITask, ITaskResultListener, ITaskProgressListener {
        private final TaskFragment taskFragment;
        private final String id;
        private final ITaskParam param;
        private final Bundle bundle;
        private final boolean canCacheReferenceToParamResult;
        private final String fragmentTag;
        private final ITaskResult result; // optional - jenom u uz hotovych tasku...
        private final long timeStamp;

        private HashMap<String, Object> processObjects; // lazy loaded

        // Upraveno!
        public PendingTask(TaskFragment taskFragment, String id, ITaskParam param, Bundle bundle, boolean canCacheReferenceToParamResult, String fragmentTag, ITaskResult result) {
            this.taskFragment = taskFragment;
            this.id = id;
            this.param = param;
            this.bundle = bundle;
            this.canCacheReferenceToParamResult = canCacheReferenceToParamResult;
            this.fragmentTag = fragmentTag;
            this.result = result;

            this.timeStamp = SystemClock.elapsedRealtime();
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

        public String getFragmentTag() {
            return this.fragmentTag;
        }

        public ITaskResult getResult() {
            return this.result;
        }

        public long getTimeStamp() {
            return this.timeStamp;
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
            return 0;
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
            return canCacheReferenceToParamResult;
        }

        @Override
        public void onTaskProgress(int progress, String progressState) {
            onTaskProgress(id, param, bundle, progress, progressState);
        }

        @Override
        public void onTaskCompleted(String id, ITaskResult result, Bundle bundle) {
            this.taskFragment.onPreTaskCompleted(id, result, bundle, this.fragmentTag);
        }

        @Override
        public void onTaskProgress(String id, ITaskParam param, Bundle bundle, int progress, String progressState) {
            this.taskFragment.onPreTaskProgress(id, param, bundle, progress, progressState, fragmentTag);
        }
    }

    private static class ProgressTask implements IHasFragmentTag {
        private final String id;
        private final ITaskParam param;
        private final Bundle bundle;
        private final int progress;
        private final String progressState;
        private final String fragmentTag;

        public ProgressTask(String id, ITaskParam param, Bundle bundle, int progress, String progressState, String fragmentTag) {
            this.id = id;
            this.param = param;
            this.bundle = bundle;
            this.progress = progress;
            this.progressState = progressState;
            this.fragmentTag = fragmentTag;
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

        public int getProgress() {
            return this.progress;
        }

        public String getProgressState() {
            return this.progressState;
        }

        public String getFragmentTag() {
            return this.fragmentTag;
        }
    }


    public interface ITaskFragmentActivity {
        TaskFragment getTaskFragment();
    }
}