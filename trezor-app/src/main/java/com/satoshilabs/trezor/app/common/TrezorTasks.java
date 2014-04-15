package com.satoshilabs.trezor.app.common;

import android.os.Parcel;

import com.circlegate.liban.base.ApiBase.ApiCreator;
import com.circlegate.liban.base.ApiBase.ApiParcelable;
import com.circlegate.liban.base.ApiBase.IApiParcelable;
import com.circlegate.liban.base.ApiDataIO.ApiDataInput;
import com.circlegate.liban.base.ApiDataIO.ApiDataOutput;
import com.circlegate.liban.task.TaskCommon.TaskParam;
import com.circlegate.liban.task.TaskCommon.TaskResult;
import com.circlegate.liban.task.TaskErrors.BaseError;
import com.circlegate.liban.task.TaskErrors.ITaskError;
import com.circlegate.liban.task.TaskErrors.TaskError;
import com.circlegate.liban.task.TaskErrors.TaskException;
import com.circlegate.liban.task.TaskInterfaces.ITask;
import com.circlegate.liban.task.TaskInterfaces.ITaskContext;
import com.circlegate.liban.task.TaskInterfaces.ITaskParam;
import com.circlegate.liban.task.TaskInterfaces.ITaskResult;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.satoshilabs.trezor.app.R;
import com.satoshilabs.trezor.lib.TrezorException;
import com.satoshilabs.trezor.lib.TrezorManager;
import com.satoshilabs.trezor.lib.protobuf.TrezorMessage.MessageType;

public class TrezorTasks {
    public interface ITrezorTaskParam extends ITaskParam, IApiParcelable {
        String SERIAL_EXECUTION_KEY_TREZOR = "trezor";

        MsgWrp getMsgParam();
    }

    public interface ITrezorTaskResult extends ITaskResult, IApiParcelable {
        ITrezorTaskParam getParam();
        MsgWrp getMsgResult();
    }


    public static class TrezorError extends TaskError {
        public static final TrezorError ERR_NOT_CONNECTED = new TrezorError(TrezorException.TYPE_NOT_CONNECTED, R.string.trezor_err_not_connected);
        public static final TrezorError ERR_NEEDS_PERMISSION = new TrezorError(TrezorException.TYPE_NEEDS_PERMISSION, R.string.trezor_err_needs_permission);
        public static final TrezorError ERR_COMMUNICATION_ERROR = new TrezorError(TrezorException.TYPE_COMMUNICATION_ERROR, R.string.trezor_err_communication_error);
        public static final TrezorError ERR_UNEXPECTED_RESPONSE = new TrezorError(TrezorException.TYPE_UNEXPECTED_RESPONSE, R.string.trezor_err_unexpected_response);
        // POZOR pri pridavani nezapominat pridavat case radky do tryGetTrezorError()

        private static TrezorError tryGetTrezorError(int type) {
            switch (type) {
                case TrezorException.TYPE_NOT_CONNECTED: return ERR_NOT_CONNECTED;
                case TrezorException.TYPE_NEEDS_PERMISSION: return ERR_NEEDS_PERMISSION;
                case TrezorException.TYPE_COMMUNICATION_ERROR: return ERR_COMMUNICATION_ERROR;
                case TrezorException.TYPE_UNEXPECTED_RESPONSE: return ERR_UNEXPECTED_RESPONSE;
                default: return null;
            }
        }


        private final int type;
        private final int msgRid;

        public static ITaskError getErrorFromException(TrezorException ex) {
            TrezorError res = tryGetTrezorError(ex.getType());
            return res != null ? res : BaseError.ERR_UNKNOWN_ERROR;
        }

        // private!
        private TrezorError(int type, int msgRid) {
            this.type = type;
            this.msgRid = msgRid;
        }

        @Override
        public void save(ApiDataOutput d, int flags) {
            d.write(this.type);
        }

        public int getType() {
            return this.type;
        }

        public int getMsgRid() {
            return this.msgRid;
        }

        @Override
        public int hashCode() {
            int _hash = 17;
            _hash = _hash * 29 + type;
            _hash = _hash * 29 + msgRid;
            return _hash;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if (!(o instanceof TrezorError)) {
                return false;
            }

            TrezorError lhs = (TrezorError) o;
            return lhs != null &&
                    type == lhs.type &&
                    msgRid == lhs.msgRid;
        }

        @Override
        public CharSequence getMsg(ITaskContext context) {
            return context.getAndroidContext().getString(msgRid);
        }

        @Override
        public String getGoogleAnalyticsId() {
            return "TrezorError:" + type;
        }

        // upraveno!
        public static final ApiCreator<TrezorError> CREATOR = new ApiCreator<TrezorError>() {
            public TrezorError create(ApiDataInput d) {
                TrezorError ret = tryGetTrezorError(d.readInt());
                if (ret == null)
                    throw new IllegalStateException();
                return ret;
            }

            public TrezorError[] newArray(int size) { return new TrezorError[size]; }
        };
    }


    public static class TrezorTaskParam extends TaskParam implements ITrezorTaskParam {
        private final MsgWrp msgParam;
        private final IApiParcelable tag; // abstract optional

        public TrezorTaskParam(Message msg) {
            this(new MsgWrp(msg), null);
        }

        public TrezorTaskParam(Message msg, IApiParcelable tag) {
            this(new MsgWrp(msg), tag);
        }

        public TrezorTaskParam(MsgWrp msgParam, IApiParcelable tag) {
            this.msgParam = msgParam;
            this.tag = tag;
        }

        public TrezorTaskParam(ApiDataInput d) {
            this.msgParam = d.readObject(MsgWrp.CREATOR);
            this.tag = d.readOptParcelableWithName();
        }

        @Override
        public void save(ApiDataOutput d, int flags) {
            d.write(this.msgParam, flags);
            d.writeOpt(this.tag, flags);
        }

        public MsgWrp getMsgParam() {
            return this.msgParam;
        }

        public IApiParcelable getTag() {
            return this.tag;
        }

        @Override
        public int describeContents() {
            return ApiParcelable.baseDescribeContents();
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            ApiParcelable.baseWriteToParcel(this, parcel, i);
        }

        @Override
        public String getSerialExecutionKey(ITaskContext context) {
            return SERIAL_EXECUTION_KEY_TREZOR;
        }

        @Override
        public TrezorTaskResult createResultUncached(ITaskContext context, ITask task) throws TaskException {
            try {
                GlobalContext gct = (GlobalContext)context;
                TrezorManager t = gct.getTrezorManager();

                if (!t.tryConnectDevice()) {
                    return createErrorResult(context, task, t.hasDeviceWithoutPermission(false) ? TrezorError.ERR_NEEDS_PERMISSION : TrezorError.ERR_NOT_CONNECTED);
                }
                else {
                    MsgWrp msgResult = new MsgWrp(t.sendMessage(msgParam.msg));
                    return new TrezorTaskResult(this, BaseError.ERR_OK, msgResult);
                }
            }
            catch (TrezorException ex) {
                return createErrorResult(context, task, TrezorError.getErrorFromException(ex));
            }
        }

        @Override
        public TrezorTaskResult createErrorResult(ITaskContext context, ITask task, ITaskError error) {
            return new TrezorTaskResult(this, error, null);
        }

        public static final ApiCreator<TrezorTaskParam> CREATOR = new ApiCreator<TrezorTaskParam>() {
            public TrezorTaskParam create(ApiDataInput d) { return new TrezorTaskParam(d); }
            public TrezorTaskParam[] newArray(int size) { return new TrezorTaskParam[size]; }
        };
    }

    public static class TrezorTaskResult extends TaskResult<TrezorTaskParam> implements ITrezorTaskResult {
        private final MsgWrp msgResult;

        public TrezorTaskResult(TrezorTaskParam param, ITaskError error, MsgWrp msgResult) {
            super(param, error);
            this.msgResult = msgResult;
        }

        public TrezorTaskResult(ApiDataInput d) {
            super(d.<TrezorTaskParam>readParcelableWithName(), d.<ITaskError>readParcelableWithName());

            if (isValidResult()) {
                this.msgResult = d.readObject(MsgWrp.CREATOR);
            }
            else {
                this.msgResult = null;
            }

        }

        @Override
        public void save(ApiDataOutput d, int flags) {
            d.writeWithName(getParam(), flags);
            d.writeWithName(getError(), flags);

            if (isValidResult()) {
                d.write(this.msgResult, flags);
            }
        }

        public MsgWrp getMsgResult() {
            return this.msgResult;
        }

        @Override
        public int describeContents() {
            return ApiParcelable.baseDescribeContents();
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            ApiParcelable.baseWriteToParcel(this, parcel, i);
        }

        @Override
        public boolean isCacheableResult() {
            return false;
        }

        public static final ApiCreator<TrezorTaskResult> CREATOR = new ApiCreator<TrezorTaskResult>() {
            public TrezorTaskResult create(ApiDataInput d) { return new TrezorTaskResult(d); }
            public TrezorTaskResult[] newArray(int size) { return new TrezorTaskResult[size]; }
        };
    }


    public static class MsgWrp extends ApiParcelable {
        public final MessageType msgType;
        public final Message msg;

        public MsgWrp(Message msg) {
            this.msgType = MessageType.valueOf("MessageType_" + msg.getClass().getSimpleName());
            this.msg = msg;
        }

        public MsgWrp(ApiDataInput d) {
            this.msgType = MessageType.valueOf(d.readString());
            try {
                this.msg = TrezorManager.parseMessageFromBytes(msgType, d.readBytes());
            } catch (InvalidProtocolBufferException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void save(ApiDataOutput d, int flags) {
            d.write(this.msgType.name());
            d.write(msg.toByteArray());
        }

        public static final ApiCreator<MsgWrp> CREATOR = new ApiCreator<MsgWrp>() {
            public MsgWrp create(ApiDataInput d) { return new MsgWrp(d); }
            public MsgWrp[] newArray(int size) { return new MsgWrp[size]; }
        };
    }

    public static class IntParcelable extends ApiParcelable {
        public final int value;

        public IntParcelable(int value) {
            this.value = value;
        }

        public IntParcelable(ApiDataInput d) {
            this.value = d.readInt();
        }

        @Override
        public void save(ApiDataOutput d, int flags) {
            d.write(this.value);
        }

        public static final ApiCreator<IntParcelable> CREATOR = new ApiCreator<IntParcelable>() {
            public IntParcelable create(ApiDataInput d) { return new IntParcelable(d); }
            public IntParcelable[] newArray(int size) { return new IntParcelable[size]; }
        };
    }
}
