import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Arrays;

public class DistribuidorThreads {

    private static final String[] SERVER_IPS = {"26.83.104.38", "26.206.202.218"};
    private static final int PORT = 12345;

    public static void main(String[] args) {
        try {
            int tamanhoVetor = 1_000_000; // ou pergunte ao usuário
            byte[] vetorGrande = new byte[tamanhoVetor];

            // Preenche com valores aleatórios entre -128 e 127
            for (int i = 0; i < tamanhoVetor; i++) {
                vetorGrande[i] = (byte) ((int) (Math.random() * 256) - 128);
            }

            int numServidores = SERVER_IPS.length;
            int tamanhoParte = vetorGrande.length / numServidores;

            Socket[] sockets = new Socket[numServidores];
            ObjectOutputStream[] outs = new ObjectOutputStream[numServidores];
            ObjectInputStream[] ins = new ObjectInputStream[numServidores];

            for (int i = 0; i < numServidores; i++) {
                sockets[i] = new Socket(SERVER_IPS[i], PORT);
                outs[i] = new ObjectOutputStream(sockets[i].getOutputStream());
                ins[i] = new ObjectInputStream(sockets[i].getInputStream());
                System.out.println("Conectado ao servidor: " + SERVER_IPS[i]);
            }

            // Thread para cada servidor
            byte[][] respostasParciais = new byte[numServidores][];
            Thread[] threads = new Thread[numServidores];

            for (int i = 0; i < numServidores; i++) {
                int inicio = i * tamanhoParte;
                int fim = (i == numServidores - 1) ? vetorGrande.length : (i + 1) * tamanhoParte;
                byte[] parteVetor = Arrays.copyOfRange(vetorGrande, inicio, fim);
                int idx = i;

                threads[i] = new Thread(() -> {
                    try {
                        Pedido pedido = new Pedido(parteVetor);
                        outs[idx].writeObject(pedido);
                        outs[idx].flush();

                        Resposta resposta = (Resposta) ins[idx].readObject();
                        respostasParciais[idx] = resposta.getVetor();
                        System.out.println("Servidor " + SERVER_IPS[idx] + " respondeu com vetor de tamanho: " + respostasParciais[idx].length);

                    } catch (Exception e) {
                        System.err.println("[ERRO] Comunicação com servidor " + SERVER_IPS[idx] + ": " + e.getMessage());
                    }
                });

                threads[i].start();
            }

            // Espera todas as threads terminarem
            for (Thread t : threads) t.join();

            // Faz merge dos vetores recebidos
            byte[] resultadoFinal = mergeVariosVetores(respostasParciais);

            System.out.println("Merge final concluído. Vetor final tamanho: " + resultadoFinal.length);
            System.out.println("Exemplo início do vetor: " + Arrays.toString(Arrays.copyOfRange(resultadoFinal, 0, Math.min(10, resultadoFinal.length))));

            // Envia comunicado de encerramento
            for (int i = 0; i < numServidores; i++) {
                outs[i].writeObject(new ComunicadoEncerramento());
                outs[i].flush();
                sockets[i].close();
                System.out.println("Conexão com " + SERVER_IPS[i] + " encerrada.");
            }

        } catch (Exception e) {
            System.err.println("Erro na execução: " + e.getMessage());
        }
    }

    // -------------------------------
    // Função para fazer merge de vários vetores ordenados
    // -------------------------------
    private static byte[] mergeVariosVetores(byte[][] vetores) {
        while (vetores.length > 1) {
            int novaQtd = (vetores.length + 1) / 2;
            byte[][] novosVetores = new byte[novaQtd][];

            for (int i = 0; i < vetores.length / 2; i++) {
                novosVetores[i] = mergeDoisVetores(vetores[i * 2], vetores[i * 2 + 1]);
            }

            if (vetores.length % 2 != 0) {
                novosVetores[novaQtd - 1] = vetores[vetores.length - 1];
            }

            vetores = novosVetores;
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
}
