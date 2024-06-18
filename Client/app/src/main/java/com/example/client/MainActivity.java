package com.example.client;

import androidx.appcompat.app.AppCompatActivity;

import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class MainActivity extends AppCompatActivity {

    Thread Thread1 = null;
    EditText etIP, etPort;
    TextView tvMessages;
    Boolean connected = false;
    EditText etMessage;
    Button btnSend;
    String SERVER_IP;
    int SERVER_PORT;
    OutputStream output;
    InputStream input;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etIP = findViewById(R.id.etIP);
        etPort = findViewById(R.id.etPort);
        tvMessages = findViewById(R.id.tvMessages);
//        etMessage = findViewById(R.id.etMessage);
//        btnSend = findViewById(R.id.btnSend);


        Button btnConnect = findViewById(R.id.btnConnect);

        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tvMessages.setText("");
                SERVER_IP = etIP.getText().toString().trim();
                SERVER_PORT = Integer.parseInt(etPort.getText().toString().trim());
                connectToServer();
                btnConnect.setEnabled(false);

            }
        });

//        btnSend.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                String message = etMessage.getText().toString().trim();
//                if (!message.isEmpty()) {
//                    sendMessageToServer(message);
//                }
//            }
//        });
    }

    private void connectToServer() {
        Thread1 = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Socket socket = new Socket(SERVER_IP, SERVER_PORT);
                    output = socket.getOutputStream();
                    input = socket.getInputStream();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tvMessages.setText("Connected ");
                            connected = true;
                        }
                    });

                    // Start listening for incoming files from the server
                    receiveFileFromServer(socket);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        Thread1.start();
    }

//    private void sendMessageToServer(final String message) {
//        Thread thread = new Thread(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    OutputStreamWriter writer = new OutputStreamWriter(output, "UTF-8");
//                    writer.write(message + "\n");
//                    writer.flush();
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            tvMessages.append("client: " + message + "\n");
//                            etMessage.setText("");
//                        }
//                    });
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        });
//        thread.start();
//    }

    private void sendMessageToServer(final String message) {
        if(message != ""){
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (output != null) {
                            OutputStreamWriter writer = new OutputStreamWriter(output, "UTF-8");
                            writer.write(message + "\n");
                            writer.flush();
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    tvMessages.append("client: " + message + "\n");
//                                etMessage.setText("");
                                }
                            });
                        } else {
                            Log.e("CLIENT", "Output stream is null. Message not sent: " + message);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            thread.start();
        }
    }


    private void receiveFileFromServer(Socket socket) {
        try {
            // Set up input streams
            DataInputStream dis = new DataInputStream(new BufferedInputStream(socket.getInputStream()));

            // Receive number of files
            int numberOfFiles = dis.readInt();
            Log.d("CLIENT", "Number of files to receive: " + numberOfFiles);

            // Receive each file
            for (int i = 0; i < numberOfFiles; i++) {
                long fileSize = dis.readLong();
                String fileName = dis.readUTF();
                Log.d("CLIENT", "Receiving file: " + fileName + ", size: " + fileSize);

                // Check if the received file is 'testing.txt'
//                if (!fileName.equals("testing.txt")) {
//                    Log.e("CLIENT", "Received unexpected file: " + fileName);
//                    continue; // Skip to the next file
//                }

                // Set up output streams for file writing
                File directory = getExternalFilesDir(null); // Adjust as per your file storage requirements
                if (directory == null) {
                    Log.e("CLIENT", "Failed to get external files directory.");
                    return;
                }
                File fileToUpdate = new File(directory, "testing.txt");
                FileOutputStream fos = new FileOutputStream(fileToUpdate);

                // Read file data from input stream and write to file
                byte[] buffer = new byte[4096];
                int bytesRead;
                long totalBytesRead = 0;
                while (totalBytesRead < fileSize && (bytesRead = dis.read(buffer, 0, (int) Math.min(buffer.length, fileSize - totalBytesRead))) != -1) {
                    fos.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;
                }

                // Close file output stream
                fos.close();
                Log.d("CLIENT", "File received and updated: " + fileToUpdate.getAbsolutePath());
                WordCount wordCount = new WordCount();
                int ans = wordCount.countWords(fileToUpdate.getAbsolutePath());

                // Display a message on UI thread that file has been received and updated
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tvMessages.append("Received and updated file: " + fileToUpdate.getAbsolutePath() + "\n");
                        tvMessages.append("Word Count is:" + ans + " \n");
                        sendMessageToServer("Word Count is" + ans + " \n");

                    }
                });
            }

            // Close input stream and socket
//            dis.close();
//            socket.close();

        } catch (IOException e) {
            e.printStackTrace();
            Log.e("CLIENT", "Error receiving file: " + e.getMessage());
        }
    }

}
