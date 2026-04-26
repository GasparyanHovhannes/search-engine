package com.gamedb.data;

import com.gamedb.trie.Trie;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Benchmarks Trie prefix search vs. brute-force linear scan.
 *
 * Usage:
 *   BenchmarkResult result = Benchmark.run(trie, allWords, prefix, iterations);
 *   System.out.println(result);
 */
public class Benchmark {

    public record BenchmarkResult(
        String prefix,
        long trieNanos,
        long linearNanos,
        double speedupX
    ) {
        @Override
        public String toString() {
            return String.format(
                "Prefix='%s'  |  Trie: %,d ns  |  Linear: %,d ns  |  Speedup: %.1fx",
                prefix, trieNanos, linearNanos, speedupX
            );
        }
    }

    /**
     * Run both approaches for a given prefix and return timing results.
     *
     * @param trie       loaded Trie
     * @param allWords   flat list of all words (for linear scan)
     * @param prefix     query prefix
     * @param iterations warm-up + measurement repetitions
     */
    public static BenchmarkResult run(Trie trie,
                                      List<String> allWords,
                                      String prefix,
                                      int iterations) {
        final int TOP_K = 5;
        final String key = prefix.toLowerCase().trim();

        // --- Warm up (prevent JIT bias) ---
        for (int i = 0; i < 100; i++) {
            trie.topKCompletions(key, TOP_K);
            linearScan(allWords, key, TOP_K);
        }

        // --- Trie timing ---
        long trieStart = System.nanoTime();
        for (int i = 0; i < iterations; i++) trie.topKCompletions(key, TOP_K);
        long trieTotal = (System.nanoTime() - trieStart) / iterations;

        // --- Linear timing ---
        long linStart = System.nanoTime();
        for (int i = 0; i < iterations; i++) linearScan(allWords, key, TOP_K);
        long linTotal = (System.nanoTime() - linStart) / iterations;

        double speedup = (double) linTotal / trieTotal;
        return new BenchmarkResult(prefix, trieTotal, linTotal, speedup);
    }

    /** Brute-force: scan every word, filter by prefix, sort by length (no freq). */
    public static List<String> linearScan(List<String> words, String prefix, int k) {
        return words.stream()
                    .filter(w -> w.toLowerCase().startsWith(prefix))
                    .sorted()
                    .limit(k)
                    .collect(Collectors.toList());
    }
}
