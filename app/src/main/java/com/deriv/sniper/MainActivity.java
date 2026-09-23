package com.deriv.sniper;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.deriv.sniper.network.DerivWebSocket;

import org.json.JSONObject;

public class MainActivity extends Activity
        implements DerivWebSocket.Listener {

    private TextView connectionStatus;
    private TextView botStatus;

    private DerivWebSocket derivWebSocket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        buildDashboard();

        derivWebSocket = new DerivWebSocket(this);
    }

    private TextView createText(String text, int size) {

        TextView view = new TextView(this);

        view.setText(text);
        view.setTextColor(Color.WHITE);
        view.setTextSize(size);
        view.setPadding(20, 20, 20, 20);

        return view;
    }

    private void buildDashboard() {

        ScrollView scroll = new ScrollView(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 30, 24, 30);
        root.setBackgroundColor(
                Color.rgb(17, 24, 39)
        );

        TextView title = createText(
                "DERIV SNIPER BOT",
                28
        );

        title.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
        );

        title.setGravity(Gravity.CENTER);

        TextView subtitle = createText(
                "Android Automated Trading System",
                15
        );

        subtitle.setGravity(Gravity.CENTER);

        root.addView(title);
        root.addView(subtitle);

        connectionStatus = createText(
                "●  DERIV CONNECTION\n" +
                "Disconnected",
                17
        );

        root.addView(connectionStatus);

        TextView markets = createText(
                "MARKETS\n\n" +
                "XAUUSD       Waiting\n" +
                "Volatility 25 (1s)   Waiting\n" +
                "Volatility 10        Waiting\n" +
                "Volatility 50        Waiting",
                17
        );

        root.addView(markets);

        TextView balance = createText(
                "ACCOUNT BALANCE\n\n" +
                "Not connected",
                17
        );

        root.addView(balance);

        botStatus = createText(
                "BOT STATUS\n\n" +
                "STOPPED",
                17
        );

        root.addView(botStatus);

        TextView activity = createText(
                "TODAY'S ACTIVITY\n\n" +
                "Trades: 0\n" +
                "Wins: 0\n" +
                "Losses: 0\n" +
                "Profit/Loss: 0.00",
                17
        );

        root.addView(activity);

        Button connect = new Button(this);

        connect.setText(
                "CONNECT TO DERIV"
        );

        connect.setOnClickListener(
                v -> connectToDeriv()
        );

        root.addView(connect);

        Button disconnect = new Button(this);

        disconnect.setText(
                "DISCONNECT"
        );

        disconnect.setOnClickListener(
                v -> disconnectFromDeriv()
        );

        root.addView(disconnect);

        Button start = new Button(this);

        start.setText(
                "START SNIPER BOT"
        );

        start.setOnClickListener(
                v -> botStatus.setText(
                        "BOT STATUS\n\n" +
                        "READY — TRADING ENGINE NOT ENABLED YET"
                )
        );

        root.addView(start);

        Button stop = new Button(this);

        stop.setText(
                "STOP BOT"
        );

        stop.setOnClickListener(
                v -> botStatus.setText(
                        "BOT STATUS\n\n" +
                        "STOPPED"
                )
        );

        root.addView(stop);

        scroll.addView(root);

        setContentView(scroll);
    }

    private void connectToDeriv() {

        connectionStatus.setText(
                "●  DERIV CONNECTION\n" +
                "Connecting..."
        );

        derivWebSocket.connect();
    }

    private void disconnectFromDeriv() {

        derivWebSocket.disconnect();

        connectionStatus.setText(
                "●  DERIV CONNECTION\n" +
                "Disconnected"
        );
    }

    @Override
    public void onConnected() {

        runOnUiThread(() ->
                connectionStatus.setText(
                        "●  DERIV CONNECTION\n" +
                        "Connected"
                )
        );
    }

    @Override
    public void onMessage(JSONObject message) {

        runOnUiThread(() ->
                connectionStatus.setText(
                        "●  DERIV CONNECTION\n" +
                        "Connected\n\n" +
                        "Server response received"
                )
        );
    }

    @Override
    public void onDisconnected(String reason) {

        runOnUiThread(() ->
                connectionStatus.setText(
                        "●  DERIV CONNECTION\n" +
                        "Disconnected\n\n" +
                        reason
                )
        );
    }

    @Override
    public void onError(String error) {

        runOnUiThread(() ->
                connectionStatus.setText(
                        "●  DERIV CONNECTION\n" +
                        "Connection error\n\n" +
                        error
                )
        );
    }

    @Override
    protected void onDestroy() {

        if (derivWebSocket != null) {
            derivWebSocket.disconnect();
        }

        super.onDestroy();
    }
}
