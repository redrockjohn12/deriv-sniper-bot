package com.deriv.sniper.network;

import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class DerivWebSocket {

    public interface Listener {
        void onConnected();
        void onMessage(JSONObject message);
        void onDisconnected(String reason);
        void onError(String error);
    }

    private static final String PUBLIC_WS_URL =
            "wss://api.derivws.com/trading/v1/options/ws/public";

    private final Listener listener;

    private WebSocket webSocket;
    private OkHttpClient client;

    public DerivWebSocket(Listener listener) {
        this.listener = listener;
    }

    public void connect() {
        connectToUrl(PUBLIC_WS_URL);
    }

    public void connectAuthenticated(String wsUrl) {

        if (wsUrl == null || wsUrl.trim().isEmpty()) {

            if (listener != null) {
                listener.onError(
                        "Deriv returned an empty WebSocket URL"
                );
            }

            return;
        }

        connectToUrl(wsUrl);
    }

    private void connectToUrl(String url) {

        disconnect();

        client = new OkHttpClient.Builder()
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .pingInterval(20, TimeUnit.SECONDS)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .build();

        webSocket = client.newWebSocket(
                request,
                new WebSocketListener() {

                    @Override
                    public void onOpen(
                            WebSocket socket,
                            okhttp3.Response response) {

                        if (listener != null) {
                            listener.onConnected();
                        }
                    }

                    @Override
                    public void onMessage(
                            WebSocket socket,
                            String text) {

                        try {

                            JSONObject message =
                                    new JSONObject(text);

                            if (listener != null) {
                                listener.onMessage(message);
                            }

                        } catch (Exception e) {

                            if (listener != null) {
                                listener.onError(
                                        "Invalid Deriv response: "
                                                + e.getMessage()
                                );
                            }
                        }
                    }

                    @Override
                    public void onFailure(
                            WebSocket socket,
                            Throwable throwable,
                            okhttp3.Response response) {

                        String error =
                                throwable.getMessage();

                        if (error == null ||
                                error.trim().isEmpty()) {

                            error =
                                    "WebSocket connection failed";
                        }

                        if (listener != null) {
                            listener.onError(error);
                        }
                    }

                    @Override
                    public void onClosed(
                            WebSocket socket,
                            int code,
                            String reason) {

                        if (listener != null) {
                            listener.onDisconnected(
                                    reason == null
                                            ? "Connection closed"
                                            : reason
                            );
                        }
                    }
                }
        );
    }

    public boolean isConnected() {
        return webSocket != null;
    }

    public void send(JSONObject request) {

        if (webSocket == null) {

            if (listener != null) {
                listener.onError(
                        "Not connected to Deriv"
                );
            }

            return;
        }

        boolean sent =
                webSocket.send(request.toString());

        if (!sent && listener != null) {
            listener.onError(
                    "Failed to send request to Deriv"
            );
        }
    }

    public void requestBalanceSubscription() {

        try {

            JSONObject request = new JSONObject();

            request.put("balance", 1);
            request.put("subscribe", 1);
            request.put("req_id", 1001);

            send(request);

        } catch (Exception e) {

            if (listener != null) {
                listener.onError(
                        "Unable to request balance: "
                                + e.getMessage()
                );
            }
        }
    }

    public void requestActiveSymbols() {

        try {

            JSONObject request = new JSONObject();

            request.put(
                    "active_symbols",
                    "brief"
            );

            request.put("req_id", 1002);

            send(request);

        } catch (Exception e) {

            if (listener != null) {
                listener.onError(
                        "Unable to request symbols: "
                                + e.getMessage()
                );
            }
        }
    }

    public void subscribeTicks(
            String symbol,
            int requestId
    ) {

        if (symbol == null ||
                symbol.trim().isEmpty()) {
            return;
        }

        try {

            JSONObject request =
                    new JSONObject();

            request.put(
                    "ticks",
                    symbol
            );

            request.put(
                    "subscribe",
                    1
            );

            request.put(
                    "req_id",
                    requestId
            );

            send(request);

        } catch (Exception e) {

            if (listener != null) {
                listener.onError(
                        "Unable to subscribe to "
                                + symbol + ": "
                                + e.getMessage()
                );
            }
        }
    }

    public void disconnect() {

        if (webSocket != null) {

            webSocket.close(
                    1000,
                    "User disconnected"
            );

            webSocket = null;
        }

        if (client != null) {

            client.dispatcher()
                    .executorService()
                    .shutdown();

            client = null;
        }
    }
}
