package searchengine.utils.snippet;

import lombok.Getter;
import lombok.Setter;
import searchengine.utils.morphology.LemmaFinder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
public class SnippetSearch {
    private static int lengthSnippet = 53;
    private static int numberSnippets = 3;

    public static String find(String text, Set<String> lemmas) {
        Set<String> requiredLemmas = new HashSet<>(lemmas);
        String[] words = text.split("([^а-яА-ЯA-Za-z])+");
        List<String> snippets = new ArrayList<>();
        LemmaFinder finder = new LemmaFinder();

        for (String word : words) {
            if (snippets.size() >= numberSnippets) break;
            List<String> normalFormsWord = finder.getNormalForms(word.toLowerCase());
            if (normalFormsWord.isEmpty()) continue;

            for (String lemma : requiredLemmas) {
                if (normalFormsWord.contains(lemma)) {
                    requiredLemmas.remove(lemma);
                    snippets.add(firstSnippetByWord(text, word));
                    break;
                }
            }
        }

        return String.join("<br />", snippets);
    }

    private static String firstSnippetByWord(String text, String word) {
        int start = text.indexOf(word);
        int end = start + word.length();
        int remainderLength = lengthSnippet - word.length();

        int tempEnd = end;
        int i = 0;
        while (i < remainderLength && tempEnd < text.length() - 1 &&
                text.charAt(tempEnd) != '.') {
            tempEnd++;
            i++;
        }

        if (text.charAt(tempEnd) != '.') {
            if ((end + remainderLength/2) < text.length() - 1) {
                end += remainderLength/2;
                remainderLength -= remainderLength/2;
            }
            else {
                remainderLength -= text.length() - end - 1;
                end = text.length() - 1;
            }
        }
        else {
            remainderLength -= ++i;
            end = tempEnd;
        }

        int tempStart = start;
        i = 0;
        while (i < remainderLength && tempStart > 0) {
            tempStart--;
            i++;
        }

        if (tempStart != 0 && text.charAt(tempStart - 1) != ' ') {
            for (int k = tempStart + 1; k < text.length() - 1; k++) {
                if (Character.isLetter(text.charAt(k)) && text.charAt(tempStart) == ' ') {
                    tempStart = k;
                    break;
                }
                tempStart++;
            }
        }
        start = tempStart;
        end++;

        return  text.substring(start, end).replaceAll(word,"<b>" + word + "</b>");
    }

}
