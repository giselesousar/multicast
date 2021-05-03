import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Stack;
import java.util.concurrent.TimeUnit;
import javax.script.ScriptEngineManager;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import java.math.*;

public class Server {

    public static Stack<Integer> serversAlive = new Stack<Integer>();

    public static boolean shouldResponse(int serverId) {
        Iterator<Integer> value = serversAlive.iterator();
        while (value.hasNext()) {
            if (value.next() < serverId)
                return false;
        }
        return true;
    }

    static class SenderGroup implements Runnable {
        private int id;

        public SenderGroup(int serverId) {
            this.id = serverId;
        }

        public void run() {
            DatagramSocket socket = null;
            DatagramPacket outPacket = null;
            byte[] msg;
            final int PORT = 8888;

            String message = "" + id;
            try {
                socket = new DatagramSocket();
                InetAddress group = InetAddress.getByName("224.0.0.0");

                while (true) {
                    msg = message.getBytes();

                    outPacket = new DatagramPacket(msg, msg.length, group, PORT);
                    socket.send(outPacket);

                    try {
                        TimeUnit.SECONDS.sleep(1);
                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                    }

                }
            } catch (Exception e) {
            }
        }
    }

    // static class ReceiveGroup implements Runnable {
    // MulticastSocket socket = null;
    // DatagramPacket inPacket = null;
    // byte[] inBuffer = new byte[256];
    // ScriptEngineManager mgr = new ScriptEngineManager();
    // ScriptEngine engine = mgr.getEngineByName("JavaScript");

    // final int PORT = 8888;

    // public void run() {

    // try {
    // socket = new MulticastSocket(PORT);
    // InetAddress group = InetAddress.getByName("224.0.0.0");
    // socket.joinGroup(group);
    // while (true) {
    // inPacket = new DatagramPacket(inBuffer, inBuffer.length);
    // socket.receive(inPacket);
    // String received = new String(inPacket.getData());
    // System.out.println("De: " + inPacket.getAddress() + " mgs: " +
    // received.trim());
    // serversAlive.push(Integer.parseInt(received.trim()));
    // }
    // } catch (Exception e) {
    // System.out.println("Linha 82: " + e.getMessage());
    // }
    // }
    // }

    public static void listenToServerGroup() {
        MulticastSocket socket = null;
        DatagramPacket inPacket = null;
        byte[] inBuffer = new byte[256];

        final int PORT = 8888;
        try {
            socket = new MulticastSocket(PORT);
            InetAddress group = InetAddress.getByName("224.0.0.0");
            socket.setSoTimeout(500);
            socket.joinGroup(group);
            while (true) {
                try {
                    inPacket = new DatagramPacket(inBuffer, inBuffer.length);
                    socket.receive(inPacket);
                    String received = new String(inPacket.getData());
                    System.out.println("De: " + inPacket.getAddress() + " mgs: " + received.trim());
                    serversAlive.push(Integer.parseInt(received.trim()));
                } catch (SocketTimeoutException s) {
                    if (serversAlive.size() <= 0) {
                        System.out.println("Sem resposta do grupo de servidores. Tente novamente");
                    }
                    break;
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
            socket.close();

        } catch (Exception e) {
            // erro
        }
    }

    public static void main(String args[]) {

        if (args.length == 0) {
            System.out.println("Informe o identificador do servidor como argumento");
            System.exit(1);
        }

        int serverId = Integer.parseInt(args[0]);

        SenderGroup sender = new SenderGroup(serverId);
        // ReceiveGroup receive = new ReceiveGroup();
        Thread t1 = new Thread(sender);
        // Thread t2 = new Thread(receive);
        t1.start();
        // t2.start();

        MulticastSocket socket = null;
        DatagramPacket inPacket = null;
        DatagramPacket outPacket = null;

        DatagramSocket ClientSocket;
        InetAddress ClientAddress;

        byte[] inBuffer = new byte[256];
        byte[] outBuffer;
        ScriptEngineManager mgr = new ScriptEngineManager();
        ScriptEngine engine = mgr.getEngineByName("JavaScript");
        String result;
        final int PORT = 4444;

        try {
            socket = new MulticastSocket(PORT);
            socket.setLoopbackMode​(true);
            InetAddress group = InetAddress.getByName("224.0.0.0");
            socket.joinGroup(group);

            ClientSocket = new DatagramSocket();

            while (true) {
                inPacket = new DatagramPacket(inBuffer, inBuffer.length);
                socket.receive(inPacket);
                String received = new String(inPacket.getData());

                listenToServerGroup();
                System.out.println("serversAlive: " + serversAlive);

                if (shouldResponse(serverId)) {
                    result = engine.eval("Math.round(" + received.trim() + ")") + "";
                    outBuffer = result.getBytes();

                    ClientAddress = InetAddress.getByName(String.valueOf(inPacket.getAddress()));
                    outPacket = new DatagramPacket(outBuffer, outBuffer.length, ClientAddress, PORT);

                    ClientSocket.send(outPacket);
                    System.out.println("Respondendo para cliente: " + result);
                } else {
                    System.out.println("Não devo responder...");
                }

                serversAlive.clear();
            }
        } catch (Exception e) {
            System.out.println("Linha 138: " + e.getMessage());
        }

    }

}
