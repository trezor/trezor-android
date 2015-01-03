package com.satoshilabs.trezor;

import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import com.satoshilabs.trezor.protobuf.TrezorMessage;
import com.satoshilabs.trezor.protobuf.TrezorType;

import java.text.Normalizer;
import java.util.Arrays;

class Trezor {

    private TrezorGUICallback gui;
    private TrezorDevice device;

    public Trezor(TrezorGUICallback gui, TrezorDevice device) {
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
                TrezorMessage.Success r = (TrezorMessage.Success) resp;
                if (r.hasMessage()) return r.getMessage();
                return "";
            }
            case "Failure":
                throw new IllegalStateException();
        /* User can catch ButtonRequest to Cancel by not calling _get */
            case "ButtonRequest":
                return _get(this.send(TrezorMessage.ButtonAck.newBuilder().build()));
            case "PinMatrixRequest":
                return _get(this.send(
                        TrezorMessage.PinMatrixAck.newBuilder().
                                setPin(this.gui.PinMatrixRequest()).
                                build()));
            case "PassphraseRequest":
                return _get(this.send(
                        TrezorMessage.PassphraseAck.newBuilder().
                                setPassphrase(Normalizer.normalize(this.gui.PassphraseRequest(), Normalizer.Form.NFKD)).
                                build()));
            case "PublicKey": {
                TrezorMessage.PublicKey r = (TrezorMessage.PublicKey) resp;
                if (!r.hasNode()) throw new IllegalArgumentException();
                TrezorType.HDNodeType N = r.getNode();
                String NodeStr = ((N.hasDepth()) ? N.getDepth() : "") + "%" +
                        ((N.hasFingerprint()) ? N.getFingerprint() : "") + "%" +
                        ((N.hasChildNum()) ? N.getChildNum() : "") + "%" +
                        ((N.hasChainCode()) ? bytesToHex(N.getChainCode()) : "") + "%" +
                        ((N.hasPrivateKey()) ? bytesToHex(N.getPrivateKey()) : "") + "%" +
                        ((N.hasPublicKey()) ? bytesToHex(N.getPublicKey()) : "") + "%" +
                        "";
                if (r.hasXpub()) {
                    NodeStr += ":!:" + r.getXpub();
                }
                return NodeStr;
            }
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