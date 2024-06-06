package searchengine.repositories;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;

import java.util.List;

@Repository
public interface PageRepository extends CrudRepository<Page, Integer> {
    Page findPageByPathAndSite(String path, Site site);
    @Query("SELECT COUNT(*) FROM Page WHERE site_id =:siteId")
    Integer countBySite(Integer siteId);
}
