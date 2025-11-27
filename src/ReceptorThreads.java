import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class ReceptorThreads {

    public static void main(String[] args) {
        int PORTA = 12345;
        System.out.println("Servidor R inicializado. Aguardando conexões na porta " + PORTA);

        try (ServerSocket serverSocket = new ServerSocket(PORTA)) {
            while (true) {
                System.out.println("\n[Aguardando conexão de um cliente...]");
                Socket socket = serverSocket.accept();
                System.out.println("Conexão aceita de: " + socket.getInetAddress().getHostAddress());

                try (ObjectOutputStream transmissor = new ObjectOutputStream(socket.getOutputStream());
                        ObjectInputStream receptor = new ObjectInputStream(socket.getInputStream())) {

                    System.out.println("[R] Streams de comunicação configuradas.");

                    while (true) {
                        System.out.println("\n[R] Aguardando objeto do cliente...");

                        Object obj;
                        try {
                            obj = receptor.readObject();
                        } catch (EOFException e) {
                            System.out.println("[R] Cliente desconectou inesperadamente.");
                            break;
                        }

                        if (obj instanceof Pedido pedido) {
                            System.out.println("[R] Pedido recebido!");
                            System.out.println(" → Vetor recebido com " + pedido.getNumeros().length + " elementos");
                            System.out.println(" → Iniciando ordenação paralela...");

                            System.out.println(" → Início do vetor recebido: " +
                                    formatarVetor(pedido.getNumeros()));

                            int maxThreads = Runtime.getRuntime().availableProcessors();

                            long inicio = System.currentTimeMillis();

                            pedido.ordenar();

                            long fim = System.currentTimeMillis();

                            System.out.println(" → Ordenação concluída em " + (fim - inicio) + " ms");
                            System.out.println(" → Vetor ordenado (resposta): " +
                                    formatarVetor(pedido.getNumeros()));
                            System.out.println(" → Enviando resposta para o cliente...");

                            transmissor.writeObject(new Resposta(pedido.getNumeros()));
                            transmissor.flush();

                            System.out.println("[R] Resposta enviada com sucesso!");

                        } else if (obj instanceof ComunicadoEncerramento) {
                            System.out.println("[R] Cliente solicitou encerramento da conexão.");
                            break;

                        } else {
                            System.out.println("[R] Objeto desconhecido recebido: " + obj.getClass().getSimpleName());
                        }
                    }

                } catch (ClassNotFoundException e) {
                    System.err.println("[R] Classe não reconhecida no objeto recebido: " + e.getMessage());
                } catch (IOException e) {
                    System.err.println("[R] Erro de comunicação com o cliente: " + e.getMessage());
                } finally {
                    try {
                        socket.close();
                        System.out.println("[R] Conexão encerrada com o cliente.");
                    } catch (IOException e) {
                        System.err.println("[R] Erro ao fechar conexão: " + e.getMessage());
                    }
                }
            }

        } catch (IOException e) {
            System.err.println("Erro no ServerSocket: " + e.getMessage());
        }
    }

    private static String formatarVetor(byte[] v) {
        if (v.length <= 10) {
            return java.util.Arrays.toString(v);
        }

        return "[" +
                v[0] + ", " +
                v[1] + ", " +
                v[2] + ", ... " +
                v[v.length - 3] + ", " +
                v[v.length - 2] + ", " +
                v[v.length - 1] + "]";
    }

}
