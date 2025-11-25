package com.example.datastructures;

import java.util.Random;

/**
 * A probabilistic skip list that provides expected logarithmic time operations.
 */
public class SkipList<T extends Comparable<T>> {
    private static final double P = 0.5;
    private static final int MAX_LEVEL = 16;

    private final Node<T> head = new Node<>(null, MAX_LEVEL);
    private int level = 1;
    private final Random random = new Random(42L);

    public boolean contains(T value) {
        Node<T> current = head;
        for (int i = level - 1; i >= 0; i--) {
            while (current.forward[i] != null && current.forward[i].value.compareTo(value) < 0) {
                current = current.forward[i];
            }
        }
        current = current.forward[0];
        return current != null && current.value.compareTo(value) == 0;
    }

    public void insert(T value) {
        Node<T>[] update = new Node[MAX_LEVEL];
        Node<T> current = head;
        for (int i = level - 1; i >= 0; i--) {
            while (current.forward[i] != null && current.forward[i].value.compareTo(value) < 0) {
                current = current.forward[i];
            }
            update[i] = current;
        }
        current = current.forward[0];
        if (current != null && current.value.compareTo(value) == 0) {
            return; // already present
        }
        int newLevel = randomLevel();
        if (newLevel > level) {
            for (int i = level; i < newLevel; i++) {
                update[i] = head;
            }
            level = newLevel;
        }
        Node<T> newNode = new Node<>(value, newLevel);
        for (int i = 0; i < newLevel; i++) {
            newNode.forward[i] = update[i].forward[i];
            update[i].forward[i] = newNode;
        }
    }

    public boolean delete(T value) {
        Node<T>[] update = new Node[MAX_LEVEL];
        Node<T> current = head;
        for (int i = level - 1; i >= 0; i--) {
            while (current.forward[i] != null && current.forward[i].value.compareTo(value) < 0) {
                current = current.forward[i];
            }
            update[i] = current;
        }
        current = current.forward[0];
        if (current == null || current.value.compareTo(value) != 0) {
            return false;
        }
        for (int i = 0; i < level; i++) {
            if (update[i].forward[i] != current) {
                break;
            }
            update[i].forward[i] = current.forward[i];
        }
        while (level > 1 && head.forward[level - 1] == null) {
            level--;
        }
        return true;
    }

    private int randomLevel() {
        int lvl = 1;
        while (lvl < MAX_LEVEL && random.nextDouble() < P) {
            lvl++;
        }
        return lvl;
    }

    private static class Node<T> {
        final T value;
        final Node<T>[] forward;

        @SuppressWarnings("unchecked")
        Node(T value, int level) {
            this.value = value;
            this.forward = (Node<T>[]) new Node[level];
        }
    }
}
