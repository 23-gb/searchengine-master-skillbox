package searchengine.dto.search;

import lombok.Data;

import java.util.List;

@Data
public class SearchResponse {
    private boolean result;
    private Integer count;
    private List<DataSearchItem> data;
    private String error;

    public SearchResponse(int count, List<DataSearchItem> data) {
        result = true;
        this.count = count;
        this.data = data;
        error = null;
    }

    public SearchResponse(String error) {
        this.error = error;
        result = false;
        count = null;
        data = null;
    }
}
