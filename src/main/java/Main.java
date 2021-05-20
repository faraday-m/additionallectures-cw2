import java.util.*;
import java.util.concurrent.ThreadPoolExecutor;

import static java.lang.Thread.sleep;

public class Main {
    static Integer[] k;
    static double x = 0.0;
    static volatile boolean isAlive;
    static BinaryTree tree;
    static int[] threads;

    static class Worker implements Runnable {
        Random random = new Random();
        private int id;
        private int ctr = 0;
        private final int bound;
        private int i = 0;
        private final int partition;

        public Worker(int id, int threads) {
            this.id = id;
            this.partition = 50000 / threads;
            this.bound = id * partition;
        }

        @Override
        public void run() {
            while (isAlive) {
                int key = k[bound + random.nextInt(partition)];
                double p = random.nextDouble();
                if (p < x) {
                    tree.insert(key);
                    ctr++;
                } else if ((p >= x) && (p < 2*x)) {
                    tree.delete(key);
                    ctr++;
                } else {
                    tree.contains(key);
                    ctr++;
                }
            }
            threads[id] = ctr;
        }
    }

    public static void main(String[] args) {
        populate();

        heatup();

        System.out.println("procs;x;ops/s");
        for (int numThreads = 1; numThreads <= 4; numThreads++) {
            for (double border : new double[]{0.0,0.1,0.5}) {
                fillTree(numThreads, border);
            }
        }
    }

    private static void populate() {
        Set<Integer> kSet = new HashSet<>();
        Random random = new Random();
        while (kSet.size() < 50000) {
            kSet.add(random.nextInt(100000));
        }
        k = kSet.toArray(new Integer[50000]);
    }

    private static void fillTree(int numThreads, double border) {
        List<Thread> threadPool;
        tree = new BinaryTree();
        x = border;
        threads = new int[4];
        isAlive = true;
        threadPool = new ArrayList<>();
        for (int i = 0; i < numThreads; i++) {
            threadPool.add(new Thread(new Worker(i, numThreads)));
        }
        threadPool.forEach(Thread::start);
        try {
            sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        isAlive = false;
        threadPool.forEach(t -> {
            try {
                t.join();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        System.out.printf("%d;%.1f;%d\n", numThreads, border, Arrays.stream(threads).reduce(0,Integer::sum) / 5);
    }

    private static void heatup() {
        System.out.println("Прогрев . . .");
        tree = new BinaryTree();
        x = 0.2;
        isAlive = true;
        threads = new int[4];
        List<Thread> threadPool = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            threadPool.add(new Thread(new Worker(i,2)));
        }
        threadPool.forEach(Thread::start);
        try {
            sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        isAlive = false;
        threadPool.forEach(t -> {
            try {
                t.join();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
