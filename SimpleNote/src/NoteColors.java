public class NoteColors {
    private static final String[] FUNDO = {
        "#fff2ba", "#ffe2e8", "#dcecff", "#dcf3df", "#ebe3ff", "#ffe7d2"
    };
    private static final String[] BORDA = {
        "#dcb947", "#db9dac", "#9cbadd", "#9bcba1", "#b5a4db", "#e0ae82"
    };

    public static String fundo(int cor) { return FUNDO[indice(cor)]; }
    public static String borda(int cor) { return BORDA[indice(cor)]; }
    private static int indice(int cor) { return Math.max(0, Math.min(5, cor - 1)); }
}
