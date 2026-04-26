package com.gamedb.trie;

import java.util.HashMap;
import java.util.Map;

/**
 * A single node in the Trie.
 *
 * Space per node: O(ALPHABET_SIZE) in the worst case, but since we use a
 * HashMap the actual footprint is proportional to the number of distinct
 * children, not the full alphabet.
 */
public class TrieNode {

    /** Children keyed by character. */
    public final Map<Character, TrieNode> children = new HashMap<>();

    /** True when this node marks the end of a complete word/phrase. */
    public boolean isEndOfWord = false;

    /**
     * How many times this word was inserted (or looked up).
     * Only meaningful when isEndOfWord == true, but kept here to avoid
     * a separate map.
     */
    public int frequency = 0;

    /** The full word stored at this terminal node (convenience field). */
    public String word = null;
}
