package com.satoshilabs.trezor;

import com.google.protobuf.Message;
import com.satoshilabs.trezor.Trezor;
import com.satoshilabs.trezor.protobuf.TrezorMessage;
import com.satoshilabs.trezor.TrezorGUICallback;

import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        EditText editText = (EditText)findViewById(R.id.editLog);
        if (id == R.id.action_settings) {
            editText.append("Settings not implemented, yet\n");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
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

    public void sendMessage(View view) {
        EditText editText = (EditText)findViewById(R.id.editLog);
        Trezor t = Trezor.getDevice(this, this);
        if (t != null) {
            editText.append( t.toString() + "\n");
            String resp = "unkn";
            try { resp = t.MessagePing("Hello World!"); }
            catch (IllegalStateException e) { resp = "null"; }
            finally { editText.append("got: " + resp + "\n"); }
            resp = "unkn";
            resp = t.MessageGetPublicKey(new Integer[]{0});
            editText.append("got: " + resp + "\n");
            resp = "unkn";
            resp = t.MessageGetPublicKey(new Integer[]{1});
            editText.append("got: " + resp + "\n");
            resp = "unkn";
            resp = t.MessageGetPublicKey(new Integer[]{2});
            editText.append("got: " + resp + "\n");
            resp = "unkn";
            resp = t.MessageGetPublicKey(new Integer[]{3});
            editText.append("got: " + resp + "\n");
            resp = "unkn";
            resp = t.MessageGetPublicKey(new Integer[]{4});
            editText.append("got: " + resp + "\n");
            resp = "unkn";
            resp = t.MessageGetPublicKey(new Integer[]{0,1,2,3,4});
            editText.append("got: " + resp + "\n");
            resp = "unkn";
            try { resp = t.MessagePing("Protect World!", true); }
            catch (IllegalStateException e) { resp = "null"; }
            finally { editText.append("got: " + resp + "\n"); }
        } else {
            editText.append("TREZOR(null)\n");
        }
    }

    /* TrezorClientGUICallback */
    public String PinMatrixRequest() { return ""; };
    public String PassphraseRequest() { return ""; };

}
