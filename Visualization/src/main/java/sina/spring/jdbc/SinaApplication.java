package sina.spring.jdbc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import javax.sql.DataSource;
import java.io.PrintStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
/**
 * Created by zzzzddddgtuwup on 2/18/15.
 */

@EnableAutoConfiguration
@ComponentScan
@RestController("/")
public class SinaApplication {
    public static final String NEO4J_URL = System.getProperty("NEO4J_URL","jdbc:neo4j://localhost:7474");


    @Autowired
    JdbcTemplate template;

    public static final String GRAPH_QUERY = "MATCH (m:Movie)<-[:ACTED_IN]-(a:Person) " +
            " RETURN m.title as movie, collect(a.name) as cast " +
            " LIMIT {1}";

    @RequestMapping("/graph")
    public Map<String, Object> graph(@RequestParam(value = "limit",required = false) Integer limit) {
        Iterator<Map<String, Object>> result = template.queryForList(
                GRAPH_QUERY, limit == null ? 100 : limit).iterator();
        return toD3Format(result);
    }

    private Map<String, Object> toD3Format(Iterator<Map<String, Object>> result) {
        List<Map<String,Object>> nodes = new ArrayList<Map<String,Object>>();
        List<Map<String,Object>> rels= new ArrayList<Map<String,Object>>();
        int i=0;
        while (result.hasNext()) {
            Map<String, Object> row = result.next();
            nodes.add(map("title",row.get("movie"),"label","movie"));
            int target=i;
            i++;
            for (Object name : (Collection) row.get("cast")) {
                Map<String, Object> actor = map("title", name,"label","actor");
                int source = nodes.indexOf(actor);
                if (source == -1) {
                    nodes.add(actor);
                    source = i++;
                }
                rels.add(map("source",source,"target",target));
            }
        }
        return map("nodes", nodes, "links", rels);
    }

    public static void main(String[] args) throws Exception {
        System.setErr(new PrintStream(System.out) {
            @Override
            public void write(int b) {
                super.write(b);
            }

            @Override
            public void write(byte[] buf, int off, int len) {
                super.write(buf, off, len);
            }
        });
        new SpringApplicationBuilder(SinaApplication.class).run(args);
    }

    @Bean
    public DataSource dataSource() {
        return new DriverManagerDataSource(NEO4J_URL);
    }
}
