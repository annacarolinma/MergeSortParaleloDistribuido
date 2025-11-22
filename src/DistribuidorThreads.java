import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Arrays;

public class DistribuidorThreads {

    private static final String[] SERVER_IPS = {"192.168.0.154"};
    private static final int PORT = 12345;

    public static void main(String[] args) {

        try {

            System.out.println("==============================================");
            System.out.println("     SISTEMA DE ORDENAÇÃO DISTRIBUÍDA");
            System.out.println("==============================================");

            // ======== CONECTA APENAS UMA VEZ ========
            Socket[] sockets = new Socket[SERVER_IPS.length];
            ObjectOutputStream[] outs = new ObjectOutputStream[SERVER_IPS.length];
            ObjectInputStream[] ins = new ObjectInputStream[SERVER_IPS.length];

            for (int i = 0; i < SERVER_IPS.length; i++) {
                sockets[i] = new Socket(SERVER_IPS[i], PORT);
                outs[i] = new ObjectOutputStream(sockets[i].getOutputStream());
                ins[i] = new ObjectInputStream(sockets[i].getInputStream());
                System.out.println("Conectado ao servidor: " + SERVER_IPS[i]);
            }

            boolean continuar = true;

            // ======== LOOP PRINCIPAL (SEM DESCONECTAR) ========
            while (continuar) {

                System.out.print("\nDigite o tamanho do vetor que deseja ordenar: ");
                int tamanhoVetor = Teclado.getUmInt();

                // Pergunta visualizar vetor
                char opcaoMostrar;
                do {
                    System.out.print("Deseja visualizar o vetor gerado? (s/n): ");
                    opcaoMostrar = Teclado.getUmChar();
                } while (opcaoMostrar != 's' && opcaoMostrar != 'S' &&
                        opcaoMostrar != 'n' && opcaoMostrar != 'N');

                boolean mostrarVetor = (opcaoMostrar == 's' || opcaoMostrar == 'S');

                // Criar vetor
                byte[] vetorGrande = new byte[tamanhoVetor];
                for (int i = 0; i < tamanhoVetor; i++) {
                    vetorGrande[i] = (byte) ((int) (Math.random() * 256) - 128);
                }

                if (mostrarVetor) {
                    System.out.println(Arrays.toString(vetorGrande));
                }

                int numServidores = SERVER_IPS.length;
                int tamanhoParte = vetorGrande.length / numServidores;

                byte[][] respostasParciais = new byte[numServidores][];
                Thread[] threads = new Thread[numServidores];

                long inicioTempo = System.currentTimeMillis();

                // ======== ENVIA PARA SERVIDORES VIA THREADS ========
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

                            System.out.println("Servidor " + SERVER_IPS[idx] + " retornou parte ordenada.");
                        } catch (Exception e) {
                            System.err.println("[ERRO] Comunicação com servidor " + SERVER_IPS[idx] + ": " + e.getMessage());
                        }
                    });

                    threads[i].start();
                }

                // Espera threads terminarem
                for (Thread t : threads) t.join();

                long fimTempo = System.currentTimeMillis();
                double segundos = (fimTempo - inicioTempo) / 1000.0;

                byte[] vetorFinal = mergeVariosVetores(respostasParciais);

                // ======== MOSTRAR RESULTADO COMPLETO ========
                System.out.println("\n============ RESULTADO FINAL ============");
                System.out.println("Tamanho do vetor final: " + vetorFinal.length);
                System.out.println("Tempo total: " + segundos + " segundos");
                System.out.println("==========================================");

                // ======== SALVAR EM ARQUIVO ========
                System.out.print("\nDigite o nome do arquivo para salvar o vetor ordenado (sem extensão): ");
                String nomeArquivo = Teclado.getUmString();

                String caminho = nomeArquivo + ".txt";

                try (java.io.PrintWriter escritor = new java.io.PrintWriter(caminho)) {
                    escritor.println(Arrays.toString(vetorFinal));
                    System.out.println("Arquivo salvo com sucesso em: " + caminho);
                } catch (Exception e) {
                    System.err.println("Erro ao salvar o arquivo: " + e.getMessage());
                }

                // ======== PERGUNTAR SE QUER ORDENAR OUTRO ========
                char resp;
                do {
                    System.out.print("\nDeseja ordenar outro vetor? (s/n): ");
                    resp = Teclado.getUmChar();
                } while (resp != 's' && resp != 'S' && resp != 'n' && resp != 'N');

                continuar = (resp == 's' || resp == 'S');
            }

            // ======== ENCERRA SERVIDORES SOMENTE AQUI ========
            System.out.println("\nEncerrando servidores...");

            for (int i = 0; i < SERVER_IPS.length; i++) {
                outs[i].writeObject(new ComunicadoEncerramento());
                outs[i].flush();
                outs[i].close();
                ins[i].close();
                sockets[i].close();
            }

            System.out.println("Todos os servidores foram encerrados.");
            System.out.println("Programa finalizado!");

        } catch (Exception e) {
            System.err.println("Erro na execução: ");
            e.printStackTrace();
        }
    }

    // =============================================
    // FUNÇÕES DE MERGE
    // =============================================
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
}
