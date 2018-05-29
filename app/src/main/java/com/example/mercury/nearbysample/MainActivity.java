package com.example.mercury.nearbysample;

import android.Manifest;
import android.content.Context;
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
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;

import com.example.mercury.nearbysample.MessageAdapter.MainActivityCallback;
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
        GoogleApiClient.OnConnectionFailedListener, MainActivityCallback {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 1;

    private String SERVICE_ID = "com.example.mercury.nearbysample";
    private GoogleApiClient mGoogleApiClient;
    private Map<String, Endpoint> pendingConnections = new HashMap<>();
    private Map<String, Endpoint> connectedEndpoints = new HashMap<>();
    private ArrayList<MessageModel> messages = new ArrayList<>();
    private MessageAdapter messageAdapter;
    private String username = "";
    private AlertDialog usernameAlert;

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
            displayMessage(incomingMessage);
        }

        @Override
        public void onPayloadTransferUpdate(String endpointId, PayloadTransferUpdate payloadTransferUpdate) {
            String update;
            switch (payloadTransferUpdate.getStatus()){
                //TODO: color message based on status
                case PayloadTransferUpdate.Status.SUCCESS:
                    update = "SUCCESS";
                    break;
                case PayloadTransferUpdate.Status.IN_PROGRESS:
                    update = "IN_PROGRESS";
                    break;
                case PayloadTransferUpdate.Status.FAILURE:
                default:
                    update = "FAILURE";
            }
            Log.d(TAG, "onPayloadTransferUpdate endpointId: " + endpointId + "update: " + update);
        }
    };

    private final ConnectionLifecycleCallback mConnectionLifecycleCallback = new ConnectionLifecycleCallback() {
        @Override
        public void onConnectionInitiated(String endpointId, ConnectionInfo connectionInfo) {
            //TODO: populate nav drawer with endpoints, click to accept connection
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
                    hideSwitches();
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

    //region connection methods
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
                            Log.d(TAG, "startDiscovering onSuccess: " + status.toString());
                        }
                        else {
                            Log.e(TAG, "startDiscovering onFailure: " + status.getStatusMessage());
                        }
                    }
                });
    }
    //endregion

    //region view methods
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
                displayMessage(messageModel);
                Payload payload = null;
                try {
                    payload = Payload.fromBytes(MessageModel.convertToBytes(messageModel));
                } catch (IOException e) {
                    Log.e(TAG, "Send message failure: Could not convert message to bytes");
                }
                ArrayList endpointList = new ArrayList(connectedEndpoints.keySet());
                Nearby.Connections.sendPayload(mGoogleApiClient, endpointList, payload);
                messageEditText.getText().clear();
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
        messageEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (inputMethodManager != null){
                    int showFlags = hasFocus ? InputMethodManager.SHOW_IMPLICIT : 0;
                    int hideFlags = hasFocus ? 0 : InputMethodManager.HIDE_NOT_ALWAYS;
                    inputMethodManager.toggleSoftInput(showFlags, hideFlags);
                }
            }
        });

        messageRecycler.setLayoutManager(new LinearLayoutManager(this));
        messageAdapter = new MessageAdapter(messages, this);
        messageRecycler.setAdapter(messageAdapter);
    }

    @OnClick(R.id.reset_button)
    void resetConnections() {
        if (mGoogleApiClient.isConnected()) {
            Nearby.Connections.stopAllEndpoints(mGoogleApiClient);
        }
        hideMessageBoard();
        showSwitches();

        pendingConnections.clear();
        connectedEndpoints.clear();
        discoverSwitch.setChecked(false);
        broadcastSwitch.setChecked(false);
        messages.clear();
        messageAdapter.setMessageList(messages);
        messageEditText.getText().clear();
        messageEditText.clearFocus();
        if (usernameAlert != null) {
            usernameAlert.dismiss();
        }
    }
    //endregion

    //region permissions
    private void handlePermissions() {
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
    //endregion

    //region helper methods
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

    private void displayMessage(MessageModel messageModel) {
        messages.add(messageModel);
        messageAdapter.setMessageList(messages);
        messageRecycler.scrollToPosition(messageAdapter.getItemCount() - 1);
    }

    private void requestName() {
        if (!username.isEmpty()){
            return;
        }

        usernameAlert = new AlertDialog.Builder(this).create();
        final EditText usernameEditText = new EditText(this);

        usernameAlert.setMessage("What's your name?");
        usernameAlert.setTitle("Username");
        usernameAlert.setView(usernameEditText);
        usernameAlert.setButton(DialogInterface.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                username = usernameEditText.getText().toString();
                dialog.dismiss();
            }
        });

        usernameEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    Window window = usernameAlert.getWindow();
                    if (window != null) {
                        //display keyboard when alert dialog displays
                        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                    }
                }
            }
        });
        usernameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                //disable OK button for blank username
                usernameAlert.getButton(DialogInterface.BUTTON_POSITIVE)
                        .setEnabled(!usernameEditText.getText().toString().isEmpty());
            }
            @Override
            public void afterTextChanged(Editable editable) {}
        });

        usernameAlert.show();
    }

    private void hideSwitches() {
        broadcastSwitch.setVisibility(View.GONE);
        discoverSwitch.setVisibility(View.GONE);
    }

    private void showSwitches() {
        broadcastSwitch.setVisibility(View.VISIBLE);
        discoverSwitch.setVisibility(View.VISIBLE);
    }

    @Override
    public String getUsername() {
        return username;
    }
    //endregion
}
