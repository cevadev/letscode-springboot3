package letscode.spboot3;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.sql.DataSource;

import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.*;

class DefaultCustomerServices_v1Test {
    @Test
    void all() throws Exception{
        var ds = Mockito.mock(DataSource.class);
        //var cs = new DefaultCustomerServices_v1(ds);
    }
}