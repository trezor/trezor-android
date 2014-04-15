package com.circlegate.liban.ws;

import android.os.SystemClock;
import android.text.TextUtils;

import com.circlegate.liban.task.TaskCommon.TaskParam;
import com.circlegate.liban.task.TaskCommon.TaskResult;
import com.circlegate.liban.task.TaskErrors.BaseError;
import com.circlegate.liban.task.TaskErrors.ITaskError;
import com.circlegate.liban.task.TaskErrors.TaskException;
import com.circlegate.liban.task.TaskInterfaces.ITask;
import com.circlegate.liban.task.TaskInterfaces.ITaskContext;
import com.circlegate.liban.task.TaskInterfaces.ITaskParam;
import com.circlegate.liban.task.TaskInterfaces.ITaskResult;
import com.circlegate.liban.utils.EqualsUtils;
import com.circlegate.liban.utils.LogUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class WsBase {
    public interface IWsParam extends ITaskParam {
        IWsResult createResultUncached(ITaskContext context, ITask task) throws TaskException;
        IWsResult createErrorResult(ITaskContext context, ITask task, ITaskError error);
    }

    public interface IWsResult extends ITaskResult {
        IWsParam getParam();
        boolean isConnectionError();
    }


    public static abstract class WsParam extends TaskParam implements IWsParam {
        protected static final int DEF_TIMEOUT_CONNECTION = 30000;
        protected static final int DEF_TIMEOUT_WRITE = 30000;
        protected static final int DEF_TIMEOUT_READ = 30000;

        private static OkHttpClient defaultClient;

        private String tag; // lazy loaded


        @Override
        public IWsResult createResultUncached(ITaskContext context, ITask task) throws TaskException {
            String tag = getTag();
            long startTime = SystemClock.elapsedRealtime();

            if (task.isCanceled()) {
                LogUtils.d(tag, "task canceled (1)");
                return null;
            }

            Request request = createRequest(context, task).build();
            Response response = null;

            try {
                int maxRetries = Math.max(1, getRetries(context, task));

                for (int i = 0; i < maxRetries; i++) {
                    LogUtils.d(tag, "retry " + i);

                    if (task.isCanceled()) {
                        LogUtils.d(tag, "task canceled (2)");
                        return null;
                    }

                    try {
                        OkHttpClient client = getClient(context, task, i);
                        response = client.newCall(request).execute();
                        int statusCode = response.code();
                        LogUtils.d(tag, "status code: " + statusCode);

                        if (!isResponseCodeAcceptable(statusCode)) {
                            return createErrorResult(context, task, BaseError.ERR_CONNECTION_ERROR_UNEXPECTED_RES);
                        }
                        break;
                    }
                    catch (IOException ex) {
                        LogUtils.e(tag, "createResultUncached: exception while connecting - retry: " + i, ex);
                    }

                    if (i + 1 >= maxRetries) {
                        LogUtils.d(tag, "too many retries (" + i + "), giving up");
                        return createErrorResult(context, task, BaseError.ERR_CONNECTION_ERROR_COMMUNICATION);
                    }
                }

                try {
                    IWsResult result = createResult(context, task, response);
                    LogUtils.d(tag, "finished in time: " + (SystemClock.elapsedRealtime() - startTime));
                    return result;
                }
                catch (IOException ex) {
                    LogUtils.e(tag, "error while reading response", ex);
                    if (!TextUtils.isEmpty(ex.getMessage()) && ex.getMessage().contains(" ENOSPC ")) // pro zachceni vyjimky pri nedostatku mista v ulozisti: java.io.IOException: write failed: ENOSPC (No space left on device)
                        return createErrorResult(context, task, BaseError.ERR_FILE_ERROR);
                    else
                        return createErrorResult(context, task, BaseError.ERR_CONNECTION_ERROR_COMMUNICATION);
                }
            }
            finally {
                try {
                    if (response != null && response.body() != null)
                        response.body().close();
                }
                catch (Exception ex) {
                    LogUtils.e(tag, "response.body().close() thrown exception");
                }
            }
        }


        protected OkHttpClient getClient(ITaskContext context, ITask task, int retry) {
            synchronized (WsParam.class) {
                if (defaultClient == null) {
                    defaultClient = WsUtils.createClientBuilder(DEF_TIMEOUT_CONNECTION, DEF_TIMEOUT_WRITE, DEF_TIMEOUT_READ).build();
                }
                return defaultClient;
            }
        }

        protected boolean isResponseCodeAcceptable(int responseCode) {
            return responseCode == HttpURLConnection.HTTP_OK;
        }

        protected abstract int getRetries(ITaskContext context, ITask task);
        protected abstract Request.Builder createRequest(ITaskContext context, ITask task) throws TaskException;
        protected abstract IWsResult createResult(ITaskContext context, ITask task, Response acceptableResponse) throws TaskException, IOException;

        protected String getTag() {
            if (tag == null)
                tag = "WsParam-" + getClass().getSimpleName();
            return tag;
        }
    }

    public static class WsResult<TWsParam extends IWsParam> extends TaskResult<TWsParam> implements IWsResult {
        public WsResult(TWsParam param, ITaskError error) {
            super(param, error);
        }

        @Override
        public final boolean isConnectionError() {
            return BaseError.isConnectionError(getError());
        }
    }


    public static class WsFileParam extends WsParam {
        private final String uri;
        private final String fileDest;
        private final String serialExecutionKey;
        private final boolean canUseGzip;

        public WsFileParam(String uri, String fileDest, String serialExecutionKey, boolean canUseGzip) {
            this.uri = uri;
            this.fileDest = fileDest;
            this.serialExecutionKey = serialExecutionKey;
            this.canUseGzip = canUseGzip;
        }

        public String getUri() {
            return this.uri;
        }

        public String getFileDest() {
            return this.fileDest;
        }

        public String getSerialExecutionKey() {
            return this.serialExecutionKey;
        }

        public boolean getCanUseGzip() {
            return this.canUseGzip;
        }


        @Override
        public String getSerialExecutionKey(ITaskContext context) {
            return serialExecutionKey;
        }

        @Override
        public int hashCode() {
            int _hash = 17;
            _hash = _hash * 29 + EqualsUtils.hashCodeCheckNull(uri);
            _hash = _hash * 29 + EqualsUtils.hashCodeCheckNull(fileDest);
            _hash = _hash * 29 + EqualsUtils.hashCodeCheckNull(serialExecutionKey);
            _hash = _hash * 29 + (canUseGzip ? 1 : 0);
            return _hash;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if (!(o instanceof WsFileParam)) {
                return false;
            }

            WsFileParam lhs = (WsFileParam) o;
            return lhs != null &&
                    EqualsUtils.equalsCheckNull(uri, lhs.uri) &&
                    EqualsUtils.equalsCheckNull(fileDest, lhs.fileDest) &&
                    EqualsUtils.equalsCheckNull(serialExecutionKey, lhs.serialExecutionKey) &&
                    canUseGzip == lhs.canUseGzip;
        }


        @Override
        protected int getRetries(ITaskContext context, ITask task) {
            return 2;
        }

        @Override
        protected Request.Builder createRequest(ITaskContext context, ITask task) {
            Request.Builder ret = new Request.Builder().url(getUri());
            WsUtils.setRequestCustomGzipResponseHandlingIfCan(ret, canUseGzip);
            return ret;
        }

        @Override
        protected IWsResult createResult(ITaskContext context, ITask task, Response acceptableResponse) throws IOException {
            FileOutputStream fileOutputStream = null;
            boolean downloaded = false;

            try {
                try {
                    fileOutputStream = new FileOutputStream(getFileDest());
                }
                catch (IOException ex) {
                    LogUtils.e(getTag(), "IOException while opening FileOutputStream", ex);
                    return createErrorResult(context, task, BaseError.ERR_FILE_ERROR);
                }

                downloaded = WsUtils.downloadResponseToStream(acceptableResponse, fileOutputStream, task, true, true, true, getTag());
                if (downloaded)
                    return new WsResult<WsParam>(this, BaseError.ERR_OK);
                else
                    return null;
            }
            finally {
                try {
                    if (fileOutputStream != null)
                        fileOutputStream.close();
                }
                catch (IOException ex) {
                    LogUtils.e(getTag(), "IOException while closing FileOutputStream", ex);
                }

                if (!downloaded) {
                    File f = new File(fileDest);
                    f.delete();
                }
            }
        }

        @Override
        public IWsResult createErrorResult(ITaskContext context, ITask task, ITaskError error) {
            return new WsResult<WsParam>(this, error);
        }
    }
}