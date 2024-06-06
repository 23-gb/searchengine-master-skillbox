package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.SiteStatus;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final SitesList sites;
    private final SiteRepository repositorySite;
    private final PageRepository repositoryPage;
    private final LemmaRepository repositoryLemma;

    @Override
    public StatisticsResponse getStatistics() {
        TotalStatistics total = new TotalStatistics();
        total.setSites(sites.getSites().size());
        total.setIndexing(false);

        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        List<Site> sitesList = sites.getSites();
        for(int i = 0; i < sitesList.size(); i++) {
            searchengine.model.Site siteDB = repositorySite.findSiteByUrl(sitesList.get(i).getUrl());
            if (siteDB != null && siteDB.getStatus().equals(SiteStatus.INDEXING)) {
                total.setIndexing(true);
            }
            Site site = sitesList.get(i);
            DetailedStatisticsItem item = new DetailedStatisticsItem();
            item.setName(site.getName());
            item.setUrl(site.getUrl());
            int pagesCount = siteDB == null ? 0 : repositoryPage.countBySite(siteDB.getId());
            int lemmas = siteDB == null ? 0 : repositoryLemma.countBySite(siteDB.getId());
            item.setPages(pagesCount);
            item.setLemmas(lemmas);
            String status = siteDB == null ? "" : siteDB.getStatus().toString();
            item.setStatus(status);
            String siteError = siteDB != null ? siteDB.getLastError() : "Требуется индексация сайта";
            siteError = siteDB != null && siteDB.getLastError() == null ? " Ошибок не обнаружено" : siteError;
            item.setError(siteError);
            long statusTime = siteDB == null ? new Date().getTime() : siteDB.getStatusTime().getTime();
            item.setStatusTime(statusTime);
            total.setPages(total.getPages() + pagesCount);
            total.setLemmas(total.getLemmas() + lemmas);
            detailed.add(item);
        }

        StatisticsResponse response = new StatisticsResponse();
        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(detailed);
        response.setStatistics(data);
        response.setResult(true);
        return response;
    }
}
