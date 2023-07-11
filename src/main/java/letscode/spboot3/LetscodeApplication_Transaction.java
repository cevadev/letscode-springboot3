package letscode.spboot3;


import lombok.extern.slf4j.Slf4j;
import org.postgresql.Driver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;

import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

@Slf4j
public class LetscodeApplication_Transaction {
    public static void main(String[] args) throws Exception {
        var dataSource = new DriverManagerDataSource(
                "jdbc:postgresql://localhost:5432/localtest",
                "adempiere",
                "adempiere"
        );
        dataSource.setDriverClassName(Driver.class.getName());
        var template = new JdbcTemplate(dataSource);
        template.afterPropertiesSet();

        var ptm = new DataSourceTransactionManager(dataSource);
        ptm.afterPropertiesSet();

        var tt = new TransactionTemplate(ptm);
        tt.afterPropertiesSet();

        var cs = new DefaultCustomerServices_Transaction(template, tt);
        var sandvik = cs.add(1000, 10, "T", "Store Central", "Store Central", "Garden World Store Central");
        var fargo = cs.add(2000, 10, "T", "Store North", "Store North", "Garden World Store North");
        var com = cs.add(3000, 10, "T", "WH North", "WH Store North", "Garden World Store North");

        var allData = cs.getAllOrg();
        Assert.state(allData.contains(sandvik) && allData.contains(fargo) && allData.contains(com), "we can not add the records successfully");
        allData.forEach(org -> log.info(org .toString()));
    }
}

@Slf4j
class DefaultCustomerServices_Transaction {
    private final JdbcTemplate template;
    private final TransactionTemplate tt;
    private final RowMapper<Organization_Transaction> organizationRowMapper =
            (rs, rowNum) -> new Organization_Transaction(
                    rs.getInt("ad_org_id"),
                    rs.getInt("ad_client_id"),
                    rs.getString("isActive"),
                    rs.getString("value"),
                    rs.getString("name"),
                    rs.getString("description")
            );

    public DefaultCustomerServices_Transaction(JdbcTemplate template, TransactionTemplate tt) {
        this.template = template;
        this.tt = tt;
    }

    Collection<Organization_Transaction> getAllOrg() throws Exception {
        return this.tt.execute(new TransactionCallback<Collection<Organization_Transaction>>() {
            @Override
            public Collection<Organization_Transaction> doInTransaction(TransactionStatus status) {
                return template.query("select ad_org_id,ad_client_id,isActive,value,name,description from public.ad_org", organizationRowMapper);
            }
        });
    }

    Organization_Transaction add(int ad_org_id, int ad_client_id, String isActive, String value, String name, String description) {
        return this.tt.execute(status -> {
            Assert.isTrue(!name.startsWith("WH"), "The name can not start with WH");
            var al = new ArrayList<Map<String, Object>>();
            al.add(Map.of("id", Integer.class));
            var keyHolder = new GeneratedKeyHolder(al);
            template.update(
                    con -> {
                        var ps = con.prepareStatement("""
                                insert into public.ad_org (ad_org_id,ad_client_id,isActive,value,name,description) values(?,?,?,?,?,?)
                                on conflict on constraint ad_org_pk2 do update set name = excluded.name
                                """, Statement.RETURN_GENERATED_KEYS);
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
            Number number = (Number) generatedId;
            return byId(number.intValue());
        });
    }

    Organization_Transaction byId(Integer id){
        return this.tt.execute(new TransactionCallback<Organization_Transaction>() {
            @Override
            public Organization_Transaction doInTransaction(TransactionStatus status) {
                return template.queryForObject("select ad_org_id,ad_client_id,isActive,value,name,description from public.ad_org where id=?", organizationRowMapper, id);
            }
        });
    }
}

record Organization_Transaction(
        Integer ad_org_id,
        Integer ad_client_id,
        String isActive,
        String value,
        String name,
        String description
){}
