package com.example.datastructures;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * A simple generic stack implementation backed by a {@link Deque}.
 * The class demonstrates basic LIFO operations used in many algorithms.
 */
public class SimpleStack<T> {
    private final Deque<T> values = new ArrayDeque<>();

    public void push(T value) {
        values.push(value);
    }

    public T pop() {
        return values.pop();
    }

    public T peek() {
        return values.peek();
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    public int size() {
        return values.size();
    }
}
