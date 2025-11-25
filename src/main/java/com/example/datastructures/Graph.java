package com.example.datastructures;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A directed graph using adjacency lists.
 */
public class Graph<T> {
    private final Map<T, Set<T>> adjacency = new HashMap<>();

    public void addVertex(T vertex) {
        adjacency.computeIfAbsent(vertex, v -> new HashSet<>());
    }

    public void addEdge(T from, T to) {
        addVertex(from);
        addVertex(to);
        adjacency.get(from).add(to);
    }

    public void removeEdge(T from, T to) {
        Set<T> neighbors = adjacency.get(from);
        if (neighbors != null) {
            neighbors.remove(to);
        }
    }

    public boolean hasEdge(T from, T to) {
        Set<T> neighbors = adjacency.get(from);
        return neighbors != null && neighbors.contains(to);
    }

    public List<T> neighborsOf(T vertex) {
        Set<T> neighbors = adjacency.get(vertex);
        if (neighbors == null) {
            return Collections.emptyList();
        }
        return new ArrayList<>(neighbors);
    }

    public int size() {
        return adjacency.size();
    }
}
