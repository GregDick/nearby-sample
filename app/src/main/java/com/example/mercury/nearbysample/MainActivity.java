package com.example.mercury.nearbysample;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener{

    private static final String TAG = MainActivity.class.getSimpleName();
    private String SERVICE_ID = "com.example.mercury.nearbysample";

    private GoogleApiClient mGoogleApiClient;

    private Map<String, Endpoint> pendingConnections = new HashMap<>();

    private Map<String, Endpoint> connectedEndpoints = new HashMap<>();

    @BindView(R.id.broadcast_switch)
    Switch broadcastSwitch;

    @BindView(R.id.discover_switch)
    Switch discoverSwitch;

    @BindView(R.id.message_edit_text)
    EditText messageEditText;

    @BindView(R.id.message_board)
    TextView messageBoard;

    @BindView(R.id.send_message_button)
    Button sendMessageButton;

    //region callbacks
    private final PayloadCallback mPayloadCallback = new PayloadCallback() {
        @Override
        public void onPayloadReceived(String endpointId, Payload payload) {
            Log.d(TAG, "onPayloadReceived endpointId: " + endpointId);
            messageBoard.setText(new String(payload.asBytes()));
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
                Payload payload = Payload.fromBytes(message.getBytes());
                ArrayList endpointList = new ArrayList(connectedEndpoints.keySet());
                Nearby.Connections.sendPayload(mGoogleApiClient, endpointList, payload);
            }
        });

        messageEditText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                sendMessageButton.setEnabled(!messageEditText.getText().toString().isEmpty());
                return false;
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();

        resetConnections();
    }

    //endregion

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
                            //todo: now we're advertising
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
                            //todo: now we're detectin
                            Log.d(TAG, "startDiscovering onSuccess: " + status.toString());
                        }
                        else {
                            Log.e(TAG, "startDiscovering onFailure: " + status.getStatusMessage());
                        }
                    }
                });
    }

    @OnClick(R.id.reset_button)
    void resetConnections(){
        Nearby.Connections.stopAllEndpoints(mGoogleApiClient);
        hideMessageBoard();
        pendingConnections.clear();
        connectedEndpoints.clear();
        discoverSwitch.setChecked(false);
        broadcastSwitch.setChecked(false);
    }

    private String getUserNickname() {
        return "Mr. Poopy Butthole";
    }

    private void hideMessageBoard() {
        messageEditText.setVisibility(View.INVISIBLE);
        messageBoard.setVisibility(View.INVISIBLE);
        sendMessageButton.setVisibility(View.INVISIBLE);
    }

    private void showMessageBoard() {
        messageEditText.setVisibility(View.VISIBLE);
        messageBoard.setVisibility(View.VISIBLE);
        sendMessageButton.setVisibility(View.VISIBLE);
    }
}
