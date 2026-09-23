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

    private static final String DERIV_WS_URL =
            "wss://ws.derivws.com/websockets/v3";

    private final Listener listener;
    private WebSocket webSocket;
    private OkHttpClient client;

    public DerivWebSocket(Listener listener) {
        this.listener = listener;
    }

    public void connect() {

        client = new OkHttpClient.Builder()
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .build();

        Request request = new Request.Builder()
                .url(DERIV_WS_URL)
                .build();

        webSocket = client.newWebSocket(request, new WebSocketListener() {

            @Override
            public void onOpen(WebSocket socket,
                               okhttp3.Response response) {

                if (listener != null) {
                    listener.onConnected();
                }
            }

            @Override
            public void onMessage(WebSocket socket,
                                   String text) {

                try {
                    JSONObject message = new JSONObject(text);

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
            public void onFailure(WebSocket socket,
                                  Throwable t,
                                  okhttp3.Response response) {

                if (listener != null) {
                    listener.onError(
                            t.getMessage() != null
                                    ? t.getMessage()
                                    : "WebSocket connection failed"
                    );
                }
            }

            @Override
            public void onClosed(WebSocket socket,
                                 int code,
                                 String reason) {

                if (listener != null) {
                    listener.onDisconnected(reason);
                }
            }
        });
    }

    public void send(JSONObject request) {

        if (webSocket != null) {
            webSocket.send(request.toString());
        }
    }

    public void disconnect() {

        if (webSocket != null) {
            webSocket.close(1000, "User disconnected");
            webSocket = null;
        }

        if (client != null) {
            client.dispatcher().executorService().shutdown();
            client = null;
        }
    }
}
