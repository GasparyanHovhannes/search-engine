# Autocomplete Search Engine

**French University in Armenia · Data Structures · 2026**  
Students: Gasparyan Hovhannes · Madatyan Narek · Boyakhchyan Tigran  
Mentor: Ghevond Gevorgyan

---

## 1. Project Description

This project implements an autocomplete search engine built on top of the Computer Games dataset, which is a collection of 1000+ unique video games with user rating entries. When a user types any prefix into the search bar, the engine instantly returns the top 5 most relevant game titles ranked by their average user rating.

The system is designed around three core requirements from the project specification:

- **Prefix-based retrieval** - completions are found using a Trie (prefix tree), which is the standard industry approach for autocomplete systems because it provides O(prefix length) navigation time regardless of how many words are stored.
- **Frequency ranking** - each game's insertion frequency in the Trie is derived from its average user rating across all entries in the CSV, normalised to a 1-1000 scale. Higher-rated games always appear first.
- **Recently searched boost** - when a user searches for a term, its Trie frequency is increased by 200 points, causing it to naturally rise to the top of future completions for that prefix. This is managed by an LRU cache in the data layer.

---

## Data Structures Used

### 1.1 Trie (Prefix Tree)

The Trie is the primary data structure. Each node represents one character on the path from the root to a complete word. Nodes that are shared between words (e.g. `g`, `r`, `a`, `n`, `d` for both "Grand Theft Auto V" and "Grand Theft Auto IV") are stored only once, making prefix navigation extremely efficient.

Each `TrieNode` contains:

```java
class TrieNode {
    Map<Character, TrieNode> children;  // next characters (HashMap, not array)
    boolean isEndOfWord;                // true when a complete word ends here
    int     frequency;                  // ranking score (avg rating, normalised)
    String  word;                       // full word stored at terminal nodes
}
```

**Why HashMap for children instead of a fixed array?** Game titles contain letters, digits, spaces, colons, apostrophes, and hyphens. A fixed array of size 128 (ASCII) would waste memory for every node. A HashMap allocates space only for characters that actually appear as children, reducing memory usage proportionally to the branching factor of the real data.

### 1.2 Min-Heap (Priority Queue) for Top-K Retrieval

Inside `topKCompletions()`, a `PriorityQueue` (min-heap) of size K is maintained during the DFS traversal of the sub-trie. The heap always evicts the lowest-frequency word when it exceeds K entries, so at the end it holds exactly the K highest-frequency words found under the prefix.

**Why a min-heap and not sorting?** Sorting all T words in the sub-trie would cost O(T log T). The heap approach costs O(T log K). Since K = 5 is fixed and tiny, log K is effectively a constant, making the heap significantly faster on large sub-tries.

### 1.3 LinkedHashMap (LRU Cache) for Recent Searches

The recently-searched feature uses a `LinkedHashMap` with access-order enabled. This is the standard Java pattern for an LRU (Least Recently Used) cache: the most recently accessed key is automatically moved to the end of the internal linked list, and overriding `removeEldestEntry()` causes the oldest entry to be evicted once the map exceeds its capacity of 20 entries.

---

## Algorithms Used

### 1.4 DFS (Depth-First Search) for Prefix Completion

After navigating to the prefix node in the Trie, a recursive DFS collects all terminal nodes in the sub-trie rooted there. At each terminal node, the word is offered to the min-heap. This is the standard algorithm for prefix retrieval in a Trie.

### 1.5 Min-Max Normalisation

Raw average ratings from the CSV fall in a narrow band (~100-600) that makes ranking unreliable. Min-max normalisation maps every game's average rating to the range 1-1000 relative to the minimum and maximum ratings across the entire dataset:

```
frequency = round(((avg - min) / (max - min)) * 999) + 1
```

This ensures the best-rated game always gets frequency 1000 and the worst gets 1, with all others distributed proportionally in between.

### 1.6 Fuzzy Search with Edit-Budget DFS

The fuzzy search extends the standard DFS by carrying an edit budget (set to 1). At each position in the typed prefix, the algorithm tries four moves: exact match (free), substitution (costs 1 edit), deletion of a prefix character (costs 1 edit), and insertion of a Trie character (costs 1 edit). Once the budget is exhausted, only exact matches are followed. This correctly implements edit distance ≤ 1 search without requiring a full edit-distance matrix.

---

## 2. Design Choices and Justifications

### 2.1 Why a Standard Trie and Not a Compressed (Patricia) Trie?

A Patricia trie (also called a Radix tree) compresses chains of single-child nodes into a single edge labelled with a string rather than a single character. This reduces memory and can speed up traversal for sparse datasets. However, it significantly increases implementation complexity: every insert and delete must correctly split and merge compressed edges.

For this project, a standard Trie was chosen because:

- **Dataset size is small.** 1000 unique game titles occupy negligible memory even in an uncompressed Trie. The memory saving of a Patricia trie would be unmeasurable.
- **Modification frequency.** The recently-searched boost inserts terms with incremented frequencies. In a compressed Trie, this requires edge-splitting logic that adds risk of bugs.

### 2.2 Why HashMap Children and Not a Sorted Array?

Alternatives considered:

- **Fixed `char[128]` array:** O(1) child lookup by index, but wastes 128 pointers per node regardless of how many children exist. For game titles with a small alphabet in practice, most slots would be null.
- **Sorted array + binary search:** O(log c) lookup where c is the number of children. More memory-efficient than a fixed array but more complex to maintain on insert.
- **HashMap (chosen):** O(1) average-case lookup, allocates only for characters that actually exist as children. The small overhead of hashing is irrelevant at this dataset size.

### 2.3 Why Min-Heap for Top-K and Not Collecting All + Sorting?

Collecting all terminal nodes under a prefix into a list and then sorting costs O(T log T) where T is the number of matching words. For a prefix like `"g"` that matches 10 games, this is trivial. But the system must handle single-character prefixes that could match hundreds of entries in a larger dataset. The min-heap approach caps the cost at O(T log K). With K = 5 fixed, this is effectively O(T), which is optimal since every node must be visited at least once.

### 2.4 Why Min-Max Normalisation Instead of Raw Averages?

Raw average ratings cluster in the 300-700 range. Without normalisation, all games in the Trie have similar frequencies and the top-K selection is essentially arbitrary - producing the appearance of always showing the same 500+ rated games. Normalising to 1-1000 spreads games across the full frequency range so differences between them are meaningful and rankings are deterministic.

### 2.5 Why LinkedHashMap for the LRU Cache?

Java's `LinkedHashMap` with the access-order flag and overridden `removeEldestEntry()` is the idiomatic LRU cache implementation in Java with zero external dependencies. Alternatives such as a doubly linked list paired with a separate HashMap would achieve the same Big-O bounds but require significantly more code. Since the cache holds at most 20 entries, performance is irrelevant and the built-in implementation is preferable.

---

## 3. Complexity Analysis

**Variables used throughout this section:**

| Symbol | Meaning |
|--------|---------|
| L | Length of the word being inserted, searched, or deleted |
| p | Length of the query prefix |
| T | Number of nodes in the sub-trie rooted at the prefix node |
| K | Number of completions requested (always 5 in this project) |
| N | Total number of unique words stored in the Trie |
| \|Σ\| | Size of the character alphabet actually used |

### 3.1 Trie Class

| Method | Time Complexity | Space Complexity | Notes |
|--------|----------------|-----------------|-------|
| `insert()` | O(L) | O(L) new nodes | Creates at most L new TrieNodes along the path. Accumulates frequency if word already exists. |
| `search()` | O(L) | O(1) | Walks L edges, increments frequency at the terminal node. No allocation. |
| `contains()` | O(L) | O(1) | Same traversal as search() but without the frequency increment. |
| `delete()` | O(L) | O(L) stack frames | Recursive. Stack depth equals word length L. Prunes empty nodes on the way back up. |
| `topKCompletions()` | O(p + T·log K) | O(K) heap + O(L) stack | O(p) to reach prefix node. O(T·log K) DFS with heap maintenance. |
| `getNode()` | O(L) | O(1) | Private utility. Walks L edges, returns the node or null. |

> **Worst-case vs. practical for `topKCompletions()`:** T is bounded in the worst case by N·L (every word shares the prefix). In practice, for a 58-game dataset with a 2-character prefix, T is typically under 10 nodes, making the query effectively O(1).

### 3.2 TrieNode Class

| Method | Time Complexity | Space Complexity | Notes |
|--------|----------------|-----------------|-------|
| Construction | O(1) | O(1) | HashMap initialised empty. Four fields set to defaults. |
| children lookup | O(1) avg | O(\|children\|) | HashMap get/put. Amortised O(1). Space proportional to actual children count only. |

### 3.3 FuzzySearch Class

| Method | Time Complexity | Space Complexity | Notes |
|--------|----------------|-----------------|-------|
| `fuzzyTopK()` | O(\|Σ\|·p·T) | O(T) candidate map | DFS with edit budget. Each node spawns at most \|Σ\|+2 recursive calls when editsLeft>0, only 1 when editsLeft=0. |
| `dfs()` | O(\|Σ\|·p·T) | O(p) stack frames | Recursive. Budget of 1 edit limits branching to a manageable factor. |
| `collectAll()` | O(T) | O(1) per node | Visits every node in the sub-trie exactly once, adding terminals to the output map. |

### 3.4 GameDataLoader Class

| Method | Time Complexity | Space Complexity | Notes |
|--------|----------------|-----------------|-------|
| `load()` | O(R + N·log N) | O(R) | R = rows in CSV. Reads all rows O(R), averages per game O(N), min-max pass O(N), inserts N words O(N·L). |
| `recordSearch()` | O(L) | O(1) amortised | One HashMap put into the LRU cache + one Trie insert, both O(L). |
| `getRecentSearches()` | O(C) | O(C) | C = cache size ≤ 20. Copies keys into a list and reverses. Effectively O(1) since C is bounded. |

### 3.5 Benchmark Class

| Method | Time Complexity | Space Complexity | Notes |
|--------|----------------|-----------------|-------|
| `run()` | O(I·(p + T·log K + N)) | O(K + N) | I = iterations (5000). Runs both Trie query and linear scan I times each. |
| `linearScan()` | O(N) | O(k) | Streams all N words, filters by prefix, sorts matches (≤N), takes top k. Always reads the full list. |

### 3.6 Overall Memory Usage

The Trie stores at most N·L nodes in the worst case (no shared prefixes). With N = 1132 games and an average title length of ~20 characters, the upper bound is 1132 × 20 = 22640 nodes. Each node holds a HashMap (typically 2-5 entries at this dataset size), a boolean, an int, and a String reference. Total memory is well under 1 MB.

---

## 4. Team Contributions

### Gasparyan Hovhannes - Trie Engine
**Files:** `TrieNode.java` · `Trie.java` · `FuzzySearch.java`

- Designed and implemented the `TrieNode` class, choosing HashMap over a fixed-size array for children to avoid wasting memory on unused character slots.
- Implemented all core Trie operations: `insert()` with frequency accumulation, `search()` with query-event tracking (frequency increment), `contains()`, and `delete()` with recursive node pruning.
- Implemented `topKCompletions()` using a DFS traversal combined with a min-heap of size K, achieving O(T·log K) time rather than O(T·log T) from sorting.
- Implemented `FuzzySearch` with an edit-budget DFS that supports substitution, deletion, and insertion edits - allowing the search bar to return results even when the user makes a single typo.

### Madatyan Narek - Data Layer
**Files:** `GameDataLoader.java` · `Benchmark.java` · `games.csv`

- Collected the `games.csv` dataset containing 5,000 rating entries across 1000+ unique game titles from the GamePlatformDB.
- Implemented `GameDataLoader.load()`: reads the CSV, aggregates multiple rating entries per game into an average, applies min-max normalisation to spread frequencies across 1-1000, and inserts each game into the Trie.
- Implemented the LRU recently-searched cache using a `LinkedHashMap` with access-order, which automatically evicts the oldest entry when the cache exceeds 20 items and boosts searched terms by 200 frequency points in the Trie.
- Implemented the `Benchmark` class, which times Trie prefix search against a brute-force linear scan over 5,000 iterations (with a 100-iteration JIT warm-up) and reports average nanoseconds per query and speedup ratio.

### Boyakhchyan Tigran - GUI & Integration
**Files:** `SearchUI.java` · `Main.java`

- Implemented `Main.java`: initialises the Trie, triggers the data loader, collects the flat word list for the benchmark's linear scan, and launches the Swing GUI on the Event Dispatch Thread using `SwingUtilities.invokeLater()`.
- Built the `SearchUI` Swing application with a live search bar (`DocumentListener` fires on every keystroke), a top-5 suggestion list with rank badges and frequency scores, a recent-searches label, and an embedded benchmark results panel.
- Wired all three layers together: the GUI calls Hovhannes's Trie for completions and FuzzySearch for typo-tolerant queries, and calls Narek's `GameDataLoader` to record searches and retrieve the recent-search list.
- Implemented the fuzzy toggle, which switches between exact prefix search and FuzzySearch at runtime, and the Benchmark button, which runs the timing comparison asynchronously on the EDT.
