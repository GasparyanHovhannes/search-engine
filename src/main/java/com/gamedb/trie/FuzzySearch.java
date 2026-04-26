package com.gamedb.trie;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Fuzzy prefix search: returns completions even when the prefix contains
 * ONE typo (edit distance ≤ 1 from any real prefix in the Trie).
 *
 * Algorithm:
 *   We use a recursive DFS over the Trie while maintaining a "budget" of
 *   allowed edits.  At each character position we try:
 *     1. Exact match      (no edit consumed)
 *     2. Substitution     (1 edit: replace prefix[i] with a different child)
 *     3. Deletion         (1 edit: skip prefix[i], stay at same depth)
 *     4. Insertion        (1 edit: consume a Trie edge without advancing prefix)
 *
 *   Once we reach depth == prefix.length with editsUsed ≤ 1, we collect
 *   all terminal nodes under that Trie node normally.
 *
 * Complexity: O(|Σ|^maxEdits * p * T) — bounded in practice because
 * |Σ| is small (printable ASCII) and maxEdits == 1.
 */
public class FuzzySearch {

    private final Trie trie;
    private static final int MAX_EDITS = 1;

    public FuzzySearch(Trie trie) {
        this.trie = trie;
    }

    /**
     * Return top-K completions allowing at most 1 edit in the prefix.
     * Results are sorted by frequency descending.
     */
    public List<String> fuzzyTopK(String prefix, int k) {
        if (prefix == null || prefix.isBlank()) return Collections.emptyList();
        String key = prefix.toLowerCase().trim();

        // Collect all candidate terminal nodes
        Map<String, Integer> candidates = new LinkedHashMap<>();
        dfs(trie.getRoot(), key, 0, MAX_EDITS, candidates);

        // Sort by frequency desc, take top k
        return candidates.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(k)
            .map(Map.Entry::getKey)
            .toList();
    }

    /**
     * Recursive DFS.
     *
     * @param node       current Trie node
     * @param prefix     original prefix string
     * @param depth      how many characters of prefix we have consumed
     * @param editsLeft  remaining edit budget
     * @param out        accumulator: word → frequency
     */
    private void dfs(TrieNode node, String prefix, int depth,
                     int editsLeft, Map<String, Integer> out) {

        if (depth == prefix.length()) {
            // We have matched (approximately) the full prefix — collect all
            // terminal nodes in this sub-trie.
            collectAll(node, out);
            return;
        }

        char expected = prefix.charAt(depth);

        // 1. Exact match — no edit consumed
        TrieNode exactChild = node.children.get(expected);
        if (exactChild != null) {
            dfs(exactChild, prefix, depth + 1, editsLeft, out);
        }

        if (editsLeft == 0) return;  // no budget left for approximate moves

        for (Map.Entry<Character, TrieNode> entry : node.children.entrySet()) {
            char ch = entry.getKey();
            TrieNode child = entry.getValue();

            // 2. Substitution: replace prefix[depth] with ch (1 edit)
            if (ch != expected) {
                dfs(child, prefix, depth + 1, editsLeft - 1, out);
            }

            // 3. Insertion into prefix: consume a Trie edge without advancing
            //    depth — as if we inserted 'ch' into the prefix (1 edit)
            dfs(child, prefix, depth, editsLeft - 1, out);
        }

        // 4. Deletion from prefix: skip prefix[depth], stay at same node (1 edit)
        dfs(node, prefix, depth + 1, editsLeft - 1, out);
    }

    /** Collect every terminal node in the sub-trie rooted at node. */
    private void collectAll(TrieNode node, Map<String, Integer> out) {
        if (node.isEndOfWord && node.word != null) {
            out.merge(node.word, node.frequency, Integer::max);
        }
        for (TrieNode child : node.children.values()) {
            collectAll(child, out);
        }
    }
}
