package com.circlegate.liban.task;

import android.text.TextUtils;
import android.widget.Toast;

import com.circlegate.liban.R;
import com.circlegate.liban.base.ApiBase.ApiCreator;
import com.circlegate.liban.base.ApiBase.ApiParcelable;
import com.circlegate.liban.base.ApiBase.IApiParcelable;
import com.circlegate.liban.base.ApiDataIO.ApiDataInput;
import com.circlegate.liban.base.ApiDataIO.ApiDataOutput;
import com.circlegate.liban.base.Exceptions.NotImplementedException;
import com.circlegate.liban.task.TaskInterfaces.ITaskContext;
import com.circlegate.liban.utils.LogUtils;

public class TaskErrors {
    public interface ITaskError extends IApiParcelable {
        boolean isOk();
        CharSequence getMsg(ITaskContext context);
        //DialogFragment createDialog(ITaskContext context, boolean finish);
        void showToast(ITaskContext context);
        TaskException createExc(ITaskContext context);
        String getGoogleAnalyticsId();
    }



    public static class TaskException extends Exception {
        private final ITaskError taskError;

        public TaskException(ITaskError taskError) {
            this.taskError = taskError;
        }

        public ITaskError getTaskError() {
            return taskError;
        }
    }

//    public static class TaskErrorDialog extends BaseDialogFragment {
//        private boolean finish;
//
//        public static TaskErrorDialog newInstance(CharSequence msg, boolean finish) {
//            Bundle b = new Bundle();
//            b.putCharSequence("msg", msg);
//            b.putBoolean("finish", finish);
//            TaskErrorDialog ret = new TaskErrorDialog();
//            ret.setArguments(b);
//            ret.setCancelable(true);
//            return ret;
//        }
//
//        @Override
//        protected Builder build(Builder b, Bundle savedInstanceState) {
//            Bundle args = getArguments();
//            final CharSequence errorMsg = args.getCharSequence("msg");
//            this.finish = args.getBoolean("finish");
//
//            b.setTitle(getString(R.string.error));
//            b.setMessage(errorMsg);
//            b.setPositiveButton(android.R.string.ok, new OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    dismiss();
//                    if (finish && getActivity() != null)
//                        getActivity().finish();
//                }
//            });
//            return b;
//        }
//
//        @Override
//        public void onCancel(DialogInterface dialog) {
//            super.onCancel(dialog);
//            if (finish && getActivity() != null)
//                getActivity().finish();
//        }
//    }

    public static abstract class TaskError extends ApiParcelable implements ITaskError {
        @Override
        public boolean isOk() {
            return false;
        }

//        @Override
//        public DialogFragment createDialog(ITaskContext context, boolean finish) {
//            return TaskErrorDialog.newInstance(getDialogMsg(context), finish);
//        }

        @Override
        public void showToast(ITaskContext context) {
            LogUtils.e("TaskError", "showToast: " + getMsg(context).toString());
            Toast.makeText(context.getAndroidContext(), getMsg(context), Toast.LENGTH_SHORT).show();
        }

        @Override
        public TaskException createExc(ITaskContext context) {
            return new TaskException(this);
        }

        //protected CharSequence getDialogMsg(ITaskContext context) {
        //    return getMsg(context);
        //}

        //protected CharSequence getToastMsg(ITaskContext context) {
        //    return getMsg(context);
        //}
    }


    public static class BaseError extends TaskError {
        public static final int OK = 0;
        public static final int UNKNOWN_ERROR = -1;
        public static final int SERVER_ERROR = -2;
        public static final int CONNECTION_ERROR_COMMUNICATION = -3;
        public static final int CONNECTION_ERROR_UNEXPECTED_RES = -4;
        public static final int FILE_ERROR = -5;
        public static final int LOGIN_ERROR = -6;

        public static final BaseError ERR_OK = new BaseError(OK, 0) {
            @Override
            public boolean isOk() {
                return true;
            }
        };
        public static final BaseError ERR_UNKNOWN_ERROR = new BaseError(UNKNOWN_ERROR, R.string.err_unknown_error);
        public static final BaseError ERR_SERVER_ERROR = new BaseError(SERVER_ERROR, R.string.err_server_error);
        public static final BaseError ERR_CONNECTION_ERROR_COMMUNICATION = new BaseError(CONNECTION_ERROR_COMMUNICATION, R.string.err_connection_error_communication);
        public static final BaseError ERR_CONNECTION_ERROR_UNEXPECTED_RES = new BaseError(CONNECTION_ERROR_UNEXPECTED_RES, R.string.err_connection_error_unexpected_res);
        public static final BaseError ERR_FILE_ERROR = new BaseError(FILE_ERROR, R.string.err_file_error);
        public static final BaseError ERR_LOGIN_ERROR = new BaseError(LOGIN_ERROR, R.string.err_login_error);
        // nezapomenout pridavat case vetve v ApiCreatoru!

        private final int id;
        private final int resId;

        private BaseError(int id, int resId) {
            this.id = id;
            this.resId = resId;
        }

        @Override
        public void save(ApiDataOutput d, int flags) {
            d.write(this.id);
        }

        public int getId() {
            return this.id;
        }

        public int getResId() {
            return this.resId;
        }

        @Override
        public int hashCode() {
            int _hash = 17;
            _hash = _hash * 29 + id;
            //_hash = _hash * 29 + resId;
            return _hash;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if (!(o instanceof BaseError)) {
                return false;
            }

            BaseError lhs = (BaseError) o;
            return lhs != null &&
                    id == lhs.id;
            //resId == lhs.resId;
        }


        @Override
        public CharSequence getMsg(ITaskContext context) {
            return resId != 0 ? context.getAndroidContext().getString(resId) : "";
        }

        @Override
        public String getGoogleAnalyticsId() {
            return "BaseError:" + id;
        }

        public static boolean isConnectionError(ITaskError error) {
            return error instanceof BaseError
                    && (((BaseError)error).id == CONNECTION_ERROR_COMMUNICATION || ((BaseError)error).id == CONNECTION_ERROR_UNEXPECTED_RES);
        }

        public static final ApiCreator<BaseError> CREATOR = new ApiCreator<BaseError>() {
            public BaseError create(ApiDataInput d) {
                int id = d.readInt();
                switch (id) {
                    case OK: return ERR_OK;
                    case UNKNOWN_ERROR: return ERR_UNKNOWN_ERROR;
                    case SERVER_ERROR: return ERR_SERVER_ERROR;
                    case CONNECTION_ERROR_COMMUNICATION: return ERR_CONNECTION_ERROR_COMMUNICATION;
                    case CONNECTION_ERROR_UNEXPECTED_RES: return ERR_CONNECTION_ERROR_UNEXPECTED_RES;
                    case FILE_ERROR: return ERR_FILE_ERROR;
                    case LOGIN_ERROR: return ERR_LOGIN_ERROR;
                    default: throw new NotImplementedException();
                }
            }

            public BaseError[] newArray(int size) { return new BaseError[size]; }
        };
    }


    public static class SimpleError extends TaskError {
        private final int rid;

        public SimpleError(int rid) {
            this.rid = rid;
        }

        public SimpleError(ApiDataInput d) {
            this.rid = d.readInt();
        }

        @Override
        public void save(ApiDataOutput d, int flags) {
            d.write(this.rid);
        }

        public int getRid() {
            return this.rid;
        }

        @Override
        public CharSequence getMsg(ITaskContext context) {
            if (rid != 0)
                return context.getAndroidContext().getString(rid);
            else
                return "";
        }

        @Override
        public String getGoogleAnalyticsId() {
            return "SimpleError-rid:" + rid;
        }

        public static final ApiCreator<SimpleError> CREATOR = new ApiCreator<SimpleError>() {
            public SimpleError create(ApiDataInput d) { return new SimpleError(d); }
            public SimpleError[] newArray(int size) { return new SimpleError[size]; }
        };
    }

    public static class ErrCodeMsgError extends TaskError {
        public static final int DEFAULT_ERR_CODE = -1;

        private final int errCode;
        private final String errMessage;

        public ErrCodeMsgError(String errMessage) {
            this(DEFAULT_ERR_CODE, errMessage);
        }

        // Upraveno!
        public ErrCodeMsgError(int errCode, String errMessage) {
            this.errCode = errCode;
            this.errMessage = errMessage == null ? "" : errMessage;
        }

        public ErrCodeMsgError(ApiDataInput d) {
            this.errCode = d.readInt();
            this.errMessage = d.readString();
        }

        @Override
        public void save(ApiDataOutput d, int flags) {
            d.write(this.errCode);
            d.write(this.errMessage);
        }

        public int getErrCode() {
            return this.errCode;
        }

        public String getErrMessage() {
            return this.errMessage;
        }


        @Override
        public CharSequence getMsg(ITaskContext context) {
            return !TextUtils.isEmpty(errMessage) ? errMessage : context.getAndroidContext().getString(R.string.err_unknown_error);
        }

        @Override
        public String getGoogleAnalyticsId() {
            return "ErrCodeMsgError:" + errCode;
        }

        public static final ApiCreator<ErrCodeMsgError> CREATOR = new ApiCreator<ErrCodeMsgError>() {
            public ErrCodeMsgError create(ApiDataInput d) { return new ErrCodeMsgError(d); }
            public ErrCodeMsgError[] newArray(int size) { return new ErrCodeMsgError[size]; }
        };
    }
}

