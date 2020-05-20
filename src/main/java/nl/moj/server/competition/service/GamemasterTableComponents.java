package nl.moj.server.competition.service;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.RequiredArgsConstructor;
import nl.moj.server.assignment.descriptor.AssignmentDescriptor;
import nl.moj.server.assignment.descriptor.AssignmentFiles;
import nl.moj.server.competition.model.OrderedAssignment;
import nl.moj.server.runtime.CompetitionRuntime;
import nl.moj.server.teams.model.Team;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.Serializable;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * the html creation will be moved towards the angular client in the future.
 */
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

        int counter = 0;
        for (DtoAssignmentState orderedAssignment: list) {
            boolean isStateCurrent = orderedAssignment.state.contains("CURRENT");
            String viewState = orderedAssignment.state;
            if (isStateCurrent) {
                viewState = "<a href='./'>"+viewState+"</a>";
            }
            String viewName = "<a href='./assignmentAdmin?assignment="+orderedAssignment.name+"' title='view assignment'>"+orderedAssignment.name+"</a>";
            String viewOrder = "<a href='./assignmentAdmin?assignment="+orderedAssignment.name+"&solution' title='view solution'>"+counter+"</a>";

            sb.append("<tr><td>"+viewOrder+"</td><td>"+viewName+"</td><td>"+viewState + "</td><td>0</td></tr>");
            counter++;
        }
        sb.append("</table>");
        return sb.toString();
    }

    private class SimpleAssignmentDetailsPanel {
        File directory;
        public String createTables(List<AssignmentDescriptor> assignmentDescriptorList) {
            StringBuilder sb = new StringBuilder();

            for (AssignmentDescriptor descriptor: assignmentDescriptorList) {
                directory = descriptor.getDirectory().toFile();
                sb.append("<table class='roundGrayBorder table' ><thead><tr><th>Files - Assignment '"+directory.getName()+"' </th><th>Type</th><th>Size</th><th>Last Modified</th></tr></thead>");
                AssignmentFiles wrapper = descriptor.getAssignmentFiles();

                sb.append(toSimpleFileRow(wrapper.getAssignment(), "assignment"));
                sb.append(toSimpleFileRow(wrapper.getSecurityPolicy(), "security policy"));

                for (Path solution: wrapper.getSolution()) {
                    sb.append(toSimpleFileRow(solution, "solution"));
                }
                for (Path solution: wrapper.getSources().getEditable()) {
                    sb.append(toSimpleFileRow(solution, "src.editable"));
                }
                for (Path solution: wrapper.getSources().getReadonly()) {
                    sb.append(toSimpleFileRow(solution, "src.readable"));
                }
                for (Path solution: wrapper.getTestSources().getHiddenTests()) {
                    sb.append(toSimpleFileRow(solution, "src.test.hidden"));
                }
                for (Path solution: wrapper.getTestSources().getTests()) {
                    sb.append(toSimpleFileRow(solution, "src.test.visible"));
                }
                sb.append("</table>");
            }

            return sb.toString();
        }
        private final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        private String toSimpleFileRow(Path file, String type) {
            if (file == null) {
                return "";
            }
            return toSimpleFileRow(file.toFile(), type);
        }
        private String toSimpleFileRow(File file, String type) {
            if (file==null) {
                return "";
            }
            if (!file.exists() && !type.startsWith("src.")) {
                file = new File(directory, file.getPath() );
            } else
            if (!file.exists()) {
                String prefix = "/src/main/java/";
                if (type.startsWith("src.test")) {
                    prefix = "/src/test/java/";
                }
                file = new File(directory, prefix+file.getPath() );
            }
            StringBuilder sb = new StringBuilder();
            sb.append("<tr><td>"+file+"</td><td>"+type+"</td><td>"+file.length()+"</td><td>"+SDF.format(new Date(file.lastModified()))+"</td></tr>");
            return sb.toString();
        }
    }

    public String toSimpleBootstrapTablesForFileDetails(List<AssignmentDescriptor> assignmentDescriptorList) {
        return new SimpleAssignmentDetailsPanel().createTables(assignmentDescriptorList);
    }

    public String toSimpleBootstrapTable(List<AssignmentDescriptor> assignmentDescriptorList) {
        StringBuilder sb = new StringBuilder();
        String tokenForIndividualBonus = "(*1)";
        sb.append("<br/><table class='roundGrayBorder table' ><thead><tr><th>Opdracht</th><th>Auteur</th><th>Bonus</th><th>Minuten</th><th>Java</th><th>Complexiteit</th></tr></thead>");
        for (AssignmentDescriptor descriptor: assignmentDescriptorList) {
            String bonus = "";
            String title = ""+descriptor.getLabels()+ " - " +descriptor.getScoringRules().toString();
            boolean isWithIndividualTestBonus = title.contains("[test");
            if (isWithIndividualTestBonus) {
                bonus = tokenForIndividualBonus;
            }
            bonus = descriptor.getScoringRules().getSuccessBonus() + bonus;
            String author = descriptor.getAuthor().getName().split("\\(")[0];
            if (author.contains("http")) {
                author = author.split("/")[0];
            }
            if (author.contains(" based on ")) {
                author = author.substring(0, author.indexOf(" based on "));
            }
            Long duration = descriptor.getDuration().toMinutes();
            title = " \"" +descriptor.getDisplayName()+ "\"";
            sb.append("<tr title='"+title+"'><td>"+descriptor.getName()+"</td><td>"+author+"</td><td>"+bonus+"</td><td>"+duration+"</td><td>"+descriptor.getJavaVersion()+"</td><td>"+descriptor.getDifficulty() + "</td></tr>");
        }
        sb.append("</table>");
        return sb.toString();
    }
}
