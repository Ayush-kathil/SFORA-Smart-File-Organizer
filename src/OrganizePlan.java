import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class OrganizePlan {

    private final List<FileMoveCandidate> moves;
    private final Map<String, Integer> categoryCounts;
    private final String mode;

    public OrganizePlan(String mode, List<FileMoveCandidate> moves, Map<String, Integer> categoryCounts) {
        this.mode = mode;
        this.moves = new ArrayList<>(moves);
        this.categoryCounts = new LinkedHashMap<>(categoryCounts);
    }

    public List<FileMoveCandidate> getMoves() {
        return Collections.unmodifiableList(moves);
    }

    public Map<String, Integer> getCategoryCounts() {
        return Collections.unmodifiableMap(categoryCounts);
    }

    public String getMode() {
        return mode;
    }

    public int getMoveCount() {
        return moves.size();
    }

    public boolean isEmpty() {
        return moves.isEmpty();
    }

    public String describe() {
        StringBuilder builder = new StringBuilder();
        builder.append("Preview mode: ").append(mode).append('\n');
        builder.append("Planned moves: ").append(getMoveCount()).append('\n');
        if (!categoryCounts.isEmpty()) {
            builder.append("Category summary:\n");
            List<String> categories = new ArrayList<>(categoryCounts.keySet());
            Collections.sort(categories);
            for (String category : categories) {
                builder.append(" - ").append(category).append(": ").append(categoryCounts.get(category)).append('\n');
            }
        }
        return builder.toString();
    }
}
