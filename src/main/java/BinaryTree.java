import java.util.concurrent.locks.ReentrantLock;

/* Original: https://arxiv.org/pdf/1702.04441.pdf *
 * "A Concurrency-Optimal Binary Search Tree" (Aksenov, Gramoli, Kuznetsov et al.) */

public class BinaryTree {
    public class Node {
        public volatile int value;
        public volatile State state;
        public volatile Node left;
        public volatile Node right;
        public volatile boolean deleted;

        private ReentrantLock stateReadLock = new ReentrantLock();
        private ReentrantLock stateWriteLock = new ReentrantLock();
        private ReentrantLock leftLock = new ReentrantLock();
        private ReentrantLock rightLock = new ReentrantLock();

        public Node(int value) {
            this.value = value;
            this.state = State.DATA;
            this.left = null;
            this.right = null;
            this.deleted = false;
        }

        public void tryReadLock() {
            while (true) {
                if (stateWriteLock.isLocked()) {
                    continue;
                }
                if (stateReadLock.tryLock()) {
                    if (stateWriteLock.isLocked()) {
                        stateReadLock.unlock();
                        continue;
                    }
                    return;
                }
            }
        }

        public void unlockRead() {
            stateReadLock.unlock();
        }

        public void unlockWrite() {
            stateWriteLock.unlock();
        }

        public void unlockChild(boolean left) {
            if (left) {
                this.leftLock.unlock();
            } else {
                this.rightLock.unlock();
            }
        }

        public boolean tryWriteLockState(State checkedState) {
            if (deleted || this.state != checkedState || stateReadLock.isLocked()) {
                return false;
            }
            if (stateWriteLock.tryLock()) {
                if (deleted || this.state != checkedState || stateReadLock.isLocked()) {
                    this.unlockWrite();
                    return false;
                }
                return true;
            }
            return false;
        }

        public boolean tryReadLockState(State checkedState) {
            if (this.deleted || this.state != checkedState || stateWriteLock.isLocked()) {
                return false;
            }
            if (stateReadLock.tryLock()) {
                if (this.state != checkedState || this.deleted || stateWriteLock.isLocked()) {
                    this.unlockRead();
                    return false;
                } else {
                    return true;
                }
            }
            return false;
        }

        public boolean tryWriteLockRightRef(Node expected) {
            if (this.right != expected || rightLock.isLocked()) {
                return false;
            }
            if (rightLock.tryLock()) {
                if (this.right != expected) {
                    this.unlockChild(false);
                    return false;
                }
                return true;
            }
            return false;
        }

        public boolean tryWriteLockLeftRef(Node expected) {
            if (this.left != expected || leftLock.isLocked()) {
                return false;
            }
            if (leftLock.tryLock()) {
                if (this.left != expected) {
                    this.unlockChild(true);
                    return false;
                }
                return true;
            }
            return false;
        }

        public boolean tryWriteLockRightVal(int val) {
            if (this.right == null || this.right.value != val || rightLock.isLocked()) {
                return false;
            }
            if (rightLock.tryLock()) {
                if (this.right == null || this.right.value != val) {
                    this.unlockChild(false);
                    return false;
                }
                return true;
            }
            return false;
        }

        public boolean tryWriteLockLeftVal(int val) {
            if (this.left == null || this.left.value != val || leftLock.isLocked()) {
                return false;
            }
            if (leftLock.tryLock()) {
                if (this.left == null || this.left.value != val) {
                    this.unlockChild(true);
                    return false;
                }
                return true;
            }
            return false;
        }

        public int numberOfChildren() {
            int num = 0;
            if (left != null) {
                num += 1;
            }
            if (right != null) {
                num += 1;
            }
            return num;
        }
    }

    private Node root = new Node(Integer.MAX_VALUE);

    private Node[] traversal(int v) {
        Node gprev = null;
        Node prev = null;
        Node curr = root;
        while (curr != null) {
            if (curr.value == v) {
                break;
            } else {
                gprev = prev;
                prev = curr;
                if (v < curr.value) {
                    curr = curr.left;
                } else {
                    curr = curr.right;
                }
            }
        }
        return new Node[]{gprev,prev,curr};
    }

    public boolean contains(int v) {
        Node[] results = traversal(v);
        return ((results[2] != null) && (results[2].state == State.DATA));
    }

    public boolean insert(int v) {
        while (true) {
            Node[] results = traversal(v);
            Node curr = results[2];
            Node prev = results[1];
            Node gprev = results[0];
            if (curr != null) {
                if (curr.state == State.DATA) {
                    return false;
                }
                if (curr.tryWriteLockState(State.ROUTING)) {
                    curr.state = State.DATA;
                    curr.unlockWrite();
                    return true;
                }
            } else {
                Node newNode = new Node(v);
                boolean left = (v < prev.value);
                boolean tryLock = (left ? prev.tryWriteLockLeftRef(null) : prev.tryWriteLockRightRef(null));
                if (tryLock) {
                    prev.tryReadLock();
                    boolean deleted = prev.deleted;
                    if (!deleted) {
                        if (left) {
                            prev.left = newNode;
                        } else {
                            prev.right = newNode;
                        }
                        prev.unlockRead();
                        prev.unlockChild(left);
                        return true;
                    } else {
                        prev.unlockRead();
                        prev.unlockChild(left);
                        continue;
                    }
                }
            }
        }
    }

    public void delete(int v) {
        while (true) {
            Node[] results = traversal(v);
            Node curr = results[2];
            Node prev = results[1];
            Node gprev = results[0];
            if (curr == null || curr.state != State.DATA) {
                return;
            }
            int num = curr.numberOfChildren();
            if (num == 2) {
                if (!curr.tryWriteLockState(State.DATA)) {
                    continue;
                }
                if (curr.numberOfChildren() != 2) {
                    curr.unlockWrite();
                    continue;
                }
                curr.state = State.ROUTING;
                curr.unlockWrite();
                return;
            } else if (num == 1) {
                boolean isChildLeft = (curr.left != null);
                Node child = (isChildLeft ? curr.left : curr.right);
                boolean isCurrLeft = (curr.value < prev.value);
                if (!tryWriteLockWithChild(curr, child, isChildLeft)) {
                    continue;
                }
                if (!tryWriteLockWithChild(prev, curr, isCurrLeft)) {
                    curr.unlockChild(isChildLeft);
                    continue;
                }
                if (!curr.tryWriteLockState(State.DATA)) {
                    prev.unlockChild(isCurrLeft);
                    curr.unlockChild(isChildLeft);
                    continue;
                }
                if (curr.numberOfChildren() != 1) {
                    curr.unlockWrite();
                    prev.unlockChild(isCurrLeft);
                    curr.unlockChild(isChildLeft);
                    continue;
                }
                curr.deleted = true;
                if (isCurrLeft) {
                    prev.left = child;
                } else {
                    prev.right = child;
                }
                curr.unlockWrite();
                prev.unlockChild(isCurrLeft);
                curr.unlockChild(isChildLeft);
                return;
            } else {
                boolean isCurrLeft = (curr.value < prev.value);
                if (prev.state == State.DATA) {
                    if (!tryWriteLockWithChildAndValue(prev, curr.value, isCurrLeft)) {
                        continue;
                    }
                    if (isCurrLeft) {
                        curr = prev.left;
                    } else {
                        curr = prev.right;
                    }
                    if (!curr.tryWriteLockState(State.DATA)) {
                        prev.unlockChild(isCurrLeft);
                        continue;
                    }
                    if (curr.numberOfChildren() != 0) {
                        curr.unlockWrite();
                        prev.unlockChild(isCurrLeft);
                        continue;
                    }
                    if (!prev.tryReadLockState(State.DATA)) {
                        curr.unlockWrite();
                        prev.unlockChild(isCurrLeft);
                        continue;
                    }
                    curr.deleted = true;
                    if (isCurrLeft) {
                        prev.left = null;
                    } else {
                        prev.right = null;
                    }

                    prev.unlockRead();
                    curr.unlockWrite();
                    prev.unlockChild(isCurrLeft);
                    return;
                } else {
                    Node child = (isCurrLeft ? prev.right : prev.left);
                    boolean isChildLeft = !isCurrLeft;
                    boolean isPrevLeft = (prev.value < gprev.value);
                    if (!tryWriteLockWithChildAndValue(prev, curr.value, isCurrLeft)) {
                        continue;
                    }
                    if (isCurrLeft) {
                        curr = prev.left;
                    } else {
                        curr = prev.right;
                    }
                    if (!curr.tryWriteLockState(State.DATA)) {
                        prev.unlockChild(isCurrLeft);
                        continue;
                    }
                    if (curr.numberOfChildren() != 0) {
                        curr.unlockWrite();
                        prev.unlockChild(isCurrLeft);
                        continue;
                    }
                    if (!tryWriteLockWithChild(prev, child, isChildLeft)) {
                        curr.unlockWrite();
                        prev.unlockChild(isCurrLeft);
                        continue;
                    }
                    if (!tryWriteLockWithChild(gprev, prev, isPrevLeft)) {
                        prev.unlockChild(isChildLeft);
                        curr.unlockWrite();
                        prev.unlockChild(isCurrLeft);
                        continue;
                    }
                    if (!prev.tryWriteLockState(State.ROUTING)) {
                        gprev.unlockChild(isPrevLeft);
                        prev.unlockChild(isChildLeft);
                        curr.unlockWrite();
                        prev.unlockChild(isCurrLeft);
                        continue;
                    }
                    prev.deleted = true;
                    curr.deleted = true;
                    if (isPrevLeft) {
                        gprev.left = child;
                    } else {
                        gprev.right = child;
                    }
                    prev.unlockWrite();
                    gprev.unlockChild(isPrevLeft);
                    prev.unlockChild(isChildLeft);
                    curr.unlockWrite();
                    prev.unlockChild(isCurrLeft);
                    return;
                }
            }
        }
    }


    private boolean tryWriteLockWithChildAndValue(Node parent, int value, boolean left) {
        if (left) {
            if (!parent.tryWriteLockLeftVal(value)) {
                return false;
            }
        } else {
            if (!parent.tryWriteLockRightVal(value)) {
                return false;
            }
        }
        return true;
    }

    private boolean tryWriteLockWithChild(Node parent, Node child, boolean left) {
        boolean locked = (left ? parent.tryWriteLockLeftRef(child) : parent.tryWriteLockRightRef(child));
        if (parent.deleted) {
            if (locked) {
                parent.unlockChild(left);
            }
            return false;
        }
        return locked;
    }


    private void print(Node node, int lvl) {
        if (node == null) {
            return;
        }
        print(node.left, lvl+1);
        print(node.right, lvl+1);
        for (int i = 0; i < lvl; i++) {
            System.out.print("-");
        }
        if (!node.deleted && node.state == State.DATA) {
            System.out.printf("(%d)\n", node.value);
        } else {
            System.out.printf("x%dx\n", node.value);
        }
    }

    public void printTree() {
        print(root, 0);
        System.out.println();
    }


}
