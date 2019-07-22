import org.junit.Assert;
import org.junit.Test;

public class TestSecurityPolicy {

    @Test
    public void suddenlyNotIllegal() throws Exception {
        System.setProperty("java.version", "First8 Special Java");
        String version = System.getProperty("java.version");
        Assert.assertEquals("First8 Special Java", version);
    }
}