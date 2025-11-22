import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class ReceptorThreads {

    public static void main(String[] args) {
        int PORTA = 12345;
        System.out.println("Servidor R inicializado. Aguardando conexões na porta " + PORTA);

        try (ServerSocket serverSocket = new ServerSocket(PORTA)) {
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Conexão aceita de: " + socket.getInetAddress().getHostAddress());

                try (ObjectOutputStream transmissor = new ObjectOutputStream(socket.getOutputStream());
                     ObjectInputStream receptor = new ObjectInputStream(socket.getInputStream())) {

                    while (true) {
                        Object obj;
                        try {
                            obj = receptor.readObject();
                        } catch (EOFException e) {
                            System.out.println("[R] Cliente desconectou inesperadamente.");
                            break;
                        }

                        if (obj instanceof Pedido pedido) {
                            System.out.println("[R] Pedido recebido. Vetor tamanho: " + pedido.getNumeros().length);

                            // Ordena o vetor usando o método paralelo da classe Pedido
                            pedido.ordenar();

                            // Envia o vetor ordenado de volta
                            transmissor.writeObject(new Resposta(pedido.getNumeros()));
                            transmissor.flush();
                            System.out.println("[R] Resposta enviada. Vetor ordenado.");

                        } else if (obj instanceof ComunicadoEncerramento) {
                            System.out.println("[R] Cliente encerrou a conexão.");
                            break;
                        } else {
                            System.out.println("[R] Objeto desconhecido recebido: " + obj.getClass().getSimpleName());
                        }
                    }

                } catch (ClassNotFoundException e) {
                    System.err.println("[R] Classe não reconhecida: " + e.getMessage());
                } catch (IOException e) {
                    System.err.println("[R] Erro na comunicação: " + e.getMessage());
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
}
