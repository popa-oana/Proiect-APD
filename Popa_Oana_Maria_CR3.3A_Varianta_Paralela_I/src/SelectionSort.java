import java.io.*;
import java.util.*;

public class SelectionSort {

    static class SelectionSortThread extends Thread {
        private int[] arr;
        private int start, end;

        public SelectionSortThread(int[] arr, int start, int end) {
            this.arr = arr;
            this.start = start;
            this.end = end;
        }

        @Override
        public void run() {
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
    }

    public static void mergeSortedSections(int[] arr, int mid) {
        int[] left = Arrays.copyOfRange(arr, 0, mid);
        int[] right = Arrays.copyOfRange(arr, mid, arr.length);
        int i = 0, j = 0, k = 0;

        while (i < left.length && j < right.length) {
            arr[k++] = (left[i] < right[j]) ? left[i++] : right[j++];
        }
        while (i < left.length) arr[k++] = left[i++];
        while (j < right.length) arr[k++] = right[j++];
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

    public static int[] readArrayFromFile(String filename) {
        List<Integer> list = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                list.add(Integer.parseInt(line.trim()));
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + filename);
        }
        return list.stream().mapToInt(i -> i).toArray();
    }

    public static void main(String[] args) {
        String inputFile = "unsorted.txt";
        int[] data = readArrayFromFile(inputFile);

        System.out.println("Sorting " + data.length + " elements using multi-threaded Selection Sort...");

        long startTime = System.currentTimeMillis();

        int mid = data.length / 2;

        SelectionSortThread thread1 = new SelectionSortThread(data, 0, mid);
        SelectionSortThread thread2 = new SelectionSortThread(data, mid, data.length);

        thread1.start();
        thread2.start();

        try {
            thread1.join();
            thread2.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Mergem cele două părți sortate
        mergeSortedSections(data, mid);

        long endTime = System.currentTimeMillis();
        System.out.println("Sorting completed in " + (endTime - startTime) / 1000.0 + " seconds.");

        System.out.println("Saving sorted array...");
        saveArrayToFile(data, "sorted.txt");
        System.out.println("Files saved: sorted.txt");
    }
}
