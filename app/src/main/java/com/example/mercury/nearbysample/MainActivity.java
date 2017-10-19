package com.example.mercury.nearbysample;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.Connections;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener{

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 1;
    private String SERVICE_ID = "com.example.mercury.nearbysample";

    private GoogleApiClient mGoogleApiClient;

    private Map<String, Endpoint> pendingConnections = new HashMap<>();

    private Map<String, Endpoint> connectedEndpoints = new HashMap<>();

    private ArrayList<MessageModel> messages = new ArrayList<>();

    private MessageAdapter messageAdapter;

    private String username = "";

    @BindView(R.id.broadcast_switch)
    Switch broadcastSwitch;

    @BindView(R.id.discover_switch)
    Switch discoverSwitch;

    @BindView(R.id.message_edit_text)
    EditText messageEditText;

    @BindView(R.id.message_recycler)
    RecyclerView messageRecycler;

    @BindView(R.id.send_message_button)
    Button sendMessageButton;

    //region callbacks
    private final PayloadCallback mPayloadCallback = new PayloadCallback() {
        @Override
        public void onPayloadReceived(String endpointId, Payload payload) {
            Log.d(TAG, "onPayloadReceived endpointId: " + endpointId);
            MessageModel incomingMessage = MessageModel.empty();
            try {
                incomingMessage = MessageModel.convertFromBytes(payload.asBytes());
            } catch (IOException | ClassNotFoundException e) {
                Log.e(TAG, "onPayloadReceived: failure. Could not convert payload to message");
            }
            messages.add(incomingMessage);
            messageAdapter.setMessageList(messages);
        }

        @Override
        public void onPayloadTransferUpdate(String endpointId, PayloadTransferUpdate payloadTransferUpdate) {
            Log.d(TAG, "onPayloadTransferUpdate endpointId: " + endpointId + "update: " + payloadTransferUpdate.toString());
        }
    };

    private final ConnectionLifecycleCallback mConnectionLifecycleCallback = new ConnectionLifecycleCallback() {
        @Override
        public void onConnectionInitiated(String endpointId, ConnectionInfo connectionInfo) {
            Log.d(TAG, "onConnectionInitiated: accepting connection");
            Endpoint endpoint = new Endpoint(endpointId, connectionInfo.getEndpointName());
            pendingConnections.put(endpointId, endpoint);
            Nearby.Connections.acceptConnection(mGoogleApiClient, endpointId, mPayloadCallback);
        }

        @Override
        public void onConnectionResult(String endpointId, ConnectionResolution connectionResolution) {
            switch (connectionResolution.getStatus().getStatusCode()){
                case ConnectionsStatusCodes.STATUS_OK:
                    Log.d(TAG, "onConnectionResult: STATUS_OK. FINALLY CONNECTED");
                    connectedEndpoints.put(endpointId, pendingConnections.remove(endpointId));
                    showMessageBoard();
                    requestName();
                    break;

                case ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED:
                    Log.d(TAG, "onConnectionResult: STATUS_CONNECTION_REJECTED");
                    pendingConnections.remove(endpointId);
                    hideMessageBoard();
                    break;
            }
        }

        @Override
        public void onDisconnected(String s) {
            Log.d(TAG, "onDisconnected: connection ended");
            resetConnections();
        }
    };

    private final EndpointDiscoveryCallback mEndpointDiscoveryCallback = new EndpointDiscoveryCallback() {
        @Override
        public void onEndpointFound(String endpointId, DiscoveredEndpointInfo discoveredEndpointInfo) {
            String name = "the discoverer";
            Nearby.Connections.requestConnection(mGoogleApiClient,
                    name,
                    endpointId,
                    mConnectionLifecycleCallback)
                    .setResultCallback(new ResultCallback<Status>() {
                        @Override
                        public void onResult(@NonNull Status status) {
                            if (status.isSuccess()){
                                Log.d(TAG, "onEndpointFound: Success. successfully _requested_ a connection. Still need both parties to accept");
                            }
                            else {
                                Log.e(TAG, "onEndpointFound: Failure. failed to request a connection");
                            }
                        }
                    });
        }

        @Override
        public void onEndpointLost(String s) {
            Log.e(TAG, "onEndpointLost: A previously discovered endpoint has gone away - " + s);
        }
    };
    //endregion

    //region activity lifecycle
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Nearby.CONNECTIONS_API)
                .enableAutoManage(this, this)
                .build();

        initViews();
        handlePermissions();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: activity paused");
        resetConnections();
    }
    //endregion

    private void initViews() {
        broadcastSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean switchedOn) {
                if (switchedOn){
                    startAdvertising();
                }
                else {
                    Nearby.Connections.stopAdvertising(mGoogleApiClient);
                }
            }
        });

        discoverSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean switchedOn) {
                if (switchedOn){
                    startDiscovering();
                }
                else {
                    Nearby.Connections.stopDiscovery(mGoogleApiClient);
                }
            }
        });

        sendMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String message = String.valueOf(messageEditText.getText());
                MessageModel messageModel = new MessageModel(username, message);
                Payload payload = null;
                try {
                    payload = Payload.fromBytes(MessageModel.convertToBytes(messageModel));
                } catch (IOException e) {
                    Log.e(TAG, "Send message failure: Could not convert message to bytes");
                }
                ArrayList endpointList = new ArrayList(connectedEndpoints.keySet());
                Nearby.Connections.sendPayload(mGoogleApiClient, endpointList, payload);
            }
        });

        messageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                sendMessageButton.setEnabled(!messageEditText.getText().toString().isEmpty());
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });

        messageRecycler.setLayoutManager(new LinearLayoutManager(this));
        messageAdapter = new MessageAdapter(messages);
        messageRecycler.setAdapter(messageAdapter);
    }

    private void handlePermissions() {
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {


            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    Log.d(TAG, "onRequestPermissionsResult: ACCESS_COARSE_LOCATION Success");

                } else {
                    new AlertDialog.Builder(this)
                            .setMessage("This permission is required.")
                            .setPositiveButton("Request again", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    handlePermissions();
                                }
                            })
                            .show();
                }
            }
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {}

    @Override
    public void onConnectionSuspended(int i) {}

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {}

    void startAdvertising() {
        Nearby.Connections.startAdvertising(mGoogleApiClient,
                getUserNickname(),
                SERVICE_ID,
                mConnectionLifecycleCallback,
                new AdvertisingOptions(Strategy.P2P_CLUSTER))
                .setResultCallback(new ResultCallback<Connections.StartAdvertisingResult>() {
                    @Override
                    public void onResult(@NonNull Connections.StartAdvertisingResult startAdvertisingResult) {
                        if (startAdvertisingResult.getStatus().isSuccess()){
                            Log.d(TAG, "startAdvertising onSuccess: " + startAdvertisingResult.toString());
                        }
                        else {
                            Log.e(TAG, "startAdvertising onFailure: " + startAdvertisingResult.getStatus().getStatusMessage());
                        }
                    }
                });
    }

    void startDiscovering() {
        Nearby.Connections.startDiscovery(mGoogleApiClient,
                SERVICE_ID,
                mEndpointDiscoveryCallback,
                new DiscoveryOptions(Strategy.P2P_CLUSTER))
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        if (status.isSuccess()){
                            Log.d(TAG, "startDiascovering onSuccess: " + status.toString());
                        }
                        else {
                            Log.e(TAG, "startDiscovering onFailure: " + status.getStatusMessage());
                        }
                    }
                });
    }

    @OnClick(R.id.reset_button)
    void resetConnections() {
        if (mGoogleApiClient.isConnected()) {
            Nearby.Connections.stopAllEndpoints(mGoogleApiClient);
        }
        hideMessageBoard();
        pendingConnections.clear();
        connectedEndpoints.clear();
        discoverSwitch.setChecked(false);
        broadcastSwitch.setChecked(false);
        messages.clear();
        messageAdapter.setMessageList(messages);
        messageEditText.setText("");
    }

    //helper methods
    private String getUserNickname() {
        return "Mr. Poopy Butthole";
    }

    private void hideMessageBoard() {
        messageEditText.setVisibility(View.INVISIBLE);
        messageRecycler.setVisibility(View.INVISIBLE);
        sendMessageButton.setVisibility(View.INVISIBLE);
    }

    private void showMessageBoard() {
        messageEditText.setVisibility(View.VISIBLE);
        messageRecycler.setVisibility(View.VISIBLE);
        sendMessageButton.setVisibility(View.VISIBLE);
    }

    private void requestName() {
        if (!username.isEmpty()){
            return;
        }

        final AlertDialog.Builder alert = new AlertDialog.Builder(this);
        final EditText edittext = new EditText(this);
        alert.setMessage("What's your name?");
        alert.setTitle("Username");
        alert.setView(edittext);

        alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                username = edittext.getText().toString();
                dialog.dismiss();
            }
        });

        alert.show();
    }
    //endregion
}
