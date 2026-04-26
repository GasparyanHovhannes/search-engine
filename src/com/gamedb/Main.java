package gamedb;

import gamedb.data.GameDataLoader;
import gamedb.trie.Trie;
import gamedb.trie.TrieNode;
import gamedb.ui.SearchUI;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Entry point for the GamePlatformDB Autocomplete Search Engine.
 *
 * Boot sequence:
 *   1. Create Trie
 *   2. Load GamePlatformDB data (games, genres, publishers, developers)
 *   3. Collect flat word list for benchmark linear-scan
 *   4. Launch Swing GUI on the Event Dispatch Thread
 */
public class Main {

    public static void main(String[] args) {
        // ── 1. Build Trie ────────────────────────────────────────────────
        Trie trie = new Trie();

        // ── 2. Load data ─────────────────────────────────────────────────
        GameDataLoader loader = new GameDataLoader(trie);
        loader.load();

        System.out.println("Data loaded into Trie.");

        // ── 3. Flat word list (for benchmark) ────────────────────────────
        List<String> allWords = collectWords(trie);
        System.out.println("Total unique entries: " + allWords.size());

        // ── 4. Launch GUI ─────────────────────────────────────────────────
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> new SearchUI(trie, loader, allWords));
    }

    /**
     * Walk the Trie and collect all terminal words into a flat list.
     * Used by the benchmark linear-scan comparison.
     */
    private static List<String> collectWords(Trie trie) {
        List<String> words = new ArrayList<>();
        dfs(trie.getRoot(), words);
        return words;
    }

    private static void dfs(TrieNode node, List<String> words) {
        if (node.isEndOfWord && node.word != null) words.add(node.word);
        for (TrieNode child : node.children.values()) dfs(child, words);
    }
}