package gamedb.data;

import gamedb.trie.Trie;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

/**
 * Loads game data from games.csv (located in the data package).
 *
 * CSV format:
 *   Game Name,User Rating
 *   Sekiro: Shadows Die Twice,965.47
 *   ...
 *
 * The same game appears many times with different ratings.
 * We aggregate: compute the average rating per game, then use that
 * as the Trie frequency so higher-rated games appear first in results.
 *
 * Also manages the "recently searched" LRU cache (bonus feature):
 *   - Stores the last 20 searches in a LinkedHashMap (access-ordered).
 *   - When a term is searched, its Trie frequency is boosted by RECENT_BOOST,
 *     so it floats to the top of future completions.
 */
public class GameDataLoader {

    // Path to the CSV file inside the data package (on the classpath)
    private static final String CSV_PATH = "com/gamedb/data/games.csv";

    // How much to boost a recently searched term's frequency
    private static final int RECENT_BOOST = 200;

    // Maximum number of recent searches to remember
    private static final int RECENT_CAPACITY = 20;

    private final Trie trie;

    /**
     * LRU cache of recently searched terms.
     * LinkedHashMap with accessOrder=true: the most-recently accessed
     * entry moves to the end automatically.
     * removeEldestEntry() evicts the oldest when the map is full.
     */
    private final LinkedHashMap<String, Integer> recentlySearched =
        new LinkedHashMap<>(RECENT_CAPACITY, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Integer> eldest) {
                return size() > RECENT_CAPACITY;
            }
        };

    public GameDataLoader(Trie trie) {
        this.trie = trie;
    }

    // ------------------------------------------------------------------ //
    //  LOAD                                                                //
    // ------------------------------------------------------------------ //

    /**
     * Reads games.csv, aggregates ratings per game name, and inserts
     * each unique game into the Trie with its average rating as frequency.
     */
    public void load() {
        // Step 1: collect all ratings for each game name
        Map<String, List<Double>> ratingsMap = new LinkedHashMap<>();

        InputStream is = getClass().getClassLoader().getResourceAsStream(CSV_PATH);
        if (is == null) {
            System.err.println("ERROR: games.csv not found at classpath path: " + CSV_PATH);
            System.err.println("In IntelliJ: right-click the 'data' folder -> Mark Directory as -> Sources Root");
            return;
        }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            String line;
            boolean header = true;
            while ((line = br.readLine()) != null) {
                if (header) { header = false; continue; } // skip header row
                line = line.trim();
                if (line.isEmpty()) continue;

                // Split on the LAST comma — handles game names that contain colons,
                // apostrophes, and other special characters safely.
                int lastComma = line.lastIndexOf(',');
                if (lastComma == -1) continue;

                String gameName = line.substring(0, lastComma).trim();
                String ratingStr = line.substring(lastComma + 1).trim();

                try {
                    double rating = Double.parseDouble(ratingStr);
                    ratingsMap.computeIfAbsent(gameName, k -> new ArrayList<>()).add(rating);
                } catch (NumberFormatException e) {
                    // skip malformed rows silently
                }
            }
        } catch (Exception e) {
            System.err.println("Error reading games.csv: " + e.getMessage());
            return;
        }

        // Step 2: compute average rating per game
        Map<String, Double> averages = new LinkedHashMap<>();
        for (Map.Entry<String, List<Double>> entry : ratingsMap.entrySet()) {
            double avg = entry.getValue().stream()
                              .mapToDouble(Double::doubleValue)
                              .average()
                              .orElse(0.0);
            averages.put(entry.getKey(), avg);
        }

        // Step 3: normalize to 1–1000 relative to min/max across ALL games.
        // Without this, every game sits between 500–600 (the raw average range),
        // so the Trie has no meaningful way to rank them against each other.
        double min = averages.values().stream().mapToDouble(Double::doubleValue).min().orElse(0);
        double max = averages.values().stream().mapToDouble(Double::doubleValue).max().orElse(1);
        double range = (max - min) == 0 ? 1 : (max - min);

        int loaded = 0;
        for (Map.Entry<String, Double> entry : averages.entrySet()) {
            int frequency = (int) Math.round(((entry.getValue() - min) / range) * 999) + 1;
            trie.insert(entry.getKey(), frequency);
            loaded++;
        }

        System.out.println("Loaded " + loaded + " unique games from games.csv.");
    }

    // ------------------------------------------------------------------ //
    //  RECENTLY SEARCHED                                                   //
    // ------------------------------------------------------------------ //

    /**
     * Records a user search term in the LRU cache and boosts its
     * frequency in the Trie so it ranks higher in future completions.
     */
    public void recordSearch(String term) {
        if (term == null || term.isBlank()) return;
        String key = term.toLowerCase().trim();
        recentlySearched.merge(key, 1, Integer::sum);
        trie.insert(key, RECENT_BOOST); // insert() accumulates, so this adds to existing freq
    }

    /**
     * Returns the list of recent searches, most-recent first.
     */
    public List<String> getRecentSearches() {
        List<String> list = new ArrayList<>(recentlySearched.keySet());
        Collections.reverse(list); // map is oldest-first; reverse for newest-first
        return list;
    }
}
