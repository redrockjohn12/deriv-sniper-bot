package com.deriv.sniper.network;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DerivAuth {

    private static final String BASE_URL =
            "https://api.derivws.com";

    public interface Listener {
        void onAccounts(JSONArray accounts);
        void onAuthenticated(String wsUrl, JSONObject account);
        void onError(String error);
    }

    private final Listener listener;
    private final OkHttpClient client;

    public DerivAuth(Listener listener) {
        this.listener = listener;

        client = new OkHttpClient.Builder()
                .build();
    }

    public void authenticate(
            String token,
            String appId,
            boolean demo
    ) {

        new Thread(() -> {

            try {

                Request accountsRequest =
                        new Request.Builder()
                                .url(BASE_URL +
                                        "/trading/v1/options/accounts")
                                .get()
                                .addHeader(
                                        "Authorization",
                                        "Bearer " + token
                                )
                                .addHeader(
                                        "Deriv-App-ID",
                                        appId
                                )
                                .build();

                Response accountsResponse =
                        client.newCall(accountsRequest).execute();

                if (!accountsResponse.isSuccessful()) {
                    throw new IOException(
                            "Account request failed: HTTP "
                                    + accountsResponse.code()
                    );
                }

                String accountsBody =
                        accountsResponse.body().string();

                JSONObject accountsJson =
                        new JSONObject(accountsBody);

                JSONArray accounts =
                        accountsJson.optJSONArray("data");

                if (accounts == null ||
                        accounts.length() == 0) {

                    throw new IOException(
                            "No Options trading accounts found"
                    );
                }

                if (listener != null) {
                    listener.onAccounts(accounts);
                }

                JSONObject selected = null;

                for (int i = 0;
                     i < accounts.length();
                     i++) {

                    JSONObject account =
                            accounts.getJSONObject(i);

                    String type =
                            account.optString(
                                    "account_type"
                            );

                    if (demo && "demo".equalsIgnoreCase(type)) {
                        selected = account;
                        break;
                    }

                    if (!demo &&
                            "real".equalsIgnoreCase(type)) {
                        selected = account;
                        break;
                    }
                }

                if (selected == null) {
                    throw new IOException(
                            demo
                                    ? "No demo Options account found"
                                    : "No real Options account found"
                    );
                }

                String accountId =
                        selected.getString("account_id");

                Request otpRequest =
                        new Request.Builder()
                                .url(
                                        BASE_URL +
                                        "/trading/v1/options/accounts/" +
                                        accountId +
                                        "/otp"
                                )
                                .post(
                                        okhttp3.RequestBody.create(
                                                new byte[0],
                                                null
                                        )
                                )
                                .addHeader(
                                        "Authorization",
                                        "Bearer " + token
                                )
                                .addHeader(
                                        "Deriv-App-ID",
                                        appId
                                )
                                .build();

                Response otpResponse =
                        client.newCall(otpRequest).execute();

                if (!otpResponse.isSuccessful()) {
                    throw new IOException(
                            "OTP request failed: HTTP "
                                    + otpResponse.code()
                    );
                }

                String otpBody =
                        otpResponse.body().string();

                JSONObject otpJson =
                        new JSONObject(otpBody);

                JSONObject data =
                        otpJson.getJSONObject("data");

                String wsUrl =
                        data.getString("url");

                if (listener != null) {
                    listener.onAuthenticated(
                            wsUrl,
                            selected
                    );
                }

            } catch (Exception e) {

                if (listener != null) {
                    listener.onError(
                            e.getMessage() != null
                                    ? e.getMessage()
                                    : "Authentication failed"
                    );
                }
            }

        }).start();
    }
}
