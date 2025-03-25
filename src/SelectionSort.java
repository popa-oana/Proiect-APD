import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

public class SelectionSort {
    public static void selectionSort(int[] arr) {
        int n = arr.length;
        for (int i = 0; i < n - 1; i++) {
            int minIndex = i;
            for (int j = i + 1; j < n; j++) {
                if (arr[j] < arr[minIndex]) {
                    minIndex = j;
                }
            }
            int temp = arr[minIndex];
            arr[minIndex] = arr[i];
            arr[i] = temp;
        }
    }

    public static void saveArrayToFile(int[] arr, String filename) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            for (int num : arr) {
                writer.write(num + "\n");
            }
        } catch (IOException e) {
            System.err.println("Error writing to file: " + filename);
        }
    }

    public static void main(String[] args) {
        int size = 1000000;
        int[] data = generateRandomArray(size);

        System.out.println("Saving unsorted array...");
        saveArrayToFile(data, "unsorted.txt");

        System.out.println("Sorting " + size + " elements using Selection Sort...");
        long startTime = System.currentTimeMillis();

        selectionSort(data);

        long endTime = System.currentTimeMillis();
        System.out.println("Sorting completed in " + (endTime - startTime) / 1000.0 + " seconds.");

        System.out.println("Saving sorted array...");
        saveArrayToFile(data, "sorted.txt");
        System.out.println("Files saved: unsorted.txt, sorted.txt");
    }

    public static int[] generateRandomArray(int size) {
        Random rand = new Random();
        int[] array = new int[size];
        for (int i = 0; i < size; i++) {
            array[i] = rand.nextInt();
        }
        return array;
    }
}
