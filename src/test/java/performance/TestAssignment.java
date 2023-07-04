package performance;

public interface TestAssignment {

    String getAssignmentName();

    int getTabCount();

    String getDoesNotCompile();

    String getEmpty();

    String getSolution();

    int getAttempts();

    String getAttempt(int number);
}
