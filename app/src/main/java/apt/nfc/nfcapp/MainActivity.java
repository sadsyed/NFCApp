package apt.nfc.nfcapp;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.charset.Charset;


public class MainActivity extends Activity {

    public static final String TAG = "NFCDemo";

    private NfcAdapter nfcAdapter;

    private TextView mTextView;
    private Button writeTagButton;
    private Button readTagButton;

    private boolean writeMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTextView = (TextView) findViewById(R.id.textView1);
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);

        if (nfcAdapter == null) {
            Toast.makeText(this, "NFC is not supported on this device.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        
        if (!nfcAdapter.isEnabled()) {
            mTextView.setText("NFC is disabled.");
        } else {
            mTextView.setText("NFC is enabled.");
        }

        writeTagButton = (Button) findViewById(R.id.writeTagButton);
        writeTagButton.setOnClickListener(writeTagListener);

        readTagButton = (Button) findViewById(R.id.readTagButton);
        readTagButton.setOnClickListener(readTagListener);
    }

    /*public void onClick(View v) {
        displayToastMessage("Tap and hold the tag against the back to the phone to write.");
        beginWrite();
    }*/

    private View.OnClickListener writeTagListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            try {
                displayToastMessage("Tap and hold the tag against the back of the phone to write.");
                beginWrite();
            } catch (Exception e) {

            }
        }
    };

    private View.OnClickListener readTagListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            try {
                displayToastMessage("Tap and hold the tag against the back of the phone to read.");
                beginRead();
            } catch (Exception e) {

            }
        }
    };

    private void beginWrite() {
        writeMode =true;
        enableForegroundMode();
    }

    private void beginRead() {
        enableForegroundMode();
    }

    @Override
    public void onNewIntent(Intent intent) {
        if(writeMode) {
            writeMode = false;
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            writeTag(tag);
        } else { //read a tag
            setIntent(intent);
            readTag(intent);
        }
    }

    private void readTag(Intent intent) {
        String action = intent.getAction();
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            Parcelable[] parcelables = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);

            NdefMessage[] ndefMessages =null;
            if (parcelables != null) {
                ndefMessages = new NdefMessage[parcelables.length];
                for (int i=0; i<parcelables.length; i++) {
                    ndefMessages[i] = (NdefMessage) parcelables[i];
                }
            }
            if (ndefMessages == null || ndefMessages.length == 0) {
                return;
            }

            String tagId = new String(ndefMessages[0].getRecords()[0].getType());
            String body = new String(ndefMessages[0].getRecords()[0].getPayload());

            StringBuilder tagDataBuilder = new StringBuilder();
            tagDataBuilder.append("Tag Data: ").append(body);

            EditText tagDataEditText = (EditText) findViewById(R.id.tagDataEditText);
            tagDataEditText.setText(tagDataBuilder.toString());
        }
    }

    private boolean writeTag(Tag tag) {
        EditText tagDataEditText = (EditText) findViewById(R.id.tagDataEditText);
        String tagData = tagDataEditText.getText().toString();

        //byte[] payload = "Text stored in an NFC tag".getBytes();
        byte[] payload = tagData.getBytes();
        byte[] mimeBytes = "text/plain".getBytes(Charset.forName("US-ASCII"));

        NdefRecord ndefRecord = new NdefRecord(NdefRecord.TNF_MIME_MEDIA, mimeBytes, new byte[0], payload);
        NdefMessage ndefMessage = new NdefMessage(new NdefRecord[] { ndefRecord });

        try {
            Ndef ndef = Ndef.get(tag);
            if (ndef != null) {
                ndef.connect();

                if(!ndef.isWritable()) {
                    displayToastMessage("This is a read-only tag");
                    return false;
                }

                int size = ndefMessage.toByteArray().length;
                if (ndef.getMaxSize() < size ) {
                    displayToastMessage("There is not enough space to write.");
                    return false;
                }

                ndef.writeNdefMessage(ndefMessage);
                displayToastMessage("Write successful.");
                return true;
            } else {
                NdefFormatable ndefFormatable = NdefFormatable.get(tag);
                if (ndefFormatable != null) {
                    try {
                        ndefFormatable.connect();
                        ndefFormatable.format(ndefMessage);
                        displayToastMessage("Write successful\nLaunch a scanning app or scan and choose to read.");
                        return true;
                    } catch (Exception e){
                        displayToastMessage("Unable to ndefFormatable tag to NDEF");
                        return false;
                    }
                } else {
                    displayToastMessage("Tag doesn't appear to support NDEF ndefFormatable.");
                    return false;
                }
            }
        } catch (Exception e) {
            displayToastMessage("Write failed.");
        }
        return false;
    }

    private void displayToastMessage(String message) {
        Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause.....");
        super.onPause();
        stopWrite();
        disableForegroundMode();
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume.... ");
        super.onResume();
        enableForegroundMode();
    }

    private void enableForegroundMode() {
        Log.d(TAG, "enableForegroundMode...");

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        IntentFilter[] filters = new IntentFilter[] { tagDetected };
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, filters, null);
    }

    private void disableForegroundMode() {
        Log.d(TAG, "disableForegroundMode.....");
        nfcAdapter.disableForegroundDispatch(this);
    }

    private void stopWrite() {
        nfcAdapter.disableForegroundDispatch(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
