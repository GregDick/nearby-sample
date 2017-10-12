package com.example.mercury.nearbysample;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApi;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.ResultCallbacks;
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
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.Strategy;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener{

    private static final String TAG = MainActivity.class.getSimpleName();
    private String SERVICE_ID;

    private GoogleApiClient mGoogleApiClient;

    private PayloadCallback mPayloadCallback;

    //region callbacks
    private final ConnectionLifecycleCallback mConnectionLifecycleCallback = new ConnectionLifecycleCallback() {
        @Override
        public void onConnectionInitiated(String endpointId, ConnectionInfo connectionInfo) {
            Log.d(TAG, "onConnectionInitiated: accepting connection");
            Nearby.Connections.acceptConnection(mGoogleApiClient, endpointId, mPayloadCallback);
        }

        @Override
        public void onConnectionResult(String s, ConnectionResolution connectionResolution) {
            switch (connectionResolution.getStatus().getStatusCode()){
                case ConnectionsStatusCodes.STATUS_OK:
                    Log.d(TAG, "onConnectionResult: STATUS_OK. FINALLY CONNECTED");
                    break;

                case ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED:
                    Log.d(TAG, "onConnectionResult: STATUS_CONNECTION_REJECTED");
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
        public void onEndpointFound(String s, DiscoveredEndpointInfo discoveredEndpointInfo) {
            String name = "the discoverer";
            Nearby.Connections.requestConnection(mGoogleApiClient,
                    SERVICE_ID,
                    name,
                    mConnectionLifecycleCallback)
                    .setResultCallback(new ResultCallback<Status>() {
                        @Override
                        public void onResult(@NonNull Status status) {
                            if (status.isSuccess()){
                                Log.d(TAG, "onResult: Success. successfully _requested_ a connection. Still need both parties to accept");
                            }
                            else {
                                Log.e(TAG, "onResult: Failure. failed to request a connection");
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
                .build();

        SERVICE_ID  = getPackageName();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }
    //endregion

    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @OnClick(R.id.broadcast_button)
    void startAdvertising(){
        Nearby.Connections.startAdvertising(mGoogleApiClient,
                SERVICE_ID,
                getUserNickname(),
                mConnectionLifecycleCallback,
                new AdvertisingOptions(Strategy.P2P_CLUSTER))
                .setResultCallback(new ResultCallbacks<Connections.StartAdvertisingResult>() {
                    @Override
                    public void onSuccess(@NonNull Connections.StartAdvertisingResult startAdvertisingResult) {
                        //todo: now we're advertising
                        Log.d(TAG, "startAdvertising onSuccess: " + startAdvertisingResult.toString());
                    }

                    @Override
                    public void onFailure(@NonNull Status status) {
                        Log.e(TAG, "startAdvertising onFailure: " + status.getStatusMessage());
                    }
                });
    }

    @OnClick(R.id.discover_button)
    void startDiscovering() {
        Nearby.Connections.startDiscovery(mGoogleApiClient,
                SERVICE_ID,
                mEndpointDiscoveryCallback,
                new DiscoveryOptions(Strategy.P2P_CLUSTER))
                .setResultCallback(new ResultCallbacks<Status>() {
                    @Override
                    public void onSuccess(@NonNull Status status) {
                        //todo: now we're detectin
                        Log.d(TAG, "startDiscovering onSuccess: " + status.getStatusMessage());
                    }

                    @Override
                    public void onFailure(@NonNull Status status) {
                        Log.e(TAG, "startDiscovering onFailure: " + status.getStatusMessage());
                    }
                });
    }

    private String getUserNickname() {
        return "Mr. Poopy Butthole";
    }
}
