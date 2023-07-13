package letscode.spring;

import lombok.extern.slf4j.Slf4j;
import org.postgresql.Driver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

@Slf4j
public class MyApp {
    /*
     * Cuando alguien llama un metodo de este proxy obtenemos un objeto de tipo
     * OrganizationService
     */
    public static  OrganizationService transactionalOrganizationService(
            OrganizationService delegate, TransactionTemplate tt){
        var transactionalCustomerService = Proxy.newProxyInstance(ClassLoader.getSystemClassLoader(),
                new Class[]{OrganizationService.class}, new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        log.info("Invoking " + method.getName() + " with arguments " + args);
                        return tt.execute(status -> {
                            try {
                                return method.invoke(delegate, args);
                            } catch (IllegalAccessException e) {
                                throw new RuntimeException(e);
                            } catch (InvocationTargetException e) {
                                throw new RuntimeException(e);
                            }
                        });
                    }
                });
        return (OrganizationService) transactionalCustomerService;
    }
    public static void main(String[] args) {
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

        var cs = transactionalOrganizationService(new DefaultOrganizationService(template), tt);

        var sandvik = cs.add(1000, 10, "T", "Store Central", "Store Central", "Garden World Store Central");
        var fargo = cs.add(2000, 10, "T", "Store North", "Store North", "Garden World Store North");
        //var com = cs.add(3000, 10, "T", "WH North", "WH Store North", "Garden World Store North");

        var allData = cs.getAllOrg();
        Assert.state(allData.contains(sandvik) && allData.contains(fargo), "we can not add the records successfully");
        allData.forEach(org -> log.info(org .toString()));
    }
}

interface OrganizationService{
    public Organization add(int ad_org_id, int ad_client_id, String isActive, String value, String name, String description);
    public Collection<Organization> getAllOrg();
    public Organization byId(Integer id);
}

@Slf4j
class DefaultOrganizationService implements OrganizationService{
    private final JdbcTemplate template;
    private final RowMapper<Organization> organizationRowMapper = (rs, rowNum)-> new Organization(
            rs.getInt("ad_org_id"),
            rs.getInt("ad_client_id"),
            rs.getString("isActive"),
            rs.getString("value"),
            rs.getString("name"),
            rs.getString("description")
    );

    public DefaultOrganizationService(JdbcTemplate template){
        this.template = template;
    }
    @Override
    public Organization add(int ad_org_id, int ad_client_id, String isActive, String value, String name, String description) {
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
    }

    @Override
    public Collection<Organization> getAllOrg() {
        return template.query("select ad_org_id,ad_client_id,isActive,value,name,description from public.ad_org", organizationRowMapper);
    }

    @Override
    public Organization byId(Integer id) {
        return template.queryForObject("select ad_org_id,ad_client_id,isActive,value,name,description from public.ad_org where id=?", organizationRowMapper, id);
    }
}

record Organization(
        Integer ad_org_id,
        Integer ad_client_id,
        String isActive,
        String value,
        String name,
        String description
){}
