package searchengine.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "user-agent-settings")
public class UserAgents {
    private List<String> users;
    private String referrer;

    public String getUser(int index){
        return users.get(index);
    }
}
