package performance;

import java.util.function.Supplier;

import static performance.PerformanceTest.random;

public class Conf {

    // number of teams
    public static int teams = 50; // 100; (We streven nu naar 100 teams)
    // time in seconds that users starting up (reading the assignment etc)
    public static long ramp = 30; // 200; (Binnen 2 minuten heeft de helft van de teams voor het eerst getest, enkelen doen er veel langer over)
    // Every user will start with a compile, and then run 'x' attempts before submitting
    public static int attemptCount = 16; // 8; (een gemiddeld team doet 12,5 test pogingen, maar slechts 56% daarvan komt door de compile heen)
    public static Supplier<Integer> waitTimeBetweenSubmits = () -> random(0, 60); // 8 submits in 24 minuten = om de 3 minuten gemiddeld een submit.

    public final static String mojServerUrl = "mastersofjava.nljug"; // "localhost:8080"; // "mastersofjava.nljug";

    public final static String keyCloakUrl = "http://auth.mastersofjava.nljug"; // "localhost:8888"; // "auth.mastersofjava.nljug";
    // The secret of the keyCloak client named 'gatling' with 'Client authentication' set to true
    public static final String keyCloakClientSecret = "rrgPbJpTiwX1uxPGUDzSFNwdnBUmZMbI"; // "LshUtyu5XOhyL15fF35hXF7pRtD8k8Tx"; // "rrgPbJpTiwX1uxPGUDzSFNwdnBUmZMbI";
    public static final String keyCloakAdminUsername = "keycloak"; // ""admin"; // "keycloak";
    public static final String keyCloakAdminPassword = "7d597bacdfbc41b8b067e3a5eef009c1"; // "admin"; // "7d597bacdfbc41b8b067e3a5eef009c1";

    public static final String assigmentName = "sudoku solver";
    public static int tabsCount = 8;
    public static final String doesNotCompile = """
            import java.util.List;
            import java.util.ArrayList;

            /**
             * This class helps to solve a sudoku by reducing the possibilities in each region.
             */
            public class RegionReducer {

                /**
                 * @param region A region is one row, column or block in a sudoku. Each cell is a list of possible digits.\s
                 * @return A region with reduced possibilities
                 */
                public List<List<Integer>> reduce(List<List<Integer>> region) {
                	// generate groups of any size
                	for (int size = 1; size<9; size++) {
                		handleGroup( size, 0, new ArrayList<Integer>(), region);
                	}
                    return region
                }

                /**
                 * Tries to recursively reduce groups of the given size, starting from a given start number.
                 * @param the size of groups we're looking for
                 * @param start the lowest number in the group
                 * @param partialGroup the current group we are looking at
                 * @param region the region to reduce\s
                 */
            	private void handleGroup(int size, int start, List<Integer> partialGroup, List<List<Integer>> region) {
            		if (size==partialGroup.size()) {
            			checkNaked(partialGroup, region);
            		} else {
            			for( int i=start+1; i<=9; i++) {
            				List<Integer> group = new ArrayList<>(partialGroup);
            				group.add(i);
            				handleGroup( size, i, group, region);
            			}
            		}
            	}
            	
            	/**
            	 * Checks the given group if it is a naked group in the given region.
            	 * @param group the potential naked single/pair/triple...
            	 * @param region the region to check in
            	 * @return a reduced region
            	 */
            	private void checkNaked(List<Integer> group, List<List<Integer>> region ) {
            		
            		// TODO implement here.

            		// If group size is x, then if we have x cells with only digits in the given group, we have a naked group.
            		// Example: If the group is {3,4,5} and we have 3 cells containing only the digits 3, 4 or 5 (or less), then it is a naked group.\s
            		// Those digits can be removed from all other cells in the region.

            		// Tip: start with focusing on group size 1 to score at least those points!
            	}

            	
            }
                        """;

    public static final String emptyCode = """
            import java.util.List;
            import java.util.ArrayList;

            /**
             * This class helps to solve a sudoku by reducing the possibilities in each region.
             */
            public class RegionReducer {

                /**
                 * @param region A region is one row, column or block in a sudoku. Each cell is a list of possible digits.\s
                 * @return A region with reduced possibilities
                 */
                public List<List<Integer>> reduce(List<List<Integer>> region) {
                	// generate groups of any size
                	for (int size = 1; size<9; size++) {
                		handleGroup( size, 0, new ArrayList<Integer>(), region);
                	}
                    return region;
                }

                /**
                 * Tries to recursively reduce groups of the given size, starting from a given start number.
                 * @param the size of groups we're looking for
                 * @param start the lowest number in the group
                 * @param partialGroup the current group we are looking at
                 * @param region the region to reduce\s
                 */
            	private void handleGroup(int size, int start, List<Integer> partialGroup, List<List<Integer>> region) {
            		if (size==partialGroup.size()) {
            			checkNaked(partialGroup, region);
            		} else {
            			for( int i=start+1; i<=9; i++) {
            				List<Integer> group = new ArrayList<>(partialGroup);
            				group.add(i);
            				handleGroup( size, i, group, region);
            			}
            		}
            	}
            	
            	/**
            	 * Checks the given group if it is a naked group in the given region.
            	 * @param group the potential naked single/pair/triple...
            	 * @param region the region to check in
            	 * @return a reduced region
            	 */
            	private void checkNaked(List<Integer> group, List<List<Integer>> region ) {
            		
            		// TODO implement here.

            		// If group size is x, then if we have x cells with only digits in the given group, we have a naked group.
            		// Example: If the group is {3,4,5} and we have 3 cells containing only the digits 3, 4 or 5 (or less), then it is a naked group.\s
            		// Those digits can be removed from all other cells in the region.

            		// Tip: start with focusing on group size 1 to score at least those points!
            	}

            	
            }
                        """;

    public static final String attempt1 = """
            import java.util.List;
            import java.util.Map;
            import java.util.function.Predicate;
                    
            import static java.util.Arrays.asList;
            import static java.util.stream.Collectors.counting;
            import static java.util.stream.Collectors.groupingBy;
                    
            /**
             * This class helps to solve a sudoku by reducing the possibilities in each region.
             */
            public class RegionReducer {
                    
                /**
                 * @param region A region is one row, column or block in a sudoku
                 * @return A region with reduced possibilities
                 */
                public List<List<Integer>> reduce(List<List<Integer>> region) {
                    List<List<Integer>> singleOccurrences = reduceSingleOccurrences(region);
                    List<List<Integer>> singlesReduced = reduceSingles(singleOccurrences);
                    List<List<Integer>> pairsReduced = reducePairs(singlesReduced);
                    List<List<Integer>> triplesReduced = reduceTriples(pairsReduced);
                    List<List<Integer>> quadruplesReduced = reduceQuadruples(triplesReduced);
                    return quadruplesReduced.equals(singleOccurrences) ? quadruplesReduced : reduce(quadruplesReduced);
                }
                    
                private List<List<Integer>> reduceSingleOccurrences(List<List<Integer>> region) {
                    List<Integer> singleOccurrences = region.stream()
                            .flatMap(cell -> cell.stream())
                            .collect(groupingBy(value -> value, counting()))
                            .entrySet().stream()
                            .filter(entry -> entry.getValue() == 1)
                            .map(Map.Entry::getKey)
                            .toList();
                    return region.stream().map(cell -> keepSingleOccurrences(cell, singleOccurrences)).toList();
                }
                    
                private List<Integer> keepSingleOccurrences(List<Integer> cell, List<Integer> singleOccurrences) {
                    for (Integer singleOccurrence : singleOccurrences) {
                        if (cell.contains(singleOccurrence)) {
                            return asList(singleOccurrence);
                        }
                    }
                    return cell;
                }
                    
                private List<List<Integer>> reduceSingles(List<List<Integer>> region) {
                    List<List<Integer>> singles = region.stream().filter(cell -> cell.size() == 1).toList();
                    return reduceItems(region, singles);
                }
                    
                private List<List<Integer>> reducePairs(List<List<Integer>> region) {
                    List<List<Integer>> pairs = findPairs(region);
                    return reduceItems(region, pairs);
                }
                    
                private List<List<Integer>> reduceTriples(List<List<Integer>> region) {
                    List<List<Integer>> triples = findTriples(region);
                    return reduceItems(region, triples);
                }
                    
                private List<List<Integer>> reduceQuadruples(List<List<Integer>> region) {
                    List<List<Integer>> triples = findQuadruples(region);
                    return reduceItems(region, triples);
                }
                    
                private List<List<Integer>> findPairs(List<List<Integer>> region) {
                    Predicate<List<Integer>> occursTwice = pair -> region.stream().filter(cell -> cell.equals(pair)).count() == 2;
                    return region.stream()
                            .filter(cell -> cell.size() == 2)
                            .filter(occursTwice)
                            .distinct().toList();
                }
                    
                private List<List<Integer>> findTriples(List<List<Integer>> region) {
                    Predicate<List<Integer>> occursThreeTimes = triple -> region.stream().filter(cell -> cell.equals(triple)).count() == 3;
                    return region.stream()
                            .filter(cell -> cell.size() == 3)
                            .filter(occursThreeTimes)
                            .distinct().toList();
                }
                    
                private List<List<Integer>> findQuadruples(List<List<Integer>> region) {
                    Predicate<List<Integer>> occursFourTimes = quadruple -> region.stream().filter(cell -> cell.equals(quadruple)).count() == 4;
                    return region.stream()
                            .filter(cell -> cell.size() == 6)
                            .filter(occursFourTimes)
                            .distinct().toList();
                }
                    
                private List<List<Integer>> reduceItems(List<List<Integer>> region, List<List<Integer>> itemsToReduce) {
                    List<List<Integer>> reducedRegion = region;
                    for (List<Integer> item : itemsToReduce) {
                        reducedRegion = reduceItem(reducedRegion, item);
                    }
                    return reducedRegion;
                }
                    
                private List<List<Integer>> reduceItem(List<List<Integer>> region, List<Integer> itemToReduce) {
                    return region.stream()
                            .map(cell -> reduceCell(cell, itemToReduce))
                            .toList();
                }
                    
                private List<Integer> reduceCell(List<Integer> cell, List<Integer> itemToReduce) {
                    if (cell.equals(itemToReduce)) {
                        return cell;
                    } else {
                        return cell.stream().filter(digit -> !itemToReduce.contains(digit)).toList();
                    }
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
            import java.util.Map;
            import java.util.function.Predicate;
                        
            import static java.util.Arrays.asList;
            import static java.util.stream.Collectors.counting;
            import static java.util.stream.Collectors.groupingBy;
                        
            /**
             * This class helps to solve a sudoku by reducing the possibilities in each region.
             */
            public class RegionReducer {
                        
                /**
                 * @param region A region is one row, column or block in a sudoku
                 * @return A region with reduced possibilities
                 */
                public List<List<Integer>> reduce(List<List<Integer>> region) {
                    List<List<Integer>> singleOccurrences = reduceSingleOccurrences(region);
                    List<List<Integer>> singlesReduced = reduceSingles(singleOccurrences);
                    List<List<Integer>> pairsReduced = reducePairs(singlesReduced);
                    List<List<Integer>> triplesReduced = reduceTriples(pairsReduced);
                    List<List<Integer>> quadruplesReduced = reduceQuadruples(triplesReduced);
                    return quadruplesReduced.equals(singleOccurrences) ? quadruplesReduced : reduce(quadruplesReduced);
                }
                        
                private List<List<Integer>> reduceSingleOccurrences(List<List<Integer>> region) {
                    List<Integer> singleOccurrences = region.stream()
                            .flatMap(cell -> cell.stream())
                            .collect(groupingBy(value -> value, counting()))
                            .entrySet().stream()
                            .filter(entry -> entry.getValue() == 1)
                            .map(Map.Entry::getKey)
                            .toList();
                    return region.stream().map(cell -> keepSingleOccurrences(cell, singleOccurrences)).toList();
                }
                        
                private List<Integer> keepSingleOccurrences(List<Integer> cell, List<Integer> singleOccurrences) {
                    for (Integer singleOccurrence : singleOccurrences) {
                        if (cell.contains(singleOccurrence)) {
                            return asList(singleOccurrence);
                        }
                    }
                    return cell;
                }
                        
                private List<List<Integer>> reduceSingles(List<List<Integer>> region) {
                    List<List<Integer>> singles = region.stream().filter(cell -> cell.size() == 1).toList();
                    return reduceItems(region, singles);
                }
                        
                private List<List<Integer>> reducePairs(List<List<Integer>> region) {
                    List<List<Integer>> pairs = findPairs(region);
                    return reduceItems(region, pairs);
                }
                        
                private List<List<Integer>> reduceTriples(List<List<Integer>> region) {
                    List<List<Integer>> triples = findTriples(region);
                    return reduceItems(region, triples);
                }
                        
                private List<List<Integer>> reduceQuadruples(List<List<Integer>> region) {
                    List<List<Integer>> triples = findQuadruples(region);
                    return reduceItems(region, triples);
                }
                        
                private List<List<Integer>> findPairs(List<List<Integer>> region) {
                    Predicate<List<Integer>> occursTwice = pair -> region.stream().filter(cell -> cell.equals(pair)).count() == 2;
                    return region.stream()
                            .filter(cell -> cell.size() == 2)
                            .filter(occursTwice)
                            .distinct().toList();
                }
                        
                private List<List<Integer>> findTriples(List<List<Integer>> region) {
                    Predicate<List<Integer>> occursThreeTimes = triple -> region.stream().filter(cell -> cell.equals(triple)).count() == 3;
                    return region.stream()
                            .filter(cell -> cell.size() == 3)
                            .filter(occursThreeTimes)
                            .distinct().toList();
                }
                        
                private List<List<Integer>> findQuadruples(List<List<Integer>> region) {
                    Predicate<List<Integer>> occursFourTimes = quadruple -> region.stream().filter(cell -> cell.equals(quadruple)).count() == 4;
                    return region.stream()
                            .filter(cell -> cell.size() == 4)
                            .filter(occursFourTimes)
                            .distinct().toList();
                }
                        
                private List<List<Integer>> reduceItems(List<List<Integer>> region, List<List<Integer>> itemsToReduce) {
                    List<List<Integer>> reducedRegion = region;
                    for (List<Integer> item : itemsToReduce) {
                        reducedRegion = reduceItem(reducedRegion, item);
                    }
                    return reducedRegion;
                }
                        
                private List<List<Integer>> reduceItem(List<List<Integer>> region, List<Integer> itemToReduce) {
                    return region.stream()
                            .map(cell -> reduceCell(cell, itemToReduce))
                            .toList();
                }
                        
                private List<Integer> reduceCell(List<Integer> cell, List<Integer> itemToReduce) {
                    if (cell.equals(itemToReduce)) {
                        return cell;
                    } else {
                        return cell.stream().filter(digit -> !itemToReduce.contains(digit)).toList();
                    }
                }
            }
                        
                        """;

}
