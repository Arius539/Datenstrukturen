package com.example.datastructures;

import java.util.ArrayList;
import java.util.List;

/**
 * A generic B-Tree supporting search and insert.
 * @param <T> comparable type for stored keys
 */
public class BTree<T extends Comparable<T>> {
    private final int minDegree;
    private Node<T> root;

    public BTree(int minDegree) {
        if (minDegree < 2) {
            throw new IllegalArgumentException("Minimum degree must be at least 2");
        }
        this.minDegree = minDegree;
        this.root = new Node<>(true);
    }

    public boolean search(T key) {
        return search(root, key) != null;
    }

    private Node<T> search(Node<T> node, T key) {
        int i = 0;
        while (i < node.keys.size() && key.compareTo(node.keys.get(i)) > 0) {
            i++;
        }
        if (i < node.keys.size() && key.compareTo(node.keys.get(i)) == 0) {
            return node;
        }
        if (node.leaf) {
            return null;
        }
        return search(node.children.get(i), key);
    }

    public void insert(T key) {
        Node<T> r = root;
        if (r.keys.size() == 2 * minDegree - 1) {
            Node<T> s = new Node<>(false);
            s.children.add(r);
            splitChild(s, 0, r);
            root = s;
            insertNonFull(s, key);
        } else {
            insertNonFull(r, key);
        }
    }

    private void insertNonFull(Node<T> node, T key) {
        int i = node.keys.size() - 1;
        if (node.leaf) {
            node.keys.add(null);
            while (i >= 0 && key.compareTo(node.keys.get(i)) < 0) {
                node.keys.set(i + 1, node.keys.get(i));
                i--;
            }
            node.keys.set(i + 1, key);
        } else {
            while (i >= 0 && key.compareTo(node.keys.get(i)) < 0) {
                i--;
            }
            i++;
            Node<T> child = node.children.get(i);
            if (child.keys.size() == 2 * minDegree - 1) {
                splitChild(node, i, child);
                if (key.compareTo(node.keys.get(i)) > 0) {
                    i++;
                }
            }
            insertNonFull(node.children.get(i), key);
        }
    }

    private void splitChild(Node<T> parent, int index, Node<T> fullChild) {
        Node<T> sibling = new Node<>(fullChild.leaf);
        for (int j = 0; j < minDegree - 1; j++) {
            sibling.keys.add(fullChild.keys.remove(minDegree));
        }
        if (!fullChild.leaf) {
            for (int j = 0; j < minDegree; j++) {
                sibling.children.add(fullChild.children.remove(minDegree));
            }
        }
        parent.children.add(index + 1, sibling);
        parent.keys.add(index, fullChild.keys.remove(minDegree - 1));
    }

    /**
        Minimal node representation used by the tree.
     */
    private static class Node<T> {
        final List<T> keys = new ArrayList<>();
        final List<Node<T>> children = new ArrayList<>();
        final boolean leaf;

        Node(boolean leaf) {
            this.leaf = leaf;
        }
    }
}
