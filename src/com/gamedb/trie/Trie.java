package com.gamedb.trie;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Trie (prefix tree) that supports:
 *   - insert / delete / search
 *   - top-K completion by frequency
 *
 * Complexity (n = number of words, L = average word length, p = prefix length):
 *   insert   : O(L)
 *   search   : O(L)
 *   delete   : O(L)
 *   topK     : O(p + T)  where T = total nodes in the sub-trie rooted at
 *                         the prefix node (we do a full DFS of that sub-trie)
 *              In the worst case T = O(n * L), but in practice only a small
 *              fraction of words share a given prefix, so it is much faster
 *              than a linear scan of the full word list.
 */
public class Trie {

    private final TrieNode root = new TrieNode();

    // ------------------------------------------------------------------ //
    //  INSERT                                                              //
    // ------------------------------------------------------------------ //

    /**
     * Insert a word with an initial frequency.
     * If the word already exists its frequency is INCREMENTED by the given
     * amount (useful for loading a pre-counted dataset).
     *
     * Time: O(L)
     */
    public void insert(String word, int frequency) {
        if (word == null || word.isEmpty()) return;
        String key = word.toLowerCase().trim();

        TrieNode current = root;
        for (char ch : key.toCharArray()) {
            current.children.putIfAbsent(ch, new TrieNode());
            current = current.children.get(ch);
        }
        current.isEndOfWord = true;
        current.frequency  += frequency;   // accumulate if inserted twice
        current.word        = key;         // store canonical form
    }

    /** Convenience overload — inserts with frequency 1. */
    public void insert(String word) {
        insert(word, 1);
    }

    // ------------------------------------------------------------------ //
    //  SEARCH                                                              //
    // ------------------------------------------------------------------ //

    /**
     * Returns the frequency of an exact word, or 0 if not found.
     * Also increments the frequency by 1 (models a "query" event).
     *
     * Time: O(L)
     */
    public int search(String word) {
        TrieNode node = getNode(word);
        if (node == null || !node.isEndOfWord) return 0;
        node.frequency++;          // record the query
        return node.frequency;
    }

    /** Returns true if the word exists in the Trie. */
    public boolean contains(String word) {
        TrieNode node = getNode(word);
        return node != null && node.isEndOfWord;
    }

    // ------------------------------------------------------------------ //
    //  DELETE                                                              //
    // ------------------------------------------------------------------ //

    /**
     * Delete a word from the Trie.
     * Prunes nodes that are no longer needed.
     *
     * Time: O(L)
     *
     * @return true if the word existed and was deleted, false otherwise.
     */
    public boolean delete(String word) {
        if (word == null || word.isEmpty()) return false;
        return deleteHelper(root, word.toLowerCase().trim(), 0);
    }

    private boolean deleteHelper(TrieNode current, String word, int depth) {
        if (depth == word.length()) {
            if (!current.isEndOfWord) return false;
            current.isEndOfWord = false;
            current.frequency   = 0;
            current.word        = null;
            return current.children.isEmpty();  // prune if leaf
        }
        char ch = word.charAt(depth);
        TrieNode next = current.children.get(ch);
        if (next == null) return false;

        boolean shouldDelete = deleteHelper(next, word, depth + 1);
        if (shouldDelete) {
            current.children.remove(ch);
            // prune current if it's now an empty non-terminal
            return current.children.isEmpty() && !current.isEndOfWord;
        }
        return false;
    }

    // ------------------------------------------------------------------ //
    //  TOP-K COMPLETIONS                                                   //
    // ------------------------------------------------------------------ //

    /**
     * Return the top-K completions for a given prefix, ranked by frequency
     * (descending).  Ties are broken alphabetically.
     *
     * Time: O(p + T * log K)  where T = nodes explored in the sub-trie
     *
     * @param prefix user-typed prefix (case-insensitive)
     * @param k      number of results to return
     * @return       list of at most k words, highest frequency first
     */
    public List<String> topKCompletions(String prefix, int k) {
        List<String> results = new ArrayList<>();
        if (prefix == null || k <= 0) return results;

        String key = prefix.toLowerCase().trim();
        TrieNode prefixNode = getNode(key);
        if (prefixNode == null) return results;   // prefix not found

        // Min-heap of size k — keeps the k highest-frequency words seen so far
        PriorityQueue<TrieNode> heap = new PriorityQueue<>(
            Comparator.comparingInt((TrieNode n) -> n.frequency)
                      .thenComparing(n -> n.word, Comparator.reverseOrder())
        );

        // DFS to collect all terminal nodes under this prefix
        collectWords(prefixNode, heap, k);

        // Drain heap into a list (it comes out min-first, so reverse)
        List<String> tmp = new ArrayList<>();
        while (!heap.isEmpty()) tmp.add(heap.poll().word);
        for (int i = tmp.size() - 1; i >= 0; i--) results.add(tmp.get(i));

        return results;
    }

    private void collectWords(TrieNode node, PriorityQueue<TrieNode> heap, int k) {
        if (node.isEndOfWord) {
            heap.offer(node);
            if (heap.size() > k) heap.poll();   // evict lowest-frequency
        }
        for (TrieNode child : node.children.values()) {
            collectWords(child, heap, k);
        }
    }

    // ------------------------------------------------------------------ //
    //  UTILITY                                                             //
    // ------------------------------------------------------------------ //

    /** Walk the trie to the node matching word, or return null. */
    private TrieNode getNode(String word) {
        if (word == null) return null;
        String key = word.toLowerCase().trim();
        TrieNode current = root;
        for (char ch : key.toCharArray()) {
            current = current.children.get(ch);
            if (current == null) return null;
        }
        return current;
    }

    /**
     * Return the root (for testing / benchmarking).
     */
    public TrieNode getRoot() { return root; }
}
