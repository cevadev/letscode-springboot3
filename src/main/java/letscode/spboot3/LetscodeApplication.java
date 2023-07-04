package letscode.spboot3;


import lombok.extern.slf4j.Slf4j;
import org.postgresql.Driver;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Collection;
@Slf4j
public class LetscodeApplication {
    public static void main(String[] args) throws Exception {
        var cs = new DefaultCustomerServices();
        var allData = cs.getAllOrg();
        allData.forEach(org -> log.info(org .toString()));
    }
}


// THIS IS THE WRONG WAY
@Slf4j
class DefaultCustomerServices{
    private final DataSource dataSource;

    public DefaultCustomerServices(){
        var dataSource = new DriverManagerDataSource(
                "jdbc:postgresql://localhost:5432/idempiere",
                "adempiere",
                "adempiere"
        );
        dataSource.setDriverClassName(Driver.class.getName());
        this.dataSource = dataSource;
    }

    Collection<Organization> getAllOrg() throws Exception{
        var listOfOrganizations = new ArrayList<Organization>();
        try{
            try(var conn = this.dataSource.getConnection()){
                try(var stmt = conn.createStatement()){
                    try(var resultSet = stmt.executeQuery(
                            "select ad_org_id,ad_client_id, isActive,value,name,description from adempiere.ad_org")) {
                        while (resultSet.next()){
                            var org_id = resultSet.getInt("ad_org_id");
                            var client_id = resultSet.getInt("ad_client_id");
                            var isActive = resultSet.getString("isActive");
                            var value = resultSet.getString("value");
                            var name = resultSet.getString("name");
                            var description = resultSet.getString("description");
                            listOfOrganizations.add(new Organization(
                                    org_id,
                                    client_id,
                                    isActive,
                                    value,
                                    name,
                                    description
                            ));
                        }
                    }
                }
            }
        }
        catch (Exception e){
            log.error("something went terribly wrong, but search me, I have no idea what", e);
        }
        return listOfOrganizations;
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
