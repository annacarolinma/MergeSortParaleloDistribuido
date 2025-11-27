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

                            byte[] vet = pedido.getNumeros();
                            int n = vet.length;

                            System.out.println("[R] Pedido recebido!");
                            System.out.println(" → Vetor recebido com " + n + " elementos");
                            System.out.println(" → Início do vetor recebido: " + formatarVetor(vet));
                            System.out.println(" → Iniciando ordenação paralela...");

                            long inicio = System.currentTimeMillis();

                            // ----------------------------------------------------------------------------------------------------
                            // PARALELIZAÇÃO AQUI!
                            // ----------------------------------------------------------------------------------------------------
                            int nThreads = Runtime.getRuntime().availableProcessors();
                            Thread[] threads = new Thread[nThreads];

                            int base = n / nThreads;
                            int resto = n % nThreads;

                            int inicioBloco = 0;

                            for (int i = 0; i < nThreads; i++) {

                                int tamanho = base + (i < resto ? 1 : 0);
                                int ini = inicioBloco;
                                int fim = ini + tamanho - 1;

                                inicioBloco += tamanho;

                                threads[i] = new Thread(() -> pedido.ordenar(ini, fim));
                                threads[i].start();
                            }

                            for (Thread t : threads)
                                t.join();

                            // Merge final das partes ordenadas
                            mergeFinal(vet, nThreads);
                            // ----------------------------------------------------------------------------------------------------

                            long fim = System.currentTimeMillis();

                            System.out.println(" → Ordenação concluída em " + (fim - inicio) + " ms");
                            System.out.println(" → Vetor ordenado (resposta): " + formatarVetor(vet));
                            System.out.println(" → Enviando resposta para o cliente...");

                            transmissor.writeObject(new Resposta(vet));
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
                } catch (InterruptedException e) {
                    System.err.println("[R] Erro nas threads de ordenação: " + e.getMessage());
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

    // -------------------------------------------------------------------------
    // MÉTODO DE MERGE FINAL DAS PARTES
    // -------------------------------------------------------------------------
    private static void mergeFinal(byte[] v, int nPartes) {
        int parte = v.length / nPartes;

        for (int i = 1; i < nPartes; i++) {
            int ini = 0;
            int meio = parte * i - 1;
            int fim = (i == nPartes - 1) ? v.length - 1 : meio + parte;

            merge(v, ini, meio, fim);
        }
    }

    // Merge clássico
    private static void merge(byte[] v, int ini, int meio, int fim) {
        int i = ini;
        int j = meio + 1;
        int k = 0;

        byte[] aux = new byte[fim - ini + 1];

        while (i <= meio && j <= fim) {
            aux[k++] = (v[i] <= v[j]) ? v[i++] : v[j++];
        }

        while (i <= meio)
            aux[k++] = v[i++];

        while (j <= fim)
            aux[k++] = v[j++];

        for (int x = 0; x < aux.length; x++)
            v[ini + x] = aux[x];
    }

    // Apenas para exibir um pedaço do vetor
    private static String formatarVetor(byte[] v) {
        if (v.length <= 10)
            return java.util.Arrays.toString(v);

        return "[" + v[0] + ", " + v[1] + ", " + v[2] + ", ... "
                + v[v.length - 3] + ", " + v[v.length - 2] + ", " + v[v.length - 1] + "]";
    }
}
