package searchengine.services;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.SitesList;
import searchengine.config.UserAgents;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.model.*;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.utils.index.SiteIndexer;
import searchengine.utils.morphology.LemmaIndexer;
import searchengine.utils.parse.LinkParser;

import java.io.IOException;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService{

    private final SiteRepository repositorySite;
    private final PageRepository repositoryPage;
    private final LemmaRepository repositoryLemma;
    private final IndexRepository repositoryIndex;
    private final SitesList sitesList;
    private final UserAgents userAgent;

    @Override
    public IndexingResponse startIndexing() {
        if (isIndexing()) {
            return new IndexingResponse("Индексация уже запущена");
        }

        System.out.println("** START INDEXING ** " + LocalTime.now().truncatedTo(ChronoUnit.SECONDS));

        repositoryIndex.deleteAll();
        repositoryLemma.deleteAll();
        repositoryPage.deleteAll();
        repositorySite.deleteAll();

        for (searchengine.config.Site site : sitesList.getSites()) {
            new Thread(() -> {
                Site mainSite = new Site();
                mainSite.setStatus(SiteStatus.INDEXING);
                mainSite.setStatusTime(new Date());
                mainSite.setUrl(site.getUrl());
                mainSite.setName(site.getName());
                repositorySite.save(mainSite);

                Set<String> allLinks = ConcurrentHashMap.newKeySet();
                allLinks.add(realUrl(mainSite.getUrl()));
                ForkJoinPool pool = new ForkJoinPool();
                pool.invoke(new SiteIndexer(pool, repositorySite, repositoryPage, repositoryLemma, repositoryIndex,
                        mainSite, realUrl(mainSite.getUrl()), allLinks, userAgent));
            }).start();
        }
        return new IndexingResponse();
    }

    @Override
    public IndexingResponse stopIndexing() {
        if (!isIndexing()){
            return new IndexingResponse("Индексация не запущена");
        }

        SiteIndexer.stopIndexing();
        System.out.println("** STOP INDEXING ** " + LocalTime.now().truncatedTo(ChronoUnit.SECONDS));

        List<Site> siteList = (List<Site>) repositorySite.findAll();
        for (Site site : siteList) {
            if (site.getStatus() == SiteStatus.INDEXING) {
                site.setStatus(SiteStatus.FAILED);
                site.setStatusTime(new Date());
                site.setLastError("Индексация остановлена пользователем");
                repositorySite.save(site);
            }
        }

        return new IndexingResponse();
    }

    @Override
    public synchronized IndexingResponse indexPage(String url){
        if (url.trim().isEmpty()) {
            return new IndexingResponse("Страница не указана");
        }
        url = realUrl(url).trim();

        searchengine.config.Site siteCfg = findSiteCfgByUrl(url);
        if (siteCfg == null){
            return new IndexingResponse("Данная страница находится за пределами сайтов, " +
                    "указанных в конфигурационном файле");
        }

        String siteUrl = siteCfg.getUrl();
        Site mainSite = repositorySite.findSiteByUrl(siteUrl);
        if (mainSite == null) {
            return new IndexingResponse("Сайт данной страницы не был проиндексирован. " +
                    "Требуется индексация!");
        }

        String path = "/" + url.replaceAll(realUrl(mainSite.getUrl()), "");

        Page oldPage = repositoryPage.findPageByPathAndSite(path, mainSite);
        if (oldPage != null){
            deletePage(oldPage);
        }

        LinkParser parser = new LinkParser(mainSite, url, userAgent);
        return resultIndexingPage(parser);
    }

    private IndexingResponse resultIndexingPage(LinkParser parser){
        Site mainSite = parser.getSite();
        String url = parser.getUrl();
        try {
            parser.parse();
            int code = parser.getCode();
            if (code >= 400 && code <= 599) {
                return new IndexingResponse("Код ответа страницы: " + code);
            }else {
                Page page = savePage(parser);
                new LemmaIndexer(repositoryLemma, repositoryIndex, mainSite, page).indexing();
                System.out.println("** PAGE " + url + " IS INDEXED ** " + LocalTime.now().truncatedTo(ChronoUnit.SECONDS));
                mainSite.setStatusTime(new Date());
                repositorySite.save(mainSite);

                return new IndexingResponse();
            }
        } catch (IOException e) {
            mainSite.setStatus(SiteStatus.FAILED);
            mainSite.setStatusTime(new Date());
            mainSite.setLastError(e + ": " + e.getMessage());
            repositorySite.save(mainSite);
            System.out.println("Данная ссылка привела к ошибке: " + url);
            e.printStackTrace();
            return new IndexingResponse("Индексация ссылки привела к ошибке. " + e + ": " + e.getMessage());
        }
    }

    private boolean isIndexing() {
        List<Site> siteList = (List<Site>) repositorySite.findAll();
        for (Site site : siteList) {
            if (site.getStatus().equals(SiteStatus.INDEXING)) {
                return true;
            }
        }
        return false;
    }

    private String realUrl(String url) {
        String realUrl = url.replace("www.", "");
        realUrl = realUrl.endsWith("/") ? realUrl : realUrl + "/";
        return realUrl;
    }
    private searchengine.config.Site findSiteCfgByUrl(String url){
        for (searchengine.config.Site site : sitesList.getSites()){
            if (url.contains(realUrl(site.getUrl()))){
                return site;
            }
        }
        return null;
    }

    private void deletePage(Page page){
        List<Index> indexesOfPage = repositoryIndex.findByPage(page);
        indexesOfPage.forEach(index ->{
            Lemma lemma = index.getLemma();
            lemma.setFrequency(lemma.getFrequency() - 1);
            repositoryLemma.save(lemma);
        });
        repositoryIndex.deleteAll(indexesOfPage);
        repositoryPage.delete(page);
    }

    private Page savePage(LinkParser parser){
        Page page = new Page();
        page.setSite(parser.getSite());
        page.setPath(parser.getPath());
        page.setCode(parser.getCode());
        page.setContent(parser.getContent());
        repositoryPage.save(page);
        return page;
    }
}
