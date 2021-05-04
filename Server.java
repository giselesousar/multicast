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
    public static final int PORT_SERVER_GROUP = 8888;
    public static final int PORT_CLIENT = 4444;

    /**
     * @param serverId inteiro identificador do servidor, recebido como
     * parâmetro no momento da execução
     * @return true se o identificador for o menor entre os armazenados na
     * Stack de servidores em execução e false caso contrário
     */
    public static boolean shouldResponse(int serverId) {
        Iterator<Integer> value = serversAlive.iterator();
        while (value.hasNext()) {
            if (value.next() < serverId)
                return false;
        }
        return true;
    }

    static class SenderThread implements Runnable {

        private int id;

        public SenderThread(int serverId) {
            this.id = serverId;
        }

        public void run() {
            DatagramSocket socket = null;
            DatagramPacket outPacket = null;
            byte[] message_bytes;
            
            /** mensagem a ser enviada via Multicast IP para o grupo de servidores */
            String message = "" + id;

            try {
                socket = new DatagramSocket();
                InetAddress group = InetAddress.getByName("224.0.0.0");

                while (true) {
                    message_bytes = message.getBytes();

                    outPacket = new DatagramPacket(message_bytes, message_bytes.length, group, PORT_SERVER_GROUP);
                    socket.send(outPacket);

                    /** tempo de espera para reenviar a mensagem */
                    TimeUnit.MILLISECONDS.sleep(1000);
                }
            } catch (Exception e) {
            }
        }
    }

    public static void listenToServerGroup() {
        MulticastSocket socket = null;
        DatagramPacket inPacket = null;
        byte[] inBuffer = new byte[256];

        try {
            socket = new MulticastSocket(PORT_SERVER_GROUP);
            InetAddress group = InetAddress.getByName("224.0.0.0");

            /** tempo, em milissegundos, em que o socket espera por mensagens */
            socket.setSoTimeout(500);

            socket.joinGroup(group);

            while (true) {
                try {
                    inPacket = new DatagramPacket(inBuffer, inBuffer.length);
                    socket.receive(inPacket);
                    String received = new String(inPacket.getData());
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
            System.out.println(e.getMessage());
        }
    }

    public static void main(String args[]) {

        if (args.length == 0) {
            System.out.println("Por favor, informe o identificador do servidor como argumento!");
            System.exit(1);
        }

        int serverId = Integer.parseInt(args[0]);

        SenderThread sender = new SenderThread(serverId);
        Thread sender_thread = new Thread(sender);
        sender_thread.start();

        MulticastSocket socket = null;
        DatagramPacket inPacket = null;
        DatagramPacket outPacket = null;

        DatagramSocket ClientSocket;
        InetAddress ClientAddress;

        byte[] outBuffer;
        ScriptEngineManager mgr = new ScriptEngineManager();
        ScriptEngine engine = mgr.getEngineByName("JavaScript");
        String result;

        try {
            socket = new MulticastSocket(PORT_CLIENT);
            socket.setLoopbackMode​(true);
            InetAddress group = InetAddress.getByName("224.0.0.0");
            socket.joinGroup(group);

            ClientSocket = new DatagramSocket();

            while (true) {
                byte[] inBuffer = new byte[256];
                inPacket = new DatagramPacket(inBuffer, inBuffer.length);
                socket.receive(inPacket);
                String received = new String(inPacket.getData());

                System.out.println("Expressão recebida do cliente: " + received.trim());

                listenToServerGroup();

                if (shouldResponse(serverId)) {
                    result = engine.eval(received.trim()) + "";
                    outBuffer = result.getBytes();

                    /** envio do resultado diretamente para o cliente */
                    ClientAddress = InetAddress.getByName(String.valueOf(inPacket.getAddress()).substring(1));
                    outPacket = new DatagramPacket(outBuffer, outBuffer.length, ClientAddress, PORT_CLIENT);

                    ClientSocket.send(outPacket);
                    System.out.println("O resultado da expressão é: " + result);
                } else {
                    System.out.println("Não devo responder...");
                }

                serversAlive.clear();
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

    }

}
