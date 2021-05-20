import org.junit.Assert;
import org.junit.Test;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class TreeTest {
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
}
