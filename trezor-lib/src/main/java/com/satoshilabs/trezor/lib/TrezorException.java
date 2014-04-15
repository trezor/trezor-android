package com.satoshilabs.trezor.lib;

public class TrezorException extends RuntimeException {
    public static final int TYPE_NOT_CONNECTED = 0;
    public static final int TYPE_NEEDS_PERMISSION = 1;
    public static final int TYPE_COMMUNICATION_ERROR = 2;
    public static final int TYPE_UNEXPECTED_RESPONSE = 3;
    // pozor - pri pridavani nezapominat pridavat prislusne polozky v app: TrezorTasks.TrezorError

    private final int type;

    public TrezorException(int type) {
        this.type = type;
    }

    public TrezorException(int type, Throwable throwable) {
        super(throwable);
        this.type = type;
    }

    public int getType() {
        return this.type;
    }
}
