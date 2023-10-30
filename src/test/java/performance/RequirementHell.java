package performance;

public class RequirementHell implements TestAssignment {

    private final String[] attempts = {
            """
            import java.util.List;
                        
            /**
             * This class makes the super awesome list of names
             */
            public class SuperAwesomeNameService {
                        
                public List<String> doTheThings(List<String> names) {
                    return names.stream().sorted().toList();
                }
                        
            }
            """,
            """
            import java.util.List;
                        
            /**
             * This class makes the super awesome list of names
             */
            public class SuperAwesomeNameService {
                        
                public List<String> doTheThings(List<String> names) {
                    return names.stream().sorted((a,b)->a.toLowerCase().compareTo(b.toLowerCase())).toList();
                }
                        
            }
            """,
            """
            import java.util.List;
                        
            /**
             * This class makes the super awesome list of names
             */
            public class SuperAwesomeNameService {
                        
                public List<String> doTheThings(List<String> names) {
                    return names.stream()
                      .sorted((a,b)->a.toLowerCase().compareTo(b.toLowerCase()))
                      .filter(a -> !a.contains("e"))
                      .toList();
                }
                        
            }
            """,
            """
            import java.util.List;
                        
            /**
             * This class makes the super awesome list of names
             */
            public class SuperAwesomeNameService {
                        
                public List<String> doTheThings(List<String> names) {
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
                private boolean hasVowel(String input) {
                    return input.contains("aa")
                      || input.contains("ae")
                      || input.contains("ai")
                      || input.contains("ao")
                      || input.contains("au")
                      || input.contains("ea")
                      || input.contains("ee")
                      || input.contains("ei")
                      || input.contains("eo")
                      || input.contains("eu")
                      || input.contains("ia")
                      || input.contains("ie")
                      || input.contains("ii")
                      || input.contains("io")
                      || input.contains("iu")
                      || input.contains("oa")
                      || input.contains("oe")
                      || input.contains("oi")
                      || input.contains("oo")
                      || input.contains("ou")
                      || input.contains("ua")
                      || input.contains("ue")
                      || input.contains("ui")
                      || input.contains("uo")
                      || input.contains("uu");
                }
                        
            }
            """,
            """
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
                
              public String reverse(String input){
                System.out.println("+++++++" + input);
              if (input.length() == 1) {
                return input;
              }
                return reverse(input.substring(1,input.length())) + input.charAt(0);
              }
                        
            }
            """
    };

    @Override
    public String getAssignmentName() {
        return "requirement hell";
    }

    @Override
    public int getTabCount() {
        return 9;
    }

    @Override
    public String getDoesNotCompile() {
        return """
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
    }

    @Override
    public String getEmpty() {
        return """
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
    }


    @Override
    public String getSolution() {
        return """
                import java.util.Comparator;
                 import java.util.List;
                 
                 import static java.util.Comparator.comparing;
                 
                 /**
                  * This class makes the super awesome list of names
                  */
                 public class SuperAwesomeNameService {
                     public List<String> doTheThings(List<String> names) {
                         names.sort(comparing(String::toLowerCase));
                         for (int i = 1; i < names.size(); i++) {
                             if (names.get(i - 1).contains("a")) {
                                 names.set(i, reverseAndCapitalize(names.get(i)));
                             }
                         }
                         return names;
                     }
                 
                     private String reverseAndCapitalize(String name) {
                         var reversedLowerCase = new StringBuilder(name).reverse().toString().toLowerCase();
                         return reversedLowerCase.substring(0,1).toUpperCase() + reversedLowerCase.substring(1).toLowerCase();
                     }
                }
                """;
    }

    @Override
    public int getAttempts() {
        return attempts.length;
    }

    @Override
    public String getAttempt(int number) {
        return attempts[number];
    }
}
