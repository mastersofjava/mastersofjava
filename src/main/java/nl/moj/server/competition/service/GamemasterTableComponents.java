package nl.moj.server.competition.service;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.RequiredArgsConstructor;
import nl.moj.server.GamemasterController;
import nl.moj.server.assignment.descriptor.AssignmentDescriptor;
import nl.moj.server.competition.model.OrderedAssignment;
import nl.moj.server.runtime.CompetitionRuntime;
import nl.moj.server.teams.model.Team;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GamemasterTableComponents {
    private final CompetitionRuntime competition;
    @JsonSerialize
    public static class DtoAssignmentState implements Serializable {
        long order;
        String name;
        String state;

        public long getOrder() {
            return order;
        }

        public String getName() {
            return name;
        }

        public String getState() {
            return state;
        }
    }

    public List<DtoAssignmentState> createAssignmentStatusMap() {
        List<DtoAssignmentState> result = new ArrayList<>();
        List<OrderedAssignment> orderedList = competition.getCompetition().getAssignmentsInOrder();

        if (orderedList.isEmpty()) {
            return result;
        }
        List<OrderedAssignment> completedList = competition.getCompetitionState().getCompletedAssignments();
        for (OrderedAssignment orderedAssignment: orderedList) {
            boolean isCompleted = false;
            boolean isCurrent = orderedAssignment.equals(competition.getCurrentAssignment());

            for (OrderedAssignment completedAssignment: completedList) {
                if (completedAssignment.getAssignment().getName().equals(orderedAssignment.getAssignment().getName())) {
                    isCompleted = true;
                }
            }

            String status = "";
            if (isCompleted) {
                status += "COMPLETED"; // NB: started assignments are completed by default, this seems like a bug.
            }
            if (isCurrent &&  competition.getActiveAssignment().getTimeRemaining()>0) {
                status = "CURRENT"; // NB: if current, then not completed and with time remaining!
            }
            if (status.isEmpty()) {
                status = "-";
            }
            DtoAssignmentState state = new DtoAssignmentState();
            state.name = orderedAssignment.getAssignment().getName();
            state.state = status;
            state.order = orderedAssignment.getOrder();
            result.add(state);
        }
        return result;
    }
    public String toSimpleBootstrapTableForTeams(List<Team> teams) {
        StringBuilder sb = new StringBuilder();
        sb.append("<br/><table class='roundGrayBorder table' ><thead><tr><th>Nr</th><th>Teamnaam</th><th>Score</th></tr></thead>");

        int counter = 1;
        for (Team team: teams) {
            sb.append("<tr><td>"+counter+"</td><td>"+team.getName()+"</td><td>0</td></tr>");
            counter ++;
        }
        sb.append("</table>");
        return sb.toString();
    }
    public String toSimpleBootstrapTableForAssignmentStatus() {
        StringBuilder sb = new StringBuilder();
        List<DtoAssignmentState> list = createAssignmentStatusMap();
        if (list.isEmpty()) {
            return "";
        }
        sb.append("<br/><table class='roundGrayBorder table' ><thead><tr><th>Nr</th><th>Opdracht</th><th>Status</th><th>High score</th></tr></thead>");


        for (DtoAssignmentState orderedAssignment: list) {
            sb.append("<tr><td>"+orderedAssignment.order+"</td><td>"+orderedAssignment.name+"</td><td>"+orderedAssignment.state+"</td><td>0</td></tr>");
        }
        sb.append("</table>");
        return sb.toString();
    }

    public String toSimpleBootstrapTable(List<AssignmentDescriptor> assignmentDescriptorList) {
        StringBuilder sb = new StringBuilder();
        sb.append("<br/><table class='roundGrayBorder table' ><thead><tr><th>Opdracht</th><th>Auteur</th><th>Java versie</th><th>Complexiteit</th></tr></thead>");
        for (AssignmentDescriptor descriptor: assignmentDescriptorList) {


            sb.append("<tr><td>"+descriptor.getName()+"</td><td>"+descriptor.getAuthor().getName().split("\\(")[0]+"</td><td>"+descriptor.getJavaVersion()+"</td><td>"+descriptor.getDifficulty() + "</td></tr>");
        }
        sb.append("</table>");
        return sb.toString();
    }
}
