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
                            imprimirVetorParcial(pedido.getNumeros()));

                            long inicio = System.currentTimeMillis();

                            ordenarEmMultithreading(pedido.getNumeros());

                            long fim = System.currentTimeMillis();

                            System.out.println(" → Ordenação concluída em " + (fim - inicio) + " ms");
                            System.out.println(" → Vetor ordenado (resposta): " +
                            imprimirVetorParcial(pedido.getNumeros()));

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

    private static void ordenarEmMultithreading(byte[] numeros) throws InterruptedException {
        int tamanho = numeros.length;
        int numThreads = Runtime.getRuntime().availableProcessors();

        int tamanhoPorThread = tamanho / numThreads;
        int resto = tamanho % numThreads;

        Thread[] threads = new Thread[numThreads];
        byte[][] partes = new byte[numThreads][];

        int inicio = 0;

        for (int i = 0; i < numThreads; i++) {
            final int idx = i;
            final int inicioBloco = inicio;
            final int fimBloco = inicio + tamanhoPorThread + (i < resto ? 1 : 0);
            inicio = fimBloco;

            partes[i] = new byte[fimBloco - inicioBloco];
            System.arraycopy(numeros, inicioBloco, partes[i], 0, partes[i].length);

            threads[i] = new Thread(() -> {
                Pedido p = new Pedido(partes[idx]);
                p.ordenar();    
            });

            threads[i].start();
        }

        for (Thread t : threads) t.join();

        byte[] resultadoFinal = mergeVariosVetores(partes);

        System.arraycopy(resultadoFinal, 0, numeros, 0, numeros.length);
    }

    private static byte[] mergeVariosVetores(byte[][] vetores) {
        while (vetores.length > 1) {
            int novaQtd = (vetores.length + 1) / 2;
            byte[][] novos = new byte[novaQtd][];

            for (int i = 0; i < vetores.length / 2; i++) {
                novos[i] = mergeDoisVetores(vetores[i * 2], vetores[i * 2 + 1]);
            }

            if (vetores.length % 2 != 0) {
                novos[novaQtd - 1] = vetores[vetores.length - 1];
            }

            vetores = novos;
        }
        return vetores[0];
    }

    private static byte[] mergeDoisVetores(byte[] a, byte[] b) {
        byte[] c = new byte[a.length + b.length];
        int i = 0, j = 0, k = 0;

        while (i < a.length && j < b.length) {
            if (a[i] <= b[j]) c[k++] = a[i++];
            else c[k++] = b[j++];
        }

        while (i < a.length) c[k++] = a[i++];
        while (j < b.length) c[k++] = b[j++];

        return c;
    }

    private static String imprimirVetorParcial(byte[] v) {
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
