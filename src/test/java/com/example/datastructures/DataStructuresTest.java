package com.example.datastructures;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DataStructuresTest {

    @Test
    void stackSupportsLifo() {
        SimpleStack<Integer> stack = new SimpleStack<>();
        assertTrue(stack.isEmpty());
        stack.push(1);
        stack.push(2);
        stack.push(3);
        assertEquals(3, stack.size());
        assertEquals(3, stack.pop());
        assertEquals(2, stack.pop());
        assertEquals(1, stack.peek());
        assertFalse(stack.isEmpty());
    }

    @Test
    void linkedListHandlesInsertions() {
        SinglyLinkedList<String> list = new SinglyLinkedList<>();
        list.addFirst("world");
        list.addFirst("hello");
        list.addLast("!");
        assertEquals(3, list.size());
        assertTrue(list.contains("hello"));
        assertEquals("hello", list.removeFirst());
        assertEquals(2, list.size());
    }

    @Test
    void queuePreservesOrder() {
        SimpleQueue<Integer> queue = new SimpleQueue<>();
        queue.enqueue(5);
        queue.enqueue(6);
        queue.enqueue(7);
        assertEquals(3, queue.size());
        assertEquals(5, queue.dequeue());
        assertEquals(6, queue.peek());
    }

    @Test
    void bTreeInsertsAndFindsKeys() {
        BTree<Integer> bTree = new BTree<>(2);
        for (int value : new int[]{10, 20, 5, 6, 12, 30, 7, 17}) {
            bTree.insert(value);
        }
        assertTrue(bTree.search(6));
        assertTrue(bTree.search(17));
        assertFalse(bTree.search(25));
    }

    @Test
    void adjacencyMatrixTracksEdges() {
        AdjacencyMatrixGraph graph = new AdjacencyMatrixGraph(3);
        graph.addEdge(0, 1);
        graph.addEdge(1, 2);
        assertTrue(graph.hasEdge(0, 1));
        assertTrue(graph.hasEdge(1, 2));
        graph.removeEdge(0, 1);
        assertFalse(graph.hasEdge(0, 1));
        assertEquals(3, graph.getVertexCount());
    }

    @Test
    void adjacencyListGraphSupportsNeighbors() {
        Graph<String> graph = new Graph<>();
        graph.addEdge("A", "B");
        graph.addEdge("A", "C");
        graph.addEdge("B", "C");
        List<String> neighbors = graph.neighborsOf("A");
        assertEquals(2, neighbors.size());
        assertTrue(graph.hasEdge("A", "C"));
        graph.removeEdge("A", "B");
        assertFalse(graph.hasEdge("A", "B"));
    }

    @Test
    void binarySearchTreeSortsValues() {
        BinarySearchTree<Integer> tree = new BinarySearchTree<>();
        tree.insert(5);
        tree.insert(1);
        tree.insert(10);
        tree.insert(7);
        assertTrue(tree.contains(10));
        assertFalse(tree.contains(100));
        assertEquals(List.of(1, 5, 7, 10), tree.inOrder());
    }

    @Test
    void skipListSupportsInsertAndDelete() {
        SkipList<Integer> skipList = new SkipList<>();
        skipList.insert(10);
        skipList.insert(20);
        skipList.insert(30);
        assertTrue(skipList.contains(20));
        assertTrue(skipList.delete(20));
        assertFalse(skipList.contains(20));
        assertFalse(skipList.delete(40));
    }
}
