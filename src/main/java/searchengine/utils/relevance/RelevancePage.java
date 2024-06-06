package searchengine.utils.relevance;

import lombok.Getter;
import searchengine.model.Page;

import java.util.HashMap;
import java.util.Map;

@Getter
public class RelevancePage {
    private final Page page;
    private final Map<String, Float> rankWords;
    private Float relevance;

    public RelevancePage(Page page) {
        this.page = page;
        rankWords = new HashMap<>();
    }

    public float getAbsRelevance() {
        return rankWords.values().stream().reduce(0.0f, Float::sum);
    }

    public void putRankWord(String word, Float rank) {
        rankWords.put(word, rank);
    }

    public void setRelevance(Float rel) {
        relevance = rel;
    }

}
