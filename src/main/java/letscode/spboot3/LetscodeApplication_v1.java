package letscode.spboot3;


import lombok.extern.slf4j.Slf4j;
import org.postgresql.Driver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.util.Assert;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

@Slf4j
public class LetscodeApplication_v1 {
    public static void main(String[] args) throws Exception {
        var dataSource = new DriverManagerDataSource(
                "jdbc:postgresql://localhost:5432/localtest",
                "adempiere",
                "adempiere"
        );
        dataSource.setDriverClassName(Driver.class.getName());
        var template = new JdbcTemplate(dataSource);

        var cs = new DefaultCustomerServices_v1(template);
        var sandvik = cs.add(1000, 10, "T", "Store Central", "Store Central", "Garden World Store Central");
        var fargo = cs.add(2000, 10, "T", "Store North", "Store North", "Garden World Store North");

        var allData = cs.getAllOrg();
        Assert.state(allData.contains(sandvik) && allData.contains(fargo), "");
        allData.forEach(org -> log.info(org .toString()));
    }
}


// THIS IS THE WRONG WAY
@Slf4j
class DefaultCustomerServices_v1 {
    private final JdbcTemplate template;
    private final RowMapper<Organization_v1> organizationRowMapper =
            (rs, rowNum) -> new Organization_v1(
                    rs.getInt("ad_org_id"),
                    rs.getInt("ad_client_id"),
                    rs.getString("isActive"),
                    rs.getString("value"),
                    rs.getString("name"),
                    rs.getString("description")
            );

    public DefaultCustomerServices_v1(JdbcTemplate template) {
        this.template = template;
    }

    Collection<Organization_v1> getAllOrg() throws Exception {
        var listOfOrganizations = this.template.query("select ad_org_id,ad_client_id,isActive,value,name,description from public.ad_org", this.organizationRowMapper);
        return listOfOrganizations;
    }

    Organization_v1 add(int ad_org_id, int ad_client_id, String isActive, String value, String name, String description) {
        var al = new ArrayList<Map<String, Object>>();
        al.add(Map.of("id", Integer.class));
        var keyHolder = new GeneratedKeyHolder(al);
        this.template.update(
                con -> {
                    var ps = con.prepareStatement("insert into public.ad_org " +
                            "(ad_org_id,ad_client_id,isActive,value,name,description) values(?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS);
                    ps.setInt(1, ad_org_id);
                    ps.setInt(2, ad_client_id);
                    ps.setString(3, isActive);
                    ps.setString(4, value);
                    ps.setString(5, name);
                    ps.setString(6, description);
                    return ps;
                }, keyHolder);
        var generatedId = keyHolder.getKeys().get("id");
        log.info("generatedId: {}", generatedId.toString());
        Assert.state(generatedId instanceof Number, "the generated id must be a number");
        if(generatedId instanceof Number number){
            return byId(number.intValue());
        }
        return null;
    }

    Organization_v1 byId(Integer id){
        return this.template.queryForObject("select ad_org_id,ad_client_id,isActive,value,name,description from public.ad_org where id=?", this.organizationRowMapper, id);
    }
}

record Organization_v1(
        Integer ad_org_id,
        Integer ad_client_id,
        String isActive,
        String value,
        String name,
        String description
){}
