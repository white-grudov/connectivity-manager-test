package com.example.connectivity_manager_test;

import androidx.appcompat.app.AppCompatActivity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.os.StrictMode;
import android.text.Html;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

public class MainActivity extends AppCompatActivity {

    private TextView networkInfoTextView;
    private EditText addressInput;
    private String address;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        networkInfoTextView = findViewById(R.id.network_info_text_view);
        addressInput = findViewById(R.id.address_input);
        Button startButton = findViewById(R.id.start_button);

        // Allow network operations on the main thread for simplicity
        // In production code, use an AsyncTask or similar background thread mechanism
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        startButton.setOnClickListener(v -> {
            address = addressInput.getText().toString().trim();
            if (!address.isEmpty()) {
                displayNetworkInfo();
            } else {
                Toast.makeText(MainActivity.this, "Please enter a valid address", Toast.LENGTH_SHORT).show();
            }
        });

        networkInfoTextView.setOnLongClickListener(v -> {
            copyTextToClipboard();
            return true;
        });
    }

    private void displayNetworkInfo() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        StringBuilder info = new StringBuilder();

        for (Network network : connectivityManager.getAllNetworks()) {
            NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(network);
            LinkProperties linkProperties = connectivityManager.getLinkProperties(network);

            if (networkCapabilities != null && linkProperties != null) {
                String interfaceName = linkProperties.getInterfaceName();

                if ("wlan0".equals(interfaceName) || "eth0".equals(interfaceName)) {
                    info.append("<b>Interface:</b> ").append(interfaceName).append("<br>");
                    info.append("<b>Network Capabilities:</b> ").append(networkCapabilities).append("<br>");
                    info.append("<b>Link Properties:</b> ").append(linkProperties).append("<br>");

                    boolean canAccessAddress = canAccessAddress(network);
                    info.append("<b>Can access ").append(address).append(":</b> ").append(canAccessAddress).append("<br><br>");
                }
            }
        }

        networkInfoTextView.setText(Html.fromHtml(info.toString(), Html.FROM_HTML_MODE_LEGACY));
    }

    private boolean canAccessAddress(Network network) {
        Socket socket = null;
        try {
            socket = network.getSocketFactory().createSocket();
            SocketAddress socketAddress = new InetSocketAddress(address, 80);
            socket.connect(socketAddress, 5000); // 5 seconds timeout
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void copyTextToClipboard() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Network Info", networkInfoTextView.getText());
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, "Text copied to clipboard", Toast.LENGTH_SHORT).show();
    }
}
