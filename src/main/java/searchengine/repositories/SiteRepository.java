package searchengine.repositories;


import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.Site;

@Repository
public interface SiteRepository extends CrudRepository<Site, Integer> {
    @Query("SELECT status FROM Site WHERE id =:id")
    String getStatusBySiteId(Integer id);

    Site findSiteByUrl(String url);
}
