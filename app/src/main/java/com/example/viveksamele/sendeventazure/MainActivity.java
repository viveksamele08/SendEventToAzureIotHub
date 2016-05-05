package com.example.viveksamele.sendeventazure;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.gson.Gson;
import com.microsoft.azure.iothub.DeviceClient;
import com.microsoft.azure.iothub.IotHubClientProtocol;
import com.microsoft.azure.iothub.IotHubEventCallback;
import com.microsoft.azure.iothub.IotHubStatusCode;
import com.microsoft.azure.iothub.Message;

import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private static String connString = "HostName=<your hub>.azure-devices.net;"
            + "DeviceId=< your device >;"
            + "SharedAccessKey=<your share key >";
    private static IotHubClientProtocol protocol = IotHubClientProtocol.AMQPS;//or HTTPS
    private static boolean stopThread = false;
    private static String DEVICEID = "demodo-myFirstDevice-1";
    private TextView tvLog;
    private Button stopButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvLog = (TextView) findViewById(R.id.consoleLog);
        stopButton = (Button) findViewById(R.id.button);

        final MessageSender messageSender = new MessageSender();
        final Thread t0 = new Thread(messageSender);
        t0.start();

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                messageSender.stopThread = true;
                try {
                    t0.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

    }

    private class MessageSender implements Runnable {
        public volatile boolean stopThread = false;

        public void run() {
            try {
                double avgWindSpeed = 10;
                Random rand = new Random();
                DeviceClient client;

                client = new DeviceClient(connString, protocol);
                client.open();

                while (!stopThread) {
                    double currentWindSpeed = avgWindSpeed + rand.nextDouble() * 4 - 2;
                    TelemetryDataPoint telemetryDataPoint = new TelemetryDataPoint();

                    telemetryDataPoint.deviceId = DEVICEID;
                    telemetryDataPoint.windSpeed = currentWindSpeed;

                    String msgStr = telemetryDataPoint.serialize();
                    Message msg = new Message(msgStr);

                    System.out.println(msgStr);

                    Object lockobj = new Object();
                    EventCallback callback = new EventCallback();

                    client.sendEventAsync(msg, callback, lockobj);

                    synchronized (lockobj) {
                        lockobj.wait();
                    }

                    Thread.sleep(1000);
                }
                client.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static class TelemetryDataPoint {
        public String deviceId;
        public double windSpeed;
        public String deviceOwner = "@vivek samele";

        public String serialize() {
            Gson gson = new Gson();
            return gson.toJson(this);
        }
    }

    protected class EventCallback implements IotHubEventCallback {
        public void execute(IotHubStatusCode statusCode, Object context) {
            tvLog.setText("IoT Hub responded to message with status " + statusCode.name());

            if (context != null) {
                synchronized (context) {
                    context.notify();
                }
            }
        }
    }
}
