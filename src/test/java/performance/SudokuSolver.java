package performance;

public class SudokuSolver implements TestAssignment {

    private final String[] attempts = {
            """
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
            """
    };

    @Override
    public String getAssignmentName() {
        return "sudoku solver";
    }

    @Override
    public int getTabCount() {
        return 8;
    }

    @Override
    public String getDoesNotCompile() {
        return """
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
    }

    @Override
    public String getEmpty() {
        return """
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
    }

    @Override
    public String getSolution() {
        return """
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

    @Override
    public int getAttempts() {
        return attempts.length;
    }

    @Override
    public String getAttempt(int number) {
        return attempts[number];
    }
}
