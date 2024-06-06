package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.dto.search.DataSearchItem;
import searchengine.dto.search.SearchResponse;
import searchengine.model.*;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.utils.morphology.LemmaFinder;
import searchengine.utils.relevance.RelevancePage;
import searchengine.utils.snippet.SnippetSearch;

import java.util.*;


@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    @Autowired
    private SiteRepository repositorySite;
    @Autowired
    private PageRepository repositoryPage;
    @Autowired
    private LemmaRepository repositoryLemma;
    @Autowired
    private IndexRepository repositoryIndex;

    @Override
    public SearchResponse getSearch(String query, String siteUrl, Integer offset, Integer limit) {

        if (query.isEmpty()){
            return new SearchResponse("Запрос не введен");
        }

        offset = (offset == null) ? 0 : offset;
        limit = (limit == null) ? 20 : limit;


        LemmaFinder finder = new LemmaFinder();
        Set<String> queryLemmas = finder.collectLemmas(query).keySet();
        List<Index> foundIndexes;

        if (siteUrl == null) {
            List<Site> allSites = (List<Site>) repositorySite.findAll();
            for (Site site : allSites){
                if (site.getStatus() == SiteStatus.INDEXING) {
                    return new SearchResponse("Дождитесь окончания индексации");
                }
            }
            foundIndexes = searchByAll(queryLemmas, allSites);
        }
        else {
            Site site = repositorySite.findSiteByUrl(siteUrl);
            if (site.getStatus() != SiteStatus.INDEXED){
                return new SearchResponse("Выбранный сайт ещё не проиндексирован");
            }
            foundIndexes = searchBySite(queryLemmas, site);
        }
        if (foundIndexes.isEmpty()){
            return new SearchResponse("Ничего не найдено");
        }

        List<DataSearchItem> data = getDataList(foundIndexes);

        return buildResponse(offset, limit, data);
    }

    private List<Index> searchByAll(Set<String> queryLemmas, List<Site> sites) {
        List<Index> indexList = new ArrayList<>();
        sites.forEach(site -> {
            indexList.addAll(searchBySite(queryLemmas, site));
        });
        return indexList;
    }

    private List<Index> searchBySite(Set<String> queryLemmas, Site site) {
        List<Lemma> lemmas = repositoryLemma.selectLemmasBySite(queryLemmas, site);
        if (queryLemmas.size() != lemmas.size()){
            return new ArrayList<>();
        }

        if (lemmas.size() == 1){
            return repositoryIndex.findByLemma(lemmas.get(0));
        }

        List<Page> allPages = repositoryIndex.findPagesByLemma(lemmas.get(0));
        for (int i = 1; i < lemmas.size(); i++){
            if (allPages.isEmpty()){
                return new ArrayList<>();
            }
            List<Page> pagesOfLemma = repositoryIndex.findPagesByLemma(lemmas.get(i));
            allPages.removeIf(page -> !pagesOfLemma.contains(page));
        }
        return repositoryIndex.findByLemmasAndPages(lemmas, allPages);
    }

    private List<DataSearchItem> getDataList(List<Index> indexes) {
        List<RelevancePage> relevancePages = getRelevantList(indexes);
        List<DataSearchItem> result = new ArrayList<>();

        for (RelevancePage page : relevancePages) {
            DataSearchItem item = new DataSearchItem();
            item.setSite(page.getPage().getSite().getUrl());
            item.setSiteName(page.getPage().getSite().getName());
            item.setUri(page.getPage().getPath());

            String title = Jsoup.parse(page.getPage().getContent()).title();
            if (title.length() > 50) {
                title = title.substring(0,50).concat("...");
            }
            item.setTitle(title);
            item.setRelevance(page.getRelevance());

            String titles = Jsoup.parse(page.getPage().getContent()).title();
            String body = Jsoup.parse(page.getPage().getContent()).body().text();
            String text = titles.concat(body);
            item.setSnippet( SnippetSearch.find(text, page.getRankWords().keySet()) );

            result.add(item);
        }

        return result;
    }

    private List<RelevancePage> getRelevantList(List<Index> indexes) {
        List<RelevancePage> pageSet = new ArrayList<>();

        for (Index index : indexes) {
            RelevancePage existingPage = pageSet.stream().filter(temp -> temp.getPage().equals(index.getPage())).findFirst().orElse(null);
            if (existingPage != null) {
                existingPage.putRankWord(index.getLemma().getLemma(), index.getRank());
                continue;
            }

            RelevancePage page = new RelevancePage(index.getPage());
            page.putRankWord(index.getLemma().getLemma(), index.getRank());
            pageSet.add(page);

        }

        float maxRelevance = 0.0f;

        for (RelevancePage page : pageSet) {
            float absRelevance = page.getAbsRelevance();
            if (absRelevance > maxRelevance) {
                maxRelevance = absRelevance;
            }
        }

        for (RelevancePage page : pageSet) {
            page.setRelevance(page.getAbsRelevance() / maxRelevance);
        }

        pageSet.sort(Comparator.comparingDouble(RelevancePage::getRelevance).reversed());
        return pageSet;
    }

    private SearchResponse buildResponse(Integer offset, Integer limit, List<DataSearchItem> data) {
        if (offset + limit >= data.size()) {
            limit = data.size() - offset;
        }
        return new SearchResponse(data.size(), data.subList(offset, offset + limit));
    }
}
