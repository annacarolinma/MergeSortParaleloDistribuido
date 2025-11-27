import java.io.Serializable;

public class Pedido extends Comunicado implements Serializable {
    private byte[] numeros;

    public Pedido(byte[] numeros) {
        this.numeros = numeros;
    }

    public byte[] getNumeros() {
        return numeros;
    }

    // ---------------------------------------------------
    // Método ordenar(): Merge Sort sequencial
    // ---------------------------------------------------
    public void ordenar() {
        if (numeros == null || numeros.length <= 1)
            return;

        mergeSort(0, numeros.length - 1);
    }

    private void mergeSort(int inicio, int fim) {
        if (inicio >= fim) return;

        int meio = (inicio + fim) / 2;

        mergeSort(inicio, meio);
        mergeSort(meio + 1, fim);

        merge(inicio, meio, fim);
    }

    private void merge(int ini, int meio, int fim) {
        int tam1 = meio - ini + 1;
        int tam2 = fim - meio;

        byte[] esq = new byte[tam1];
        byte[] dir = new byte[tam2];

        for (int i = 0; i < tam1; i++) esq[i] = numeros[ini + i];
        for (int j = 0; j < tam2; j++) dir[j] = numeros[meio + 1 + j];

        int i = 0, j = 0, k = ini;

        while (i < tam1 && j < tam2) {
            if (esq[i] <= dir[j]) numeros[k++] = esq[i++];
            else numeros[k++] = dir[j++];
        }

        while (i < tam1) numeros[k++] = esq[i++];
        while (j < tam2) numeros[k++] = dir[j++];
    }
}
