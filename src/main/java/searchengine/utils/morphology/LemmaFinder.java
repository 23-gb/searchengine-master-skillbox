package searchengine.utils.morphology;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.english.EnglishLuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;

import java.io.IOException;
import java.util.*;

public class LemmaFinder {
    private LuceneMorphology luceneMorphologyRu;
    private LuceneMorphology luceneMorphologyEn;
    private static final String[] particlesNames = new String[]{"МЕЖД", "ПРЕДЛ", "СОЮЗ", "ЧАСТ",
            "ARTICLE", "CONJ", "PREP"};

    public LemmaFinder() {
        try {
            luceneMorphologyRu = new RussianLuceneMorphology();
            luceneMorphologyEn = new EnglishLuceneMorphology();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    public Map<String, Integer> collectLemmas(String text) {
        String[] words = arrayContainsWords(text);
        HashMap<String, Integer> lemmas = new HashMap<>();

        for (String word : words) {
            if (word.isBlank()) continue;

            List<String> wordBaseForms = getMorphInfo(word);
            if (wordBaseForms.isEmpty() || anyWordBaseBelongToParticle(wordBaseForms)) {
                continue;
            }

            List<String> normalForms = getNormalForms(word);
            if (normalForms.isEmpty()) {
                continue;
            }

            String normalWord = normalForms.get(0);

            if (lemmas.containsKey(normalWord)) {
                lemmas.put(normalWord, lemmas.get(normalWord) + 1);
            }
            else {
                lemmas.put(normalWord, 1);
            }
        }

        return lemmas;
    }

    private boolean anyWordBaseBelongToParticle(List<String> wordBaseForms) {
        return wordBaseForms.stream().anyMatch(LemmaFinder::isParticle);
    }

    private static boolean isParticle(String wordBase) {
        for (String property : particlesNames) {
            if (wordBase.contains(property)) {
                return true;
            }
        }
        return false;
    }

    private List<String> getMorphInfo(String word) {
        if (word.matches("[а-я]+")) {
            return luceneMorphologyRu.getMorphInfo(word);
        }
        else if (word.matches("[a-z]+")) {
            return luceneMorphologyEn.getNormalForms(word);
        }
        return new ArrayList<>();
    }

    public List<String> getNormalForms(String word) {
        if (word.matches("[а-я]+")) {
            return luceneMorphologyRu.getNormalForms(word);
        }
        else if (word.matches("[a-z]+")) {
            return luceneMorphologyEn.getNormalForms(word);
        }
        return new ArrayList<>();
    }

    private String[] arrayContainsWords(String text) {
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("([^а-яa-z\\s])", " ")
                .trim()
                .split("\\s+");
    }

}
