package searchengine.utils.morphology;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;

import java.io.IOException;
import java.util.Map;

public class LemmaIndexer {
    @Autowired
    private LemmaRepository repositoryLemma;
    @Autowired
    private IndexRepository repositoryIndex;
    private Site site;
    private Page page;


    public LemmaIndexer(LemmaRepository repositoryLemma, IndexRepository repositoryIndex, Site site, Page page) {
        this.repositoryLemma = repositoryLemma;
        this.repositoryIndex = repositoryIndex;
        this.site = site;
        this.page = page;
    }

    public void indexing() throws IOException {
        Map<String, Integer> lemmas = getLemmasOfPage();
        lemmas.forEach(this::saveLemmaAndIndex);
    }

    private Map<String, Integer> getLemmasOfPage() throws IOException {
        LemmaFinder finder = new LemmaFinder();

        String content = page.getContent();

        Document document = Jsoup.parse(content);
        String titleTagText = document.title();
        String bodyTagText = document.body().text();
        String text = titleTagText + " " + bodyTagText;
        return finder.collectLemmas(text);
    }

    private void saveLemmaAndIndex(String lemma, Integer count) {
        synchronized (site) {
            Lemma lemmaDB = repositoryLemma.findByLemmaAndSite(lemma, site);
            if (lemmaDB == null) {
                Lemma lemmaNew = new Lemma();
                lemmaNew.setSite(site);
                lemmaNew.setLemma(lemma);
                lemmaNew.setFrequency(1);
                lemmaDB = repositoryLemma.save(lemmaNew);
            }
            else {
                lemmaDB.setFrequency(lemmaDB.getFrequency() + 1);
                repositoryLemma.save(lemmaDB);
            }
            Index index = new Index();
            index.setPage(page);
            index.setRank(count);
            index.setLemma(lemmaDB);
            repositoryIndex.save(index);
        }
    }
}
