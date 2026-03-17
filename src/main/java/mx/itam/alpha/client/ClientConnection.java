package mx.itam.alpha.client;

import mx.itam.alpha.common.model.TcpRequest;
import mx.itam.alpha.common.model.TcpResponse;
import mx.itam.alpha.common.util.JsonUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ClientConnection implements Closeable {

    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;

    public void connect(String host, int port) throws IOException {
        close();
        socket = new Socket(host, port);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
    }

    public synchronized TcpResponse send(TcpRequest request) throws IOException {
        ensureConnected();
        writer.write(JsonUtils.toJson(request));
        writer.newLine();
        writer.flush();
        String line = reader.readLine();
        if (line == null) {
            throw new IOException("El servidor cerró la conexión");
        }
        return JsonUtils.fromJson(line, TcpResponse.class);
    }

    private void ensureConnected() throws IOException {
        if (socket == null || socket.isClosed()) {
            throw new IOException("No hay conexión TCP activa");
        }
    }

    @Override
    public void close() throws IOException {
        IOException failure = null;
        if (reader != null) {
            try {
                reader.close();
            } catch (IOException exception) {
                failure = exception;
            } finally {
                reader = null;
            }
        }
        if (writer != null) {
            try {
                writer.close();
            } catch (IOException exception) {
                if (failure == null) {
                    failure = exception;
                }
            } finally {
                writer = null;
            }
        }
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException exception) {
                if (failure == null) {
                    failure = exception;
                }
            } finally {
                socket = null;
            }
        }
        if (failure != null) {
            throw failure;
        }
    }
}
