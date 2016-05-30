# trezor-android

TREZOR Library for Android

## Requirements

TREZOR has to be connected to Android by OTG cable. The Android device needs to support such connections.

## Example


``` java
import com.google.protobuf.Message;
import com.satoshilabs.trezor.Trezor;
import com.satoshilabs.trezor.protobuf.TrezorMessage;

Trezor t = Trezor.getDevice(this);
if (t != null) {
    TrezorMessage.Initialize req = TrezorMessage.Initialize.newBuilder().build();
    Message resp = t.send(req);
    if (resp != null) {
        // "got: " + resp.getClass().getSimpleName()
    }
}
```

You can see examples of real-life usage in [GreenBits repository](https://github.com/greenaddress/GreenBits/blob/master/app/src/main/java/com/greenaddress/greenbits/wallets/TrezorHWWallet.java) or [MyCelium repository](https://github.com/mycelium-com/wallet/tree/master/public/mbw/src/main/java/com/mycelium/wallet/trezor).
