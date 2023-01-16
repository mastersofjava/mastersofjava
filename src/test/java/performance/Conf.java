package performance;

public class Conf {

    // number of users
    public static int users = 1;
    // time in seconds that users starting up
    public static long ramp = 1;

    public final static String keyCloakUrl = "localhost:8888";
    public final static String mojServerUrl = "localhost:8080";

    // The secret of the keyCloak client named 'gatling' with 'Client authentication' set to true
    public static final String keyCloakClientSecret = "2OTUkmLo2QxpjePtmQ8irIXsEyIKsO9g";

    public static final String keyCloakAdminUsername = "admin";
    public static final String keyCloakAdminPassword = "admin";


    public static final String emptyCode = """
            import java.util.List;
                        
            /**
             * This class makes the super awesome list of names
             */
            public class SuperAwesomeNameService {
                        
                public List<String> doTheThings(List<String> names) {
                    // TODO: implement here
                    return names;
                }
                        
            }
            """;
    public static final String correctSolution = """
            import java.util.Comparator;
            import java.util.List;
                        
            import static java.util.Comparator.comparing;
                        
            /**
             * This class makes the super awesome list of names
             */
            public class SuperAwesomeNameService {
                public static List<String> doTheThings(List<String> names) {
                    names.sort(comparing(String::toLowerCase));
                    for (int i = 1; i < names.size(); i++) {
                        if (names.get(i - 1).contains("a")) {
                            names.set(i, reverseAndCapitalize(names.get(i)));
                        }
                    }
                    return names;
                }
                        
                private static String reverseAndCapitalize(String name) {
                    var reversedLowerCase = new StringBuilder(name).reverse().toString().toLowerCase();
                    return reversedLowerCase.substring(0,1).toUpperCase() + reversedLowerCase.substring(1).toLowerCase();
                }
            }
                        
            """;


}
