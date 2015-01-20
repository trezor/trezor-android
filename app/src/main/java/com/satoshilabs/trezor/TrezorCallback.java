package com.satoshilabs.trezor;
import com.satoshilabs.trezor.protobuf.TrezorType;

public interface TrezorCallback {

    public boolean ButtonRequest(TrezorType.ButtonRequestType code, String data);

    public String PinMatrixRequest(TrezorType.PinMatrixRequestType type);

    public String PassphraseRequest();

}
