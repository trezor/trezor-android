package com.satoshilabs.trezor;

import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import com.satoshilabs.trezor.protobuf.TrezorMessage;
import com.satoshilabs.trezor.protobuf.TrezorType;

import java.text.Normalizer;
import java.util.Arrays;

class Trezor {

    private TrezorCallback gui;
    private TrezorDevice device;

    public Trezor(TrezorCallback gui, TrezorDevice device) {
        this.gui = gui;
        this.device = device;
    }

    private static String bytesToHex(ByteString bytes) {
        String hex = "";
        for (int j = 0; j < bytes.size(); j++) {
            hex += String.format("%02x", bytes.byteAt(j) & 0xFF);
        }
        return hex;
    }

    private String _get(Message resp) {
        switch (resp.getClass().getSimpleName()) {
            case "Success": {
                TrezorMessage.Success rs = (TrezorMessage.Success) resp;
                if (rs.hasMessage()) return rs.getMessage();
                return "";
            }
            case "ButtonRequest":
                TrezorMessage.ButtonRequest rbr = (TrezorMessage.ButtonRequest) resp;
                if (this.gui.ButtonRequest(rbr.getCode(), rbr.getData())) {
                    return _get(this.send(TrezorMessage.ButtonAck.newBuilder().build()));
                } else {
                    this.send(TrezorMessage.Cancel.newBuilder().build());
                    return "";
                }
            case "PinMatrixRequest":
                TrezorMessage.PinMatrixRequest rpmr = (TrezorMessage.PinMatrixRequest) resp;
                return _get(this.send(
                        TrezorMessage.PinMatrixAck.newBuilder().
                                setPin(this.gui.PinMatrixRequest(rpmr.getType())).
                                build()));
            case "PassphraseRequest":
                return _get(this.send(
                        TrezorMessage.PassphraseAck.newBuilder().
                                setPassphrase(Normalizer.normalize(this.gui.PassphraseRequest(), Normalizer.Form.NFKD)).
                                build()));
            case "PublicKey": {
                TrezorMessage.PublicKey rpk = (TrezorMessage.PublicKey) resp;
                if (!rpk.hasNode()) throw new IllegalArgumentException();
                TrezorType.HDNodeType N = rpk.getNode();
                String str = ((N.hasDepth()) ? N.getDepth() : "") + "%" +
                        ((N.hasFingerprint()) ? N.getFingerprint() : "") + "%" +
                        ((N.hasChildNum()) ? N.getChildNum() : "") + "%" +
                        ((N.hasChainCode()) ? bytesToHex(N.getChainCode()) : "") + "%" +
                        ((N.hasPrivateKey()) ? bytesToHex(N.getPrivateKey()) : "") + "%" +
                        ((N.hasPublicKey()) ? bytesToHex(N.getPublicKey()) : "") + "%" +
                        "";
                if (rpk.hasXpub()) {
                    str += ":!:" + rpk.getXpub();
                }
                return str;
            }
            case "Failure":
                throw new IllegalStateException();
        }
//		throw new IllegalArgumentException();
        return resp.getClass().getSimpleName();
    }

    public Message send(Message msg) {
        return this.device.send(msg);
    }

    public String MessagePing() {
        return _get(this.send(TrezorMessage.Ping.newBuilder().build()));
    }

    public String MessagePing(String msg) {
        return _get(this.send(
                TrezorMessage.Ping.newBuilder().
                        setMessage(msg).
                        build()));
    }

    public String MessageGetPublicKey(Integer[] addrn) {
        return _get(this.send(
                TrezorMessage.GetPublicKey.newBuilder().
                        clearAddressN().
                        addAllAddressN(Arrays.asList(addrn)).
                        build()));
    }


}