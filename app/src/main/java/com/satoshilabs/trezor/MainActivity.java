package com.satoshilabs.trezor;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import java.util.HashMap;

public class MainActivity extends ActionBarActivity implements TrezorGUICallback {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }
    }

    public void test(View view) {
        EditText editText = (EditText) findViewById(R.id.editLog);

        // enumerate devices
        HashMap<String, TrezorDevice> list = TrezorEnumerator.enumerate(this);
        editText.append("Found " + list.size() + " device(s)\n");
        if (list.size() < 1) {
            return;
        }

        // use the first one
        editText.append("Using first one\n");
        TrezorDevice dev = list.values().iterator().next();
        editText.append(dev.toString() + "\n");
        Trezor t = new Trezor(this, dev);

        String resp;

        resp = t.MessagePing("Hello World!");
        editText.append("got: " + resp + "\n");

        resp = t.MessageGetPublicKey(new Integer[]{44 + 0x80000000, 0 + 0x80000000, 0 + 0x80000000});
        editText.append("got: " + resp + "\n");
    }

    public String PinMatrixRequest() {
        return "";
    }

    public String PassphraseRequest() {
        return "";
    }

    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            return rootView;
        }
    }

}
