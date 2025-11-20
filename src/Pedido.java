import java.io.Serializable;
import java.util.concurrent.*;

public class Pedido extends Comunicado implements Serializable {
    private byte[] numeros;

    public Pedido(byte[] numeros) {
        this.numeros = numeros;
    }

    public byte[] getNumeros() {
        return numeros;
    }

    // -------------------------------
    // MÉTODO ORDENAR - Merge Sort Paralelo
    // -------------------------------
    public void ordenar() {
        if (numeros == null || numeros.length <= 1)
            return;

        int maxThreads = Runtime.getRuntime().availableProcessors();
        ForkJoinPool pool = new ForkJoinPool(maxThreads);
        pool.invoke(new MergeSortTask(numeros, 0, numeros.length - 1));
        pool.shutdown();
    }

    // -------------------------------
    // TAREFA RECURSIVA PARA MERGE SORT
    // -------------------------------
    private static class MergeSortTask extends RecursiveAction {
        private final byte[] vetor;
        private final int inicio, fim;

        MergeSortTask(byte[] vetor, int inicio, int fim) {
            this.vetor = vetor;
            this.inicio = inicio;
            this.fim = fim;
        }

        @Override
        protected void compute() {
            if (inicio >= fim) return;

            int meio = (inicio + fim) / 2;

            MergeSortTask esquerda = new MergeSortTask(vetor, inicio, meio);
            MergeSortTask direita = new MergeSortTask(vetor, meio + 1, fim);

            invokeAll(esquerda, direita); // executa as duas em paralelo

            merge(vetor, inicio, meio, fim); // junta os resultados
        }

        private void merge(byte[] v, int ini, int meio, int fim) {
            int tam1 = meio - ini + 1;
            int tam2 = fim - meio;

            byte[] esq = new byte[tam1];
            byte[] dir = new byte[tam2];

            for (int i = 0; i < tam1; i++) esq[i] = v[ini + i];
            for (int j = 0; j < tam2; j++) dir[j] = v[meio + 1 + j];

            int i = 0, j = 0, k = ini;

            while (i < tam1 && j < tam2) {
                if (esq[i] <= dir[j]) v[k++] = esq[i++];
                else v[k++] = dir[j++];
            }

            while (i < tam1) v[k++] = esq[i++];
            while (j < tam2) v[k++] = dir[j++];
        }
    }
}
