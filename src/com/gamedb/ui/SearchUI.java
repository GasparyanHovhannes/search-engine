package gamedb.ui;

import gamedb.data.Benchmark;
import gamedb.data.GameDataLoader;
import gamedb.trie.FuzzySearch;
import gamedb.trie.Trie;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * Main Swing GUI for the GamePlatformDB Autocomplete Search Engine.
 *
 * Layout:
 *   ┌─────────────────────────────────┐
 *   │        HEADER / LOGO            │
 *   │  [Search bar          ] [⚙]     │
 *   │  ┌─ Suggestions ──────────────┐ │
 *   │  │  1. Cyberpunk 2077  ★ 95  │ │
 *   │  │  2. Celeste         ★ 90  │ │
 *   │  └────────────────────────────┘ │
 *   │  [Recent searches]              │
 *   │  [Benchmark results]            │
 *   └─────────────────────────────────┘
 */
public class SearchUI extends JFrame {

    // ── Colours ─────────────────────────────────────────────────────────
    private static final Color BG        = new Color(10,  12,  20);
    private static final Color PANEL_BG  = new Color(18,  22,  35);
    private static final Color CARD_BG   = new Color(24,  29,  45);
    private static final Color ACCENT    = new Color(99, 179, 237);
    private static final Color ACCENT2   = new Color(252, 129,  74);
    private static final Color TEXT_PRI  = new Color(230, 235, 245);
    private static final Color TEXT_SEC  = new Color(130, 145, 175);
    private static final Color BORDER_C  = new Color(40,  50,  75);

    private static final Font FONT_TITLE = new Font("SansSerif", Font.BOLD, 22);
    private static final Font FONT_BODY  = new Font("Monospaced", Font.PLAIN, 14);
    private static final Font FONT_SMALL = new Font("SansSerif",  Font.PLAIN, 12);
    private static final Font FONT_BADGE = new Font("SansSerif",  Font.BOLD,  11);

    // ── State ────────────────────────────────────────────────────────────
    private final Trie trie;
    private final GameDataLoader loader;
    private final FuzzySearch fuzzy;
    private final List<String>   allWords;

    // ── UI Components ─────────────────────────────────────────────────────
    private JTextField  searchField;
    private JPanel      suggestionsPanel;
    private JLabel      statusLabel;
    private JTextArea   benchArea;
    private JLabel      recentLabel;
    private JCheckBox   fuzzyToggle;

    // ── Constructor ───────────────────────────────────────────────────────

    public SearchUI(Trie trie, GameDataLoader loader, List<String> allWords) {
        this.trie     = trie;
        this.loader   = loader;
        this.fuzzy    = new FuzzySearch(trie);
        this.allWords = allWords;

        setTitle("GamePlatform Autocomplete");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(720, 680);
        setLocationRelativeTo(null);
        setBackground(BG);

        buildUI();
        setVisible(true);
    }

    // ── UI Construction ───────────────────────────────────────────────────

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(BG);

        root.add(buildHeader(),      BorderLayout.NORTH);
        root.add(buildCenter(),      BorderLayout.CENTER);
        root.add(buildFooter(),      BorderLayout.SOUTH);

        setContentPane(root);
    }

    // ── Header ────────────────────────────────────────────────────────────

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setBackground(PANEL_BG);
        header.setBorder(new CompoundBorder(
            new MatteBorder(0, 0, 1, 0, BORDER_C),
            new EmptyBorder(18, 24, 18, 24)
        ));

        // Logo + title
        JLabel title = new JLabel("◈  GamePlatform Search");
        title.setForeground(ACCENT);
        title.setFont(FONT_TITLE);
        header.add(title, BorderLayout.WEST);

        // Fuzzy toggle
        fuzzyToggle = new JCheckBox("Fuzzy (≤1 typo)");
        fuzzyToggle.setForeground(TEXT_SEC);
        fuzzyToggle.setBackground(PANEL_BG);
        fuzzyToggle.setFont(FONT_SMALL);
        fuzzyToggle.setFocusPainted(false);
        header.add(fuzzyToggle, BorderLayout.EAST);

        return header;
    }

    // ── Centre ────────────────────────────────────────────────────────────

    private JPanel buildCenter() {
        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setBackground(BG);
        center.setBorder(new EmptyBorder(20, 28, 10, 28));

        center.add(buildSearchBar());
        center.add(Box.createVerticalStrut(14));
        center.add(buildSuggestionsSection());
        center.add(Box.createVerticalStrut(14));
        center.add(buildRecentSection());
        center.add(Box.createVerticalStrut(14));
        center.add(buildBenchSection());

        return center;
    }

    // ── Search bar ────────────────────────────────────────────────────────

    private JPanel buildSearchBar() {
        JPanel wrap = new JPanel(new BorderLayout(10, 0));
        wrap.setBackground(BG);
        wrap.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));

        searchField = new JTextField();
        searchField.setFont(new Font("SansSerif", Font.PLAIN, 16));
        searchField.setBackground(CARD_BG);
        searchField.setForeground(TEXT_PRI);
        searchField.setCaretColor(ACCENT);
        searchField.setBorder(new CompoundBorder(
            new LineBorder(BORDER_C, 1, true),
            new EmptyBorder(8, 14, 8, 14)
        ));

        // Placeholder
        searchField.setText("Search games, genres, publishers…");
        searchField.setForeground(TEXT_SEC);
        searchField.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                if (searchField.getForeground().equals(TEXT_SEC)) {
                    searchField.setText("");
                    searchField.setForeground(TEXT_PRI);
                }
            }
        });

        // Live search on every keystroke
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e)  { onType(); }
            public void removeUpdate(DocumentEvent e)  { onType(); }
            public void changedUpdate(DocumentEvent e) { onType(); }
        });

        // Enter → record as explicit search
        searchField.addActionListener(e -> onEnter());

        // Benchmark button
        JButton benchBtn = new JButton("Benchmark");
        styleButton(benchBtn, ACCENT2);
        benchBtn.addActionListener(e -> runBenchmark());

        wrap.add(searchField, BorderLayout.CENTER);
        wrap.add(benchBtn,    BorderLayout.EAST);
        return wrap;
    }

    // ── Suggestions ───────────────────────────────────────────────────────

    private JPanel buildSuggestionsSection() {
        JPanel outer = new JPanel(new BorderLayout(0, 8));
        outer.setBackground(BG);

        JLabel heading = sectionLabel("Top Completions");
        outer.add(heading, BorderLayout.NORTH);

        suggestionsPanel = new JPanel();
        suggestionsPanel.setLayout(new BoxLayout(suggestionsPanel, BoxLayout.Y_AXIS));
        suggestionsPanel.setBackground(CARD_BG);
        suggestionsPanel.setBorder(new LineBorder(BORDER_C, 1, true));

        showPlaceholder();

        outer.add(suggestionsPanel, BorderLayout.CENTER);
        return outer;
    }

    // ── Recent section ───────────────────────────────────────────────────

    private JPanel buildRecentSection() {
        JPanel outer = new JPanel(new BorderLayout(0, 6));
        outer.setBackground(BG);
        outer.add(sectionLabel("Recent Searches"), BorderLayout.NORTH);

        recentLabel = new JLabel("—");
        recentLabel.setForeground(TEXT_SEC);
        recentLabel.setFont(FONT_SMALL);
        recentLabel.setBorder(new EmptyBorder(4, 0, 0, 0));
        outer.add(recentLabel, BorderLayout.CENTER);
        return outer;
    }

    // ── Benchmark area ────────────────────────────────────────────────────

    private JPanel buildBenchSection() {
        JPanel outer = new JPanel(new BorderLayout(0, 6));
        outer.setBackground(BG);
        outer.add(sectionLabel("Benchmark  (Trie vs Linear Scan)"), BorderLayout.NORTH);

        benchArea = new JTextArea(3, 60);
        benchArea.setEditable(false);
        benchArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        benchArea.setBackground(CARD_BG);
        benchArea.setForeground(TEXT_SEC);
        benchArea.setBorder(new CompoundBorder(
            new LineBorder(BORDER_C, 1, true),
            new EmptyBorder(8, 10, 8, 10)
        ));
        benchArea.setText("Click 'Benchmark' after typing a prefix.");

        outer.add(benchArea, BorderLayout.CENTER);
        return outer;
    }

    // ── Footer ────────────────────────────────────────────────────────────

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 6));
        footer.setBackground(PANEL_BG);
        footer.setBorder(new MatteBorder(1, 0, 0, 0, BORDER_C));

        statusLabel = new JLabel("Ready — " + allWords.size() + " entries loaded.");
        statusLabel.setForeground(TEXT_SEC);
        statusLabel.setFont(FONT_SMALL);
        footer.add(statusLabel);
        return footer;
    }

    // ── Event handlers ────────────────────────────────────────────────────

    private void onType() {
        String query = searchField.getText().trim();
        if (query.isEmpty() || searchField.getForeground().equals(TEXT_SEC)) {
            showPlaceholder();
            return;
        }
        List<String> results = fuzzyToggle.isSelected()
            ? fuzzy.fuzzyTopK(query, 5)
            : trie.topKCompletions(query, 5);

        renderSuggestions(results, query);
        statusLabel.setText("Prefix: \"" + query + "\"  →  " + results.size() + " result(s)");
    }

    private void onEnter() {
        String term = searchField.getText().trim();
        if (term.isEmpty()) return;
        loader.recordSearch(term);
        trie.search(term);          // also increments frequency in Trie
        updateRecentLabel();
        statusLabel.setText("Recorded search: \"" + term + "\"");
    }

    private void runBenchmark() {
        String prefix = searchField.getText().trim();
        if (prefix.isEmpty() || searchField.getForeground().equals(TEXT_SEC)) {
            benchArea.setText("Type a prefix first, then click Benchmark.");
            return;
        }
        benchArea.setText("Running benchmark…");
        SwingUtilities.invokeLater(() -> {
            StringBuilder sb = new StringBuilder();
            String[] prefixes = {prefix, prefix.substring(0, Math.max(1, prefix.length() / 2)), "a"};
            for (String p : prefixes) {
                Benchmark.BenchmarkResult r = Benchmark.run(trie, allWords, p, 5000);
                sb.append(r).append("\n");
            }
            benchArea.setText(sb.toString().trim());
        });
    }

    // ── Rendering helpers ─────────────────────────────────────────────────

    private void renderSuggestions(List<String> results, String prefix) {
        suggestionsPanel.removeAll();

        if (results.isEmpty()) {
            JLabel empty = new JLabel("  No completions found for  \"" + prefix + "\"");
            empty.setForeground(TEXT_SEC);
            empty.setFont(FONT_BODY);
            empty.setBorder(new EmptyBorder(12, 14, 12, 14));
            suggestionsPanel.add(empty);
        } else {
            for (int i = 0; i < results.size(); i++) {
                suggestionsPanel.add(buildSuggestionRow(i + 1, results.get(i)));
                if (i < results.size() - 1) {
                    suggestionsPanel.add(buildDivider());
                }
            }
        }

        suggestionsPanel.revalidate();
        suggestionsPanel.repaint();
    }

    private JPanel buildSuggestionRow(int rank, String word) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setBackground(CARD_BG);
        row.setBorder(new EmptyBorder(10, 14, 10, 14));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Rank badge
        JLabel rankBadge = new JLabel(String.valueOf(rank));
        rankBadge.setFont(FONT_BADGE);
        rankBadge.setForeground(rank == 1 ? ACCENT2 : TEXT_SEC);
        rankBadge.setPreferredSize(new Dimension(20, 20));

        // Word
        JLabel wordLabel = new JLabel(capitalise(word));
        wordLabel.setFont(FONT_BODY);
        wordLabel.setForeground(TEXT_PRI);

        // Frequency bar (visual)
        int freq = trie.search(word.toLowerCase());
        JLabel freqLabel = new JLabel("★ " + Math.max(freq - 1, 0));  // search() increments; compensate
        freqLabel.setFont(FONT_SMALL);
        freqLabel.setForeground(ACCENT);

        row.add(rankBadge, BorderLayout.WEST);
        row.add(wordLabel,  BorderLayout.CENTER);
        row.add(freqLabel,  BorderLayout.EAST);

        // Click → populate search field
        row.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                searchField.setForeground(TEXT_PRI);
                searchField.setText(capitalise(word));
                loader.recordSearch(word);
                updateRecentLabel();
            }
            public void mouseEntered(MouseEvent e) { row.setBackground(new Color(32, 40, 62)); }
            public void mouseExited(MouseEvent e)  { row.setBackground(CARD_BG); }
        });

        return row;
    }

    private JSeparator buildDivider() {
        JSeparator sep = new JSeparator();
        sep.setForeground(BORDER_C);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        return sep;
    }

    private void showPlaceholder() {
        suggestionsPanel.removeAll();
        JLabel ph = new JLabel("  Start typing to see completions…");
        ph.setForeground(TEXT_SEC);
        ph.setFont(FONT_BODY);
        ph.setBorder(new EmptyBorder(12, 14, 12, 14));
        suggestionsPanel.add(ph);
        suggestionsPanel.revalidate();
        suggestionsPanel.repaint();
    }

    private void updateRecentLabel() {
        List<String> recent = loader.getRecentSearches();
        if (recent.isEmpty()) { recentLabel.setText("—"); return; }
        String text = String.join("  ·  ",
            recent.stream().limit(8).map(SearchUI::capitalise).toList());
        recentLabel.setText(text);
    }

    // ── Utility ───────────────────────────────────────────────────────────

    private static String capitalise(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static JLabel sectionLabel(String text) {
        JLabel lbl = new JLabel(text.toUpperCase());
        lbl.setFont(new Font("SansSerif", Font.BOLD, 10));
        lbl.setForeground(TEXT_SEC);
        lbl.setBorder(new EmptyBorder(0, 0, 4, 0));
        return lbl;
    }

    private static void styleButton(JButton btn, Color color) {
        btn.setBackground(color);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(8, 16, 8, 16));
    }
}
