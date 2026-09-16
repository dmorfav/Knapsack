package org.dmorfav.knapsack.core;

import java.util.BitSet;
import java.util.Objects;
import java.util.stream.IntStream;

/** Vector binario inmutable: la posición i indica si el objeto i entra en la mochila. */
public final class Solution {

    private final BitSet selected;
    private final int size;

    private Solution(BitSet selected, int size) {
        this.selected = selected;
        this.size = size;
    }

    public static Solution empty(int size) {
        if (size < 0) {
            throw new IllegalArgumentException("size debe ser >= 0, es " + size);
        }
        return new Solution(new BitSet(size), size);
    }

    public static Solution of(boolean... selected) {
        BitSet bits = new BitSet(selected.length);
        for (int i = 0; i < selected.length; i++) {
            bits.set(i, selected[i]);
        }
        return new Solution(bits, selected.length);
    }

    public static Solution fromBits(int... bits) {
        BitSet selected = new BitSet(bits.length);
        for (int i = 0; i < bits.length; i++) {
            switch (bits[i]) {
                case 0 -> { }
                case 1 -> selected.set(i);
                default -> throw new IllegalArgumentException(
                        "la posición " + i + " vale " + bits[i] + "; solo se admite 0 o 1");
            }
        }
        return new Solution(selected, bits.length);
    }

    public int size() {
        return size;
    }

    public boolean isSelected(int index) {
        Objects.checkIndex(index, size);
        return selected.get(index);
    }

    public int selectedCount() {
        return selected.cardinality();
    }

    /** Índice del siguiente objeto seleccionado desde {@code fromIndex} (incluido), o -1 si no hay más. */
    public int nextSelected(int fromIndex) {
        return selected.nextSetBit(fromIndex);
    }

    public IntStream selectedIndices() {
        return selected.stream();
    }

    public Solution with(int index, boolean value) {
        Objects.checkIndex(index, size);
        BitSet copy = (BitSet) selected.clone();
        copy.set(index, value);
        return new Solution(copy, size);
    }

    public Solution flip(int index) {
        return with(index, !isSelected(index));
    }

    public int[] toBits() {
        int[] bits = new int[size];
        selected.stream().forEach(i -> bits[i] = 1);
        return bits;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Solution that && size == that.size && selected.equals(that.selected);
    }

    @Override
    public int hashCode() {
        return 31 * size + selected.hashCode();
    }

    @Override
    public String toString() {
        StringBuilder text = new StringBuilder(size);
        for (int i = 0; i < size; i++) {
            text.append(selected.get(i) ? '1' : '0');
        }
        return text.toString();
    }
}
