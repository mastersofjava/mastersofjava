package nl.moj.server.compile;

import static java.util.Objects.requireNonNull;

public final class AssignmentDTO {

    private final String teamName;
    private final String assignment;

    public AssignmentDTO(final String teamName, final String assignment) {
        this.teamName = requireNonNull(teamName);
        this.assignment = requireNonNull(assignment);
    }

    public static AssignmentDTO of(final String teamName, final String assignment) {
        return new AssignmentDTO(teamName, assignment);
    }

    public String getTeamName() {
        return teamName;
    }

    public String getAssignment() {
        return assignment;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AssignmentDTO that = (AssignmentDTO) o;

        if (teamName != null ? !teamName.equals(that.teamName) : that.teamName != null) return false;
        return assignment != null ? assignment.equals(that.assignment) : that.assignment == null;
    }

    @Override
    public int hashCode() {
        int result = teamName != null ? teamName.hashCode() : 0;
        result = 31 * result + (assignment != null ? assignment.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "AssignmentDTO{" +
                "teamName='" + teamName + '\'' +
                ", assignment='" + assignment + '\'' +
                '}';
    }
}
