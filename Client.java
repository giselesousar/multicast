import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.concurrent.TimeUnit;
import java.util.Scanner;
import java.lang.Math;

public class Client {

    public static class ExpressionGenerator {
        public static String generate(int tam) {

            String exp = new String("");

            for (int i = 0; i < tam; i++) {
                if (i % 2 == 0) {
                    int random_int = (int) Math.floor(Math.random() * (10) + 1);
                    exp = exp.concat(Integer.toString(random_int));
                } else {
                    int random_int = (int) Math.floor(Math.random() * (4) + 1);
                    String randomChar = "";
                    if (random_int == 1) {
                        randomChar = "+";
                    } else if (random_int == 2) {
                        randomChar = "-";
                    } else if (random_int == 3) {
                        randomChar = "*";
                    } else if (random_int == 4) {
                        randomChar = "/";
                    }

                    exp = exp.concat(randomChar);
                }
            }

            return exp;
        }
    }

    public static void main(String args[]) {
        while (true) {
            try {

                //criação da expressão númerica
                String msg = new String(ExpressionGenerator.generate(7));
                System.out.println(msg);


                //criação do socket
                InetAddress group = InetAddress.getByName("224.0.0.0");
                MulticastSocket s = new MulticastSocket(4444);

                //evita que o nó receba mensagem dele mesmo
                s.setLoopbackMode​(true);

                s.joinGroup(group);

                //criação do datagrama que será enviado
                DatagramPacket hi = new DatagramPacket(msg.getBytes(), msg.length(), group, 4444);
                s.send(hi);

                //loop para receber mensagens do grupo de servidores
                while (true) {
                    try {
                        byte[] buf = new byte[1000];
                        DatagramPacket recv = new DatagramPacket(buf, buf.length);
                        s.receive(recv);

                        TimeUnit.SECONDS.sleep(1);
                        String received = new String(recv.getData());
                        System.out.println(received);
                        break;
                    } catch (Exception e) {

                    }
                }

                s.leaveGroup(group);
                s.close();
            } catch (Exception e) {
                // erro
            }
        }
    }
}