import org.junit.Test;

public class Test1 {
    @Test
    public void suddenlyNotIllegal() throws Exception {
        System.setProperty("java.version", "First8 Special Java");
        String version = System.getProperty("java.version");
        System.out.println("Java version ----------->" + version);
    }
}