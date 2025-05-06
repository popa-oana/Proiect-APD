import java.io.*;
import java.util.*;

public class MultiThreadedSelectionSort {

    static class SelectionSortThread extends Thread {
        private int[] array;
        private int start, end;
        private int threadId;

        public SelectionSortThread(int[] array, int start, int end, int threadId) {
            this.array = array;
            this.start = start;
            this.end = end;
            this.threadId = threadId;
        }

        @Override
        public void run() {
            try {
                System.out.println("Thread " + threadId + " started, sorting section: [" + start + ", " + end + ")");
                for (int i = start; i < end - 1; i++) {
                    int minIndex = i;
                    for (int j = i + 1; j < end; j++) {
                        if (array[j] < array[minIndex]) {
                            minIndex = j;
                        }
                    }
                    int temp = array[minIndex];
                    array[minIndex] = array[i];
                    array[i] = temp;
                }
                System.out.println("Thread " + threadId + " finished sorting section: [" + start + ", " + end + ")");
            } catch (Exception e) {
                System.err.println("Error in thread " + threadId + ": " + e.getMessage());
            }
        }
    }

    static class Element {
        int sectionIndex;
        int value;

        public Element(int sectionIndex, int value) {
            this.sectionIndex = sectionIndex;
            this.value = value;
        }
    }

    public static void mergeSortedSections(int[] array, int[] sectionBoundaries) {
        int[][] sortedSections = new int[sectionBoundaries.length - 1][];

        for (int i = 0; i < sectionBoundaries.length - 1; i++) {
            sortedSections[i] = Arrays.copyOfRange(array, sectionBoundaries[i], sectionBoundaries[i + 1]);
        }

        int[] indices = new int[sortedSections.length];
        PriorityQueue<Element> minHeap = new PriorityQueue<>(Comparator.comparingInt(e -> e.value));

        for (int i = 0; i < sortedSections.length; i++) {
            if (sortedSections[i].length > 0) {
                minHeap.offer(new Element(i, sortedSections[i][0]));
            }
        }

        int index = 0;
        while (!minHeap.isEmpty()) {
            Element smallest = minHeap.poll();
            array[index++] = smallest.value;
            indices[smallest.sectionIndex]++;

            if (indices[smallest.sectionIndex] < sortedSections[smallest.sectionIndex].length) {
                int nextValue = sortedSections[smallest.sectionIndex][indices[smallest.sectionIndex]];
                minHeap.offer(new Element(smallest.sectionIndex, nextValue));
            }
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
                    System.err.println("Invalid line skipped: " + line);
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + filename);
        }
        return list.stream().mapToInt(i -> i).toArray();
    }

    public static void saveArrayToFile(int[] array, String filename) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            for (int num : array) {
                writer.write(num + "\n");
            }
        } catch (IOException e) {
            System.err.println("Error writing to file: " + filename);
        }
    }

    public static void main(String[] args) {
        String inputFile = "unsorted.txt";
        String outputFile = "sorted.txt";

        int[] data = readArrayFromFile(inputFile);

        if (data.length == 0) {
            System.out.println("Input file is empty or invalid.");
            return;
        }

        int numberOfThreads = 4;
        int length = data.length;
        int chunkSize = (int) Math.ceil(length * 1.0 / numberOfThreads);
        int[] boundaries = new int[numberOfThreads + 1];

        for (int i = 0; i <= numberOfThreads; i++) {
            boundaries[i] = Math.min(i * chunkSize, length);
        }

        SelectionSortThread[] threads = new SelectionSortThread[numberOfThreads];

        System.out.println("Number of threads to be used: " + numberOfThreads);
        System.out.println("Total numbers to be sorted: " + length);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < numberOfThreads; i++) {
            threads[i] = new SelectionSortThread(data, boundaries[i], boundaries[i + 1], i + 1); // Thread IDs start from 1
            threads[i].start();
        }

        for (int i = 0; i < numberOfThreads; i++) {
            try {
                threads[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        mergeSortedSections(data, boundaries);

        long endTime = System.currentTimeMillis();
        System.out.printf("Sorting completed in %.3f seconds.%n", (endTime - startTime) / 1000.0);

        saveArrayToFile(data, outputFile);
        System.out.println("Sorted data saved to: " + outputFile);
    }
}
