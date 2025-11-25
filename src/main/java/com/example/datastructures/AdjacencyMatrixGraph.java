package com.example.datastructures;

import java.util.Arrays;

/**
 * An undirected graph represented with an adjacency matrix.
 * Edges are stored as booleans to keep the implementation compact.
 */
public class AdjacencyMatrixGraph {
    private final boolean[][] matrix;

    public AdjacencyMatrixGraph(int vertices) {
        if (vertices <= 0) {
            throw new IllegalArgumentException("Vertex count must be positive");
        }
        this.matrix = new boolean[vertices][vertices];
    }

    public void addEdge(int from, int to) {
        validateIndex(from);
        validateIndex(to);
        matrix[from][to] = true;
        matrix[to][from] = true;
    }

    public void removeEdge(int from, int to) {
        validateIndex(from);
        validateIndex(to);
        matrix[from][to] = false;
        matrix[to][from] = false;
    }

    public boolean hasEdge(int from, int to) {
        validateIndex(from);
        validateIndex(to);
        return matrix[from][to];
    }

    public int getVertexCount() {
        return matrix.length;
    }

    public boolean[][] copyMatrix() {
        boolean[][] copy = new boolean[matrix.length][matrix.length];
        for (int i = 0; i < matrix.length; i++) {
            copy[i] = Arrays.copyOf(matrix[i], matrix[i].length);
        }
        return copy;
    }

    private void validateIndex(int index) {
        if (index < 0 || index >= matrix.length) {
            throw new IndexOutOfBoundsException("Invalid vertex index: " + index);
        }
    }
}
