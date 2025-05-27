import mpi.MPI;
import java.io.*;
import java.util.*;

public class SelectionSortMPI {

    // Sortare selection sort pe subarray [start, end)
    public static void selectionSort(int[] arr, int start, int end) {
        for (int i = start; i < end - 1; i++) {
            int minIndex = i;
            for (int j = i + 1; j < end; j++) {
                if (arr[j] < arr[minIndex]) {
                    minIndex = j;
                }
            }
            int temp = arr[minIndex];
            arr[minIndex] = arr[i];
            arr[i] = temp;
        }
    }

    // Merge k secțiuni sortate (din array 2D) într-un array final sortat
    public static int[] mergeSortedSections(int[][] sections) {
        int totalLength = 0;
        for (int[] sec : sections) totalLength += sec.length;

        int[] indices = new int[sections.length];
        PriorityQueue<Element> minHeap = new PriorityQueue<>(Comparator.comparingInt(e -> e.value));
        int[] result = new int[totalLength];

        // Inițializează heap cu primul element din fiecare secțiune
        for (int i = 0; i < sections.length; i++) {
            if (sections[i].length > 0)
                minHeap.offer(new Element(i, sections[i][0]));
        }

        int idx = 0;
        while (!minHeap.isEmpty()) {
            Element el = minHeap.poll();
            result[idx++] = el.value;
            indices[el.sectionIndex]++;
            if (indices[el.sectionIndex] < sections[el.sectionIndex].length) {
                minHeap.offer(new Element(el.sectionIndex, sections[el.sectionIndex][indices[el.sectionIndex]]));
            }
        }
        return result;
    }

    static class Element {
        int sectionIndex, value;

        Element(int sectionIndex, int value) {
            this.sectionIndex = sectionIndex;
            this.value = value;
        }
    }

    public static int[] readArrayFromFile(String filename) {
        List<Integer> list = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    list.add(Integer.parseInt(line.trim()));
                } catch (NumberFormatException e) {
                    System.err.println("Linie ignorata (invalid): " + line);
                }
            }
        } catch (IOException e) {
            System.err.println("Eroare citire fisier: " + filename);
        }
        return list.stream().mapToInt(i -> i).toArray();
    }

    public static void saveArrayToFile(int[] array, String filename) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            for (int num : array) {
                writer.write(num + "\n");
            }
        } catch (IOException e) {
            System.err.println("Eroare scriere fisier: " + filename);
        }
    }

    public static void main(String[] args) throws Exception {
        MPI.Init(args);

        int rank = MPI.COMM_WORLD.Rank();
        int numProcs = MPI.COMM_WORLD.Size();

        int[] fullArray = null;
        int length = 0;

        if (rank == 0) {
            fullArray = readArrayFromFile("unsorted.txt");
            length = fullArray.length;

            if (length == 0) {
                System.out.println("Fisierul este gol sau invalid.");
                MPI.Finalize();
                return;
            }

            System.out.println("Elemente de sortat: " + length);
            System.out.println("Procese folosite: " + numProcs);
        }

        // Trimitem dimensiunea array-ului catre toate procesele
        int[] lengthBuffer = new int[1];
        if (rank == 0) lengthBuffer[0] = length;
        MPI.COMM_WORLD.Bcast(lengthBuffer, 0, 1, MPI.INT, 0);
        length = lengthBuffer[0];

        // Calculam dimensiunea chunk-ului pt fiecare proces
        int chunkSize = (int) Math.ceil((double) length / numProcs);

        // Buffer pt fiecare proces (dimensiune chunkSize, completat cu 0 daca depaseste length)
        int[] subArray = new int[chunkSize];
        Arrays.fill(subArray, Integer.MAX_VALUE); // valori mari pt umplere la final

        // Pregatim buffer de scatter (doar la rank 0)
        int[] scatterBuffer = null;
        if (rank == 0) {
            scatterBuffer = new int[chunkSize * numProcs];
            Arrays.fill(scatterBuffer, Integer.MAX_VALUE);
            System.arraycopy(fullArray, 0, scatterBuffer, 0, length);
        }

        // Scatter cu padding
        MPI.COMM_WORLD.Scatter(scatterBuffer, 0, chunkSize, MPI.INT, subArray, 0, chunkSize, MPI.INT, 0);

        MPI.COMM_WORLD.Barrier();
        double startSort = MPI.Wtime();

        // Sortare locală pe subArray
        selectionSort(subArray, 0, chunkSize);

        MPI.COMM_WORLD.Barrier();

        // Gather (procesul 0 adună toate bucățile)
        int[] gatherBuffer = null;
        if (rank == 0) {
            gatherBuffer = new int[chunkSize * numProcs];
        }

        MPI.COMM_WORLD.Gather(subArray, 0, chunkSize, MPI.INT, gatherBuffer, 0, chunkSize, MPI.INT, 0);

        if (rank == 0) {
            // Re-împărțim în secțiuni sortate și eliminăm valorile max (padding)
            int[][] sortedSections = new int[numProcs][];
            for (int i = 0; i < numProcs; i++) {
                int startIdx = i * chunkSize;
                int endIdx = Math.min(startIdx + chunkSize, length);
                sortedSections[i] = Arrays.copyOfRange(gatherBuffer, startIdx, startIdx + chunkSize);

                // Eliminam paddingul (Integer.MAX_VALUE)
                sortedSections[i] = Arrays.stream(sortedSections[i])
                        .filter(x -> x != Integer.MAX_VALUE)
                        .toArray();
            }

            // Merge final
            int[] sorted = mergeSortedSections(sortedSections);

            double endSort = MPI.Wtime();

            System.out.printf("Sortare + merge paralel in %.3f secunde.%n", (endSort - startSort));

            saveArrayToFile(sorted, "sorted.txt");
            System.out.println("Rezultatul sortarii a fost salvat in 'sorted.txt'");
        }

        MPI.Finalize();
    }
}