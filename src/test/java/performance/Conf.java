package performance;

import java.util.function.Supplier;

import static performance.PerformanceTest.random;

public class Conf {

    // number of teams
    public static int teams = 50; // 50; (We streven nu naar 50 teams)
    // time in seconds that users starting up (reading the assignment etc)
    public static long ramp = 200; // 200; (Binnen 2 minuten heeft de helft van de teams voor het eerst getest, enkelen doen er veel langer over)
    // Every user will start with a compile, and then run 'x' attempts before submitting
    public static int attemptCount = 8; // 8; (een gemiddeld team doet 12,5 test pogingen, maar slechts 56% daarvan komt door de compile heen)
    public static Supplier<Integer> waitTimeBetweenSubmits = () -> random(0, 360); // 8 submits in 24 minuten = om de 3 minuten gemiddeld een submit.

    public final static String mojServerUrl = "localhost:8080"; // "mastersofjava.nljug";

    public final static String keyCloakUrl = "localhost:8888"; // "auth.mastersofjava.nljug";
    // The secret of the keyCloak client named 'gatling' with 'Client authentication' set to true
    public static final String keyCloakClientSecret = "LshUtyu5XOhyL15fF35hXF7pRtD8k8Tx"; // "mde0YixqQDgoeeXyyRoCk7aNMzozYic9";
    public static final String keyCloakAdminUsername = "admin"; // "keycloak";
    public static final String keyCloakAdminPassword = "admin"; // "7d597bacdfbc41b8b067e3a5eef009c1";


    public static final String assigmentName = "requirement hell";
    public static final String doesNotCompile = """
            import java.util.List;
                        
            /**
             * This class makes the super awesome list of names
             */
            public class SuperAwesomeNameService {
                        
                public List<String> doTheThings(List<String> names) {
                    // TODO: implement here
                }
                        
            }
            """;

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

    public static final String attempt1 = """
            import java.util.List;
                        
            /**
             * This class makes the super awesome list of names
             */
            public class SuperAwesomeNameService {
                        
                public List<String> doTheThings(List<String> names) {
                   \s
                    return names.stream().sorted().toList();
                }
                        
            }
                        
            """;
    public static final String attempt2 = """
            import java.util.List;
                        
            /**
             * This class makes the super awesome list of names
             */
            public class SuperAwesomeNameService {
                        
                public List<String> doTheThings(List<String> names) {
                   \s
                    return names.stream().sorted((a,b)->a.toLowerCase().compareTo(b.toLowerCase())).toList();
                }
                        
            }
                        
            """;
    public static final String attempt3 = """
            import java.util.List;
                        
            /**
             * This class makes the super awesome list of names
             */
            public class SuperAwesomeNameService {
                        
                public List<String> doTheThings(List<String> names) {
                   \s
                    return names.stream()
                      .sorted((a,b)->a.toLowerCase().compareTo(b.toLowerCase()))
                      .filter(a -> !a.contains("e"))
                      .toList();
                }
                        
            }
                        
            """;
    public static final String attempt4 = """
            import java.util.List;
                        
            /**
             * This class makes the super awesome list of names
             */
            public class SuperAwesomeNameService {
                        
                public List<String> doTheThings(List<String> names) {
                   \s
                    return names.stream()
                      .sorted((a,b)-> {
                        if (hasVowel(a) && !hasVowel(b)) {
                        	return -1;
                        } else if (!hasVowel(a) && hasVowel(b)) {
                        	return 1;
                        }
                        return a.toLowerCase().compareTo(b.toLowerCase()) ;
                                                                 })
                      .filter(a -> !a.contains("e"))
                      .toList();
                }
                           \s
                            private boolean hasVowel(String input) {
                            return input.contains("aa")
                              || input.contains("ae")
                              || input.contains("ai")
                              || input.contains("ao")
                              || input.contains("au")
                             \s
                              || input.contains("ea")
                              || input.contains("ee")
                              || input.contains("ei")
                              || input.contains("eo")
                              || input.contains("eu")
                             \s
                              || input.contains("ia")
                              || input.contains("ie")
                              || input.contains("ii")
                              || input.contains("io")
                              || input.contains("iu")
                             \s
                              || input.contains("oa")
                              || input.contains("oe")
                              || input.contains("oi")
                              || input.contains("oo")
                              || input.contains("ou")
                             \s
                              || input.contains("ua")
                              || input.contains("ue")
                              || input.contains("ui")
                              || input.contains("uo")
                              || input.contains("uu");
                            }
                        
            }
            """;
    public static final String attempt5 = """
            import java.util.List;
                        
            /**
             * This class makes the super awesome list of names
             */
            public class SuperAwesomeNameService {
                      String previous = "";
                        
                public List<String> doTheThings(List<String> names) {
                        
                    return names.stream()
                      .sorted((a,b)-> {
                        return a.toLowerCase().compareTo(b.toLowerCase()) ;
                                                                 })
                      .map(a -> {
                      	if (previous.contains("a")) {
                        return reverse(a);
                        } return a;
                      })
                      .peek(a -> previous = a)
                      .toList();
                }
             \s
              public String reverse(String input){
                System.out.println("+++++++" + input);
              if (input.length() == 1) {
                return input;
              }
                return reverse(input.substring(1,input.length())) + input.charAt(0);
              }
                        
            }
                        
            """;

    public static final String correctSolution = """
            import java.util.List;

            /**
             * This class makes the super awesome list of names
             */
            public class SuperAwesomeNameService {
                      String previous = "";

                public List<String> doTheThings(List<String> names) {

                    return names.stream()
                      .sorted((a,b)-> {
                        return a.toLowerCase().compareTo(b.toLowerCase()) ;
                                                                 })
                      .map(a -> {
                      	if (previous.contains("a")) {
                        return fixCapitals(reverse(a));
                        } return a;
                      })
                      .peek(a -> previous = a)
                      .toList();
                }
             \s
              public String reverse(String input){
              if (input.length() == 1) {
                return input;
              }
               \s
                return reverse(input.substring(1,input.length())) + input.charAt(0);
              }
             \s
              public String fixCapitals(String input) {
              	input = input.toLowerCase()
                ;
                return input.substring(0,1).toUpperCase() + input.substring(1, input.length());
              }

            }
                        """;

}
