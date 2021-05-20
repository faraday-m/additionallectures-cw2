import org.junit.Assert;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.fail;

public class TreeTest {
    public class TestWorker implements Runnable {
        private BinaryTree tree;
        private Collection<Integer> elements;
        private boolean insert;

        public TestWorker(BinaryTree tree, Collection<Integer> elements, boolean insert) {
            this.tree = tree;
            this.elements = elements;
            this.insert = insert;
        }

        @Override
        public void run() {
            for (int i : elements) {
                if (insert) {
                    tree.insert(i);
                } else {
                    tree.delete(i);
                }
            }
        }
    }

    @Test
    public void sequentialTreeInsertTest() {
        BinaryTree tree = new BinaryTree();
        int[] data = new int[1_000_000];
        Random random = new Random();
        for (int i = 0; i < 1_000_000; i++) {
            data[i] = random.nextInt(500_000);
        }
        for (int i : data) {
            tree.insert(i);
        }
        for (int i : data) {
            Assert.assertTrue(tree.contains(i));
        }
    }


    @Test
    public void sequentialTreeInsertAndDeleteTest() {
        BinaryTree tree = new BinaryTree();
        Set<Integer> data = new HashSet<>();
        Set<Integer> delete = new HashSet<>();
        Random random = new Random();
        for (int i = 0; i < 10_000; i++) {
            data.add(random.nextInt(3000));
        }
        int size = data.size();
        for (int i = 0; i < 3_000; i++) {
            delete.add(random.nextInt(3000));
        }
        for (int i : data) {
            tree.insert(i);
        }
        for (int i : data) {
            Assert.assertTrue(tree.contains(i));
        }
        for (int i : delete) {
            tree.delete(i);
        }
        data.removeAll(delete);
        Assert.assertNotEquals(size, data.size());
        for (int i : delete) {
            Assert.assertFalse(tree.contains(i));
        }
        for (int i : data) {
            Assert.assertTrue(tree.contains(i));
        }
    }

    @Test
    public void falsePositiveContainsTest() {
        BinaryTree tree = new BinaryTree();
        Random random = new Random();
        for (int i = 0; i < 1_000; i++) {
            tree.insert(random.nextInt(1_000));
        }
        Assert.assertFalse(tree.contains(10001));
    }


    @Test
    public void parallelTreeTest() {
        BinaryTree tree = new BinaryTree();

        List<Integer> data1 = new ArrayList<>();
        List<Integer> data2 = new ArrayList<>();
        List<Integer> delete1 = new ArrayList<>();
        List<Integer> delete2 = new ArrayList<>();

        for (int i = 0; i < 1_000_000; i++) {
            if (i % 2 == 0) {
                data1.add(i);
                if (i % 3 == 0) {
                    delete1.add(i);
                }
            } else {
                data2.add(i);
                if (i % 3 == 0) {
                    delete1.add(i);
                }
            }
        }
        Collections.shuffle(data1);
        Collections.shuffle(data2);
        Thread thread1 = new Thread(new TestWorker(tree, data1, true));
        Thread thread2 = new Thread(new TestWorker(tree, data2, true));
        thread1.start();
        thread2.start();
        try {
            thread1.join();
            thread2.join();
        } catch (InterruptedException e) {
            fail();
        }
        for (int i = 0; i < 1_000_000; i++) {
            Assert.assertTrue(tree.contains(i));
        }

         thread1 = new Thread(new TestWorker(tree, delete1, false));
         thread2 = new Thread(new TestWorker(tree, delete2, false));
        thread1.start();
        thread2.start();
        try {
            thread1.join();
            thread2.join();
        } catch (InterruptedException e) {
            fail();
        }
        for (int i = 0; i < 1_000_000; i++) {
            if (i % 3 == 0) {
                Assert.assertFalse(tree.contains(i));
            } else {
                Assert.assertTrue(tree.contains(i));
            }
        }

    }
}
