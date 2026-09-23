package com.deriv.sniper;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.deriv.sniper.network.DerivAuth;
import com.deriv.sniper.network.DerivWebSocket;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends Activity
        implements DerivWebSocket.Listener,
                   DerivAuth.Listener {

    private TextView connectionStatus;
    private TextView accountStatus;
    private TextView marketsStatus;
    private TextView botStatus;
    private TextView activityStatus;

    private EditText appIdInput;
    private EditText tokenInput;
    private CheckBox demoMode;

    private DerivWebSocket derivWebSocket;
    private DerivAuth derivAuth;

    private boolean authenticated = false;

    private final Set<String> subscribedSymbols =
            new HashSet<>();

    private int nextRequestId = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        derivWebSocket = new DerivWebSocket(this);
        derivAuth = new DerivAuth(this);

        buildDashboard();
    }

    private TextView createText(
            String text,
            int size
    ) {

        TextView view = new TextView(this);

        view.setText(text);
        view.setTextColor(Color.WHITE);
        view.setTextSize(size);
        view.setPadding(20, 20, 20, 20);

        return view;
    }

    private EditText createInput(
            String hint,
            boolean password
    ) {

        EditText input = new EditText(this);

        input.setHint(hint);
        input.setTextColor(Color.WHITE);
        input.setHintTextColor(Color.LTGRAY);
        input.setTextSize(16);
        input.setSingleLine(true);
        input.setPadding(20, 10, 20, 10);

        if (password) {
            input.setInputType(
                    android.text.InputType.TYPE_CLASS_TEXT |
                    android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            );
        }

        return input;
    }

    private Button createButton(String text) {

        Button button = new Button(this);

        button.setText(text);
        button.setTextSize(15);
        button.setAllCaps(false);

        return button;
    }

    private void buildDashboard() {

        ScrollView scroll = new ScrollView(this);

        LinearLayout root = new LinearLayout(this);

        root.setOrientation(
                LinearLayout.VERTICAL
        );

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

        TextView loginTitle = createText(
                "DERIV ACCOUNT",
                20
        );

        loginTitle.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
        );

        root.addView(loginTitle);

        appIdInput = createInput(
                "Deriv App ID",
                false
        );

        root.addView(appIdInput);

        tokenInput = createInput(
                "Deriv PAT / API Token",
                true
        );

        root.addView(tokenInput);

        demoMode = new CheckBox(this);

        demoMode.setText(
                "Use DEMO account"
        );

        demoMode.setTextColor(Color.WHITE);
        demoMode.setTextSize(16);
        demoMode.setChecked(true);

        root.addView(demoMode);

        Button authenticate =
                createButton(
                        "AUTHENTICATE WITH DERIV"
                );

        authenticate.setOnClickListener(
                v -> authenticateWithDeriv()
        );

        root.addView(authenticate);

        connectionStatus = createText(
                "●  DERIV CONNECTION\n" +
                "Disconnected",
                17
        );

        root.addView(connectionStatus);

        accountStatus = createText(
                "ACCOUNT\n\n" +
                "Not authenticated",
                17
        );

        root.addView(accountStatus);

        marketsStatus = createText(
                "MARKETS\n\n" +
                "Waiting for symbol discovery...",
                17
        );

        root.addView(marketsStatus);

        botStatus = createText(
                "BOT STATUS\n\n" +
                "STOPPED",
                17
        );

        root.addView(botStatus);

        activityStatus = createText(
                "TODAY'S ACTIVITY\n\n" +
                "Trades: 0\n" +
                "Wins: 0\n" +
                "Losses: 0\n" +
                "Profit/Loss: 0.00",
                17
        );

        root.addView(activityStatus);

        Button publicConnect =
                createButton(
                        "TEST PUBLIC MARKET CONNECTION"
                );

        publicConnect.setOnClickListener(
                v -> connectToPublicDeriv()
        );

        root.addView(publicConnect);

        Button disconnect =
                createButton(
                        "DISCONNECT"
                );

        disconnect.setOnClickListener(
                v -> disconnectFromDeriv()
        );

        root.addView(disconnect);

        Button start =
                createButton(
                        "START SNIPER BOT"
                );

        start.setOnClickListener(
                v -> startBot()
        );

        root.addView(start);

        Button stop =
                createButton(
                        "STOP BOT"
                );

        stop.setOnClickListener(
                v -> stopBot()
        );

        root.addView(stop);

        scroll.addView(root);

        setContentView(scroll);
    }

    private void connectToPublicDeriv() {

        authenticated = false;
        subscribedSymbols.clear();

        connectionStatus.setText(
                "●  DERIV CONNECTION\n" +
                "Connecting to public market data..."
        );

        marketsStatus.setText(
                "MARKETS\n\n" +
                "Discovering active symbols..."
        );

        derivWebSocket.connect();
    }

    private void authenticateWithDeriv() {

        String appId =
                appIdInput.getText()
                        .toString()
                        .trim();

        String token =
                tokenInput.getText()
                        .toString()
                        .trim();

        if (appId.isEmpty()) {

            connectionStatus.setText(
                    "●  DERIV CONNECTION\n" +
                    "Enter your Deriv App ID"
            );

            return;
        }

        if (token.isEmpty()) {

            connectionStatus.setText(
                    "●  DERIV CONNECTION\n" +
                    "Enter your Deriv PAT"
            );

            return;
        }

        authenticated = false;
        subscribedSymbols.clear();

        connectionStatus.setText(
                "●  DERIV CONNECTION\n" +
                "Authenticating..."
        );

        accountStatus.setText(
                "ACCOUNT\n\n" +
                "Requesting account information..."
        );

        marketsStatus.setText(
                "MARKETS\n\n" +
                "Waiting for authenticated connection..."
        );

        boolean demo =
                demoMode.isChecked();

        derivAuth.authenticate(
                token,
                appId,
                demo
        );
    }

    private void disconnectFromDeriv() {

        authenticated = false;
        subscribedSymbols.clear();

        derivWebSocket.disconnect();

        connectionStatus.setText(
                "●  DERIV CONNECTION\n" +
                "Disconnected"
        );

        accountStatus.setText(
                "ACCOUNT\n\n" +
                "Not connected"
        );

        marketsStatus.setText(
                "MARKETS\n\n" +
                "Waiting for connection..."
        );

        botStatus.setText(
                "BOT STATUS\n\n" +
                "STOPPED"
        );
    }

    private void startBot() {

        if (!authenticated) {

            botStatus.setText(
                    "BOT STATUS\n\n" +
                    "LOGIN REQUIRED"
            );

            return;
        }

        botStatus.setText(
                "BOT STATUS\n\n" +
                "READY — LIVE DATA CONNECTED"
        );
    }

    private void stopBot() {

        botStatus.setText(
                "BOT STATUS\n\n" +
                "STOPPED"
        );
    }

    private void startLiveData() {

        derivWebSocket.requestBalanceSubscription();
        derivWebSocket.requestActiveSymbols();

        marketsStatus.setText(
                "MARKETS\n\n" +
                "Discovering active symbols..."
        );
    }

    private void subscribeToDiscoveredSymbol(
            String symbol
    ) {

        if (symbol == null ||
                symbol.trim().isEmpty()) {
            return;
        }

        if (subscribedSymbols.contains(symbol)) {
            return;
        }

        subscribedSymbols.add(symbol);

        derivWebSocket.subscribeTicks(
                symbol,
                nextRequestId++
        );
    }

    private String findSymbol(
            JSONArray symbols,
            String... terms
    ) {

        try {

            for (int i = 0;
                 i < symbols.length();
                 i++) {

                JSONObject item =
                        symbols.optJSONObject(i);

                if (item == null) {
                    continue;
                }

                String symbol =
                        item.optString(
                                "underlying_symbol",
                                item.optString(
                                        "symbol",
                                        ""
                                )
                        );

                String name =
                        item.optString(
                                "underlying_symbol_name",
                                item.optString(
                                        "display_name",
                                        ""
                                )
                        );

                String combined =
                        (
                                symbol + " " + name
                        ).toLowerCase(
                                Locale.US
                        );

                boolean matches = true;

                for (String term : terms) {

                    if (!combined.contains(
                            term.toLowerCase(
                                    Locale.US
                            )
                    )) {

                        matches = false;
                        break;
                    }
                }

                if (matches) {
                    return symbol;
                }
            }

        } catch (Exception ignored) {
        }

        return null;
    }

    private void processActiveSymbols(
            JSONArray symbols
    ) {

        StringBuilder display =
                new StringBuilder();

        display.append(
                "MARKETS\n\n"
        );

        display.append(
                "Active symbols found: "
        );

        display.append(
                symbols.length()
        );

        display.append("\n\n");

        String gold =
                findSymbol(
                        symbols,
                        "xau"
                );

        if (gold == null) {

            gold =
                    findSymbol(
                            symbols,
                            "gold"
                    );
        }

        String v25 =
                findSymbol(
                        symbols,
                        "volatility",
                        "25"
                );

        String v10 =
                findSymbol(
                        symbols,
                        "volatility",
                        "10"
                );

        String v50 =
                findSymbol(
                        symbols,
                        "volatility",
                        "50"
                );

        display.append(
                "XAU/USD: "
        );

        display.append(
                gold == null
                        ? "Not found"
                        : gold
        );

        display.append("\n");

        display.append(
                "Volatility 25: "
        );

        display.append(
                v25 == null
                        ? "Not found"
                        : v25
        );

        display.append("\n");

        display.append(
                "Volatility 10: "
        );

        display.append(
                v10 == null
                        ? "Not found"
                        : v10
        );

        display.append("\n");

        display.append(
                "Volatility 50: "
        );

        display.append(
                v50 == null
                        ? "Not found"
                        : v50
        );

        display.append(
                "\n\nLIVE TICKS\n"
        );

        marketsStatus.setText(
                display.toString()
        );

        if (gold != null) {
            subscribeToDiscoveredSymbol(gold);
        }

        if (v25 != null) {
            subscribeToDiscoveredSymbol(v25);
        }

        if (v10 != null) {
            subscribeToDiscoveredSymbol(v10);
        }

        if (v50 != null) {
            subscribeToDiscoveredSymbol(v50);
        }
    }

    @Override
    public void onAccounts(
            JSONArray accounts
    ) {

        runOnUiThread(() -> {

            accountStatus.setText(
                    "ACCOUNT\n\n" +
                    "Options accounts found: " +
                    accounts.length()
            );
        });
    }

    @Override
    public void onAuthenticated(
            String wsUrl,
            JSONObject account
    ) {

        runOnUiThread(() -> {

            String accountId =
                    account.optString(
                            "account_id",
                            "Unknown"
                    );

            String accountType =
                    account.optString(
                            "account_type",
                            "Unknown"
                    );

            String currency =
                    account.optString(
                            "currency",
                            "Unknown"
                    );

            accountStatus.setText(
                    "ACCOUNT\n\n" +
                    "ID: " + accountId + "\n" +
                    "Type: " + accountType + "\n" +
                    "Currency: " + currency
            );

            connectionStatus.setText(
                    "●  DERIV CONNECTION\n" +
                    "Opening authenticated connection..."
            );

            derivWebSocket.connectAuthenticated(
                    wsUrl
            );
        });
    }

    @Override
    public void onConnected() {

        runOnUiThread(() -> {

            if (authenticated) {

                connectionStatus.setText(
                        "●  DERIV CONNECTION\n" +
                        "Authenticated and connected"
                );

                startLiveData();

            } else {

                connectionStatus.setText(
                        "●  DERIV CONNECTION\n" +
                        "Public connection established"
                );

                derivWebSocket.requestActiveSymbols();
            }
        });
    }

    @Override
    public void onMessage(
            JSONObject message
    ) {

        runOnUiThread(() -> {

            try {

                String msgType =
                        message.optString(
                                "msg_type",
                                ""
                        );

                if ("authorize".equals(msgType)) {

                    authenticated = true;

                    JSONObject authorize =
                            message.optJSONObject(
                                    "authorize"
                            );

                    if (authorize != null) {

                        String balance =
                                authorize.optString(
                                        "balance",
                                        "Unknown"
                                );

                        String currency =
                                authorize.optString(
                                        "currency",
                                        ""
                                );

                        accountStatus.setText(
                                "ACCOUNT\n\n" +
                                "Balance: " +
                                balance + " " +
                                currency
                        );
                    }

                    connectionStatus.setText(
                            "●  DERIV CONNECTION\n" +
                            "Authenticated and connected"
                    );

                    startLiveData();

                    return;
                }

                if ("balance".equals(msgType)) {

                    JSONObject balance =
                            message.optJSONObject(
                                    "balance"
                            );

                    if (balance != null) {

                        String value =
                                balance.optString(
                                        "balance",
                                        "Unknown"
                                );

                        String currency =
                                balance.optString(
                                        "currency",
                                        ""
                                );

                        accountStatus.setText(
                                "ACCOUNT\n\n" +
                                "LIVE BALANCE\n" +
                                value + " " +
                                currency +
                                "\n\n" +
                                "Balance subscription: ACTIVE"
                        );
                    }

                    return;
                }

                if ("active_symbols".equals(msgType)) {

                    JSONArray symbols =
                            message.optJSONArray(
                                    "active_symbols"
                            );

                    if (symbols != null) {
                        processActiveSymbols(
                                symbols
                        );
                    }

                    return;
                }

                if ("tick".equals(msgType)) {

                    JSONObject tick =
                            message.optJSONObject(
                                    "tick"
                            );

                    if (tick != null) {

                        String symbol =
                                tick.optString(
                                        "symbol",
                                        ""
                                );

                        String quote =
                                tick.optString(
                                        "quote",
                                        ""
                                );

                        String current =
                                marketsStatus
                                        .getText()
                                        .toString();

                        marketsStatus.setText(
                                current +
                                "\n" +
                                symbol +
                                "  →  " +
                                quote
                        );
                    }

                    return;
                }

                if ("error".equals(msgType)) {

                    JSONObject error =
                            message.optJSONObject(
                                    "error"
                            );

                    String errorMessage =
                            error == null
                                    ? "Deriv returned an error"
                                    : error.optString(
                                            "message",
                                            "Unknown Deriv error"
                                    );

                    connectionStatus.setText(
                            "●  DERIV CONNECTION\n" +
                            "Deriv error\n\n" +
                            errorMessage
                    );

                    return;
                }

                connectionStatus.setText(
                        "●  DERIV CONNECTION\n" +
                        "Connected\n\n" +
                        "Server: " + msgType
                );

            } catch (Exception e) {

                connectionStatus.setText(
                        "●  DERIV CONNECTION\n" +
                        "Connected\n\n" +
                        e.getMessage()
                );
            }
        });
    }

    @Override
    public void onDisconnected(
            String reason
    ) {

        authenticated = false;

        runOnUiThread(() ->
                connectionStatus.setText(
                        "●  DERIV CONNECTION\n" +
                        "Disconnected\n\n" +
                        reason
                )
        );
    }

    @Override
    public void onError(
            String error
    ) {

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
