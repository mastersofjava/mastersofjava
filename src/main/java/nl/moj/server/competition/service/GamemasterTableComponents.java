package nl.moj.server.competition.service;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.moj.server.assignment.descriptor.AssignmentDescriptor;
import nl.moj.server.assignment.descriptor.AssignmentFiles;
import nl.moj.server.competition.model.Competition;
import nl.moj.server.competition.model.CompetitionSession;
import nl.moj.server.competition.model.OrderedAssignment;
import nl.moj.server.competition.repository.CompetitionRepository;
import nl.moj.server.competition.repository.CompetitionSessionRepository;
import nl.moj.server.config.properties.MojServerProperties;
import nl.moj.server.runtime.CompetitionRuntime;
import nl.moj.server.teams.model.Role;
import nl.moj.server.teams.model.Team;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * the html creation will be moved towards the angular client in the future.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GamemasterTableComponents {
    private final CompetitionRuntime competition;

    private final MojServerProperties mojServerProperties;

    private final CompetitionRepository competitionRepository;

    private final CompetitionSessionRepository competitionSessionRepository;

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
    public void deleteCurrentSessionResources() {
        String uuid = competition.getCompetitionSession().getUuid().toString();
        File rootFile = new File(mojServerProperties.getDirectories().getBaseDirectory().toFile(), "sessions\\"+uuid+"\\teams");
        try {
            File directory = mojServerProperties.getDirectories().getBaseDirectory().toFile();

            Collection<File> fileList = FileUtils.listFiles(directory, new String[] {"java","class"}, true);

            for (File file: fileList) {
                if (file.exists()) {
                    File project = file.getParentFile().getParentFile();
                    FileUtils.deleteQuietly(project);
                }
            }
            log.info("deleteCurrentSessionResources.before " + rootFile + " "+rootFile.exists() );
            FileUtils.deleteQuietly(rootFile);
            log.info("deleteCurrentSessionResources.after " + rootFile + " "+rootFile.exists() );
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public String toSimpleBootstrapTableForSessions() {
        StringBuilder sb = new StringBuilder();
        String selectedSession = competition.getCompetitionSession().getUuid().toString();
        String selectedCompetition = competition.getCompetition().getName();
        int competitionCounter = 1;
        List<Competition> competitionList = competitionRepository.findAll();
        boolean isWithDeletionButton = competitionList.size()>1;

        for (Competition competition:competitionList) {
            List<CompetitionSession> sessionList = competitionSessionRepository.findByCompetition(competition);
            String[] parts = competition.getName().split("\\|");
            String name = parts[0];
            String collection = mojServerProperties.getAssignmentRepo().toFile().getName();
            if (parts.length>=2) {
                collection = parts[1];
            }
            String actionsAdd = "<button class='btn btn-secondary' data-toggle='modal' onclick=\"$('#createNewSessionForm').submit()\">toevoegen sessie</button>";
            String actionsDelete = "<button class='btn btn-secondary' data-toggle='modal' data-target='#deleteCompetition-modal'  onclick='clientSelectSubtable(this);return false'>verwijder</button>";
            String styleCompetition = competition.getName().equals(selectedCompetition) ? " italic " :"";
            sb.append("<tbody title='sessiepanel van competitie "+competition.getId()+"' ><tr class='"+ styleCompetition+" tableSubHeader' id='"+competition.getUuid()+"'><td><button class='btn btn-secondary' onclick='clientSelectSubtable(this)'><span class='fa fa-angle-double-right pr-1'>&nbsp;&nbsp;"+competitionCounter+"</span></button></td><td contentEditable=true spellcheck=false onfocusout=\"doCompetitionSaveName(this.innerHTML, this.parentNode.id)\" >"+name+"</td><td>"+collection+"</td><td >"+actionsAdd+"</td><td>"+actionsDelete+"</td></tr>");

            int sessionCounter = 1;

            for (CompetitionSession session: sessionList) {
                String styleCompetitionSession = selectedSession.equals(session.getUuid().toString())?" bold ":"";
                String status = "-";
                if (sessionCounter==sessionList.size()) {
                    status = "<button class='btn btn-secondary' data-toggle='modal' onclick=\"confirm('start sessie')\">start</button>";
                }
                sb.append("<tr class='"+styleCompetitionSession+" subrows hide'><td colspan=2></td><td>- Sessie "+sessionCounter+"</td><td>"+status+"</td><td></td></tr>");
                sessionCounter ++;
            }
            sb.append("</tbody>");

            competitionCounter++;
        }
        sb.insert(0, "<table class='roundGrayBorder table sessionTable' ><thead><tr><th></th><th>Competities</th><th></th><th>Aantal sessies</th><th>Acties</th></tr></thead>");
        sb.append("<tfoot>");
        sb.append("<tr><th colspan=4></th><th><button class='btn btn-secondary' data-toggle='modal' data-target='#createNewCompetition-modal'>Nieuwe competitie</button></th></tr>");
        sb.append("</tfoot>");
        sb.append("</table>");
        return sb.toString();
    }


    public String toSimpleBootstrapTableForTeams(List<Team> teams, boolean isShowAllUsers) {
        StringBuilder sb = new StringBuilder();
        String uuid = competition.getCompetitionSession().getUuid().toString();

        File rootFile = new File(mojServerProperties.getDirectories().getBaseDirectory().toFile(), "sessions\\"+uuid+"\\teams");
        List<String> idList = new ArrayList<>();
        if (rootFile.exists()) {
            File[] usersInSession = rootFile.listFiles();
            for (File file: usersInSession) {
                idList.add(file.getName());
            }
        }
        List<Team> displayList = new ArrayList<>();
        for (Team team: teams) {
            if (isShowAllUsers || idList.contains(team.getUuid().toString())) {
                displayList.add(team);
            }
        }
        int counter = 1;
        String displayTable = displayList.size()>10 ?" scrollableTable ":"";
        sb.append("<table class='roundGrayBorder table "+displayTable+"' ><thead><tr><th>Nr</th><th>Team</th><th>Games</th><th>Score</th></tr></thead>");

        for (Team team: displayList) {
            List<String> titleList = new ArrayList<>();
            if (idList.contains(team.getUuid().toString())) {
                File[] games = new File(rootFile, team.getUuid().toString()).listFiles();
                for (File file: games) {
                    titleList.add(file.getName());
                }
            }
            boolean isArchived = team.getCompany().equals("ARCHIVE") || team.getCompany().equals("DISQUALIFY");
            if (isArchived) {
                continue;
            }

            String specialRole = "";
            if (!team.getRole().equals(Role.USER)) {
                specialRole = "("+team.getRole().split("_")[1]+")";
            }
            String style = isShowAllUsers?"cursorPointer":"";
            String simpleClickEvent = isShowAllUsers?"clientSelectSubtable(this, '"+team.getUuid()+"')":"";
            String viewTitle = titleList.isEmpty()?"deze gebruiker heeft nog geen opdrachten gedaan":titleList.toString();
            sb.append("<tbody id='"+team.getUuid()+"'><tr class='"+style+"' onclick=\""+simpleClickEvent+"\"><td class='alignRight'>"+counter+"</td><td class='minWidth100'>"+team.getName()+specialRole+"</td><td title='"+viewTitle+"' class='alignRight'>"+titleList.size()+"</td><td class='alignRight minWidth100'>0</td></tr>");

            if (isShowAllUsers) {
                sb.append("<tr class='subrows hide font10px'><td colspan=2>");
                String inputField = "<i>registreer contactgegevens:</i><br/><input type='text' placeholder='registreer contactgegevens' onchange=\"updateTeamStatus($(this).closest('tbody').attr('id'),this.value)\" value='"+team.getCompany()+"' class='minWidth200 font10px'/>";
                String deleteButton = "<button class='font10px cursorPointer smallBlackBorder minWidth100' onclick=\"updateTeamStatus($(this).closest('tbody').attr('id'),'ARCHIVE');$(this).closest('tbody').addClass('hide')\">archive team</button><br/>";
                String disqualifyButton = "<button class='font10px cursorPointer smallBlackBorder minWidth100' onclick=\"updateTeamStatus($(this).closest('tbody').attr('id'),'DISQUALIFY');$(this).closest('tbody').addClass('hide')\">disqualify</button><br/>";
                if (team.getRole().equals(Role.ADMIN)) {
                    deleteButton = "";
                    inputField = "";
                    disqualifyButton = "";
                }
                sb.append(inputField);
                sb.append("</td><td colspan=2 class='alignCenter'>"+deleteButton+disqualifyButton+"<button onclick=\"$('.passwordModal').toggleClass('hide')\" class='font10px minWidth100 smallBlackBorder cursorPointer nowrap'>password update</button></td></tr>");
            }
            sb.append("</tbody>");
            counter ++;
        }
        if (counter==1) {
            sb.append("<tr><td colspan=4>");
            sb.append("De competitie is nog niet gestart.");
            sb.append("</td></tr>");
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

        int counter = 1;
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
        private String assignmentName;
        private File directory;
        private List<File> fileList = new ArrayList<>();
        private List<String> errorList = new ArrayList<>();
        private String localPath = mojServerProperties.getAssignmentRepo().toFile().getParentFile().getPath();
        public String createTableList(List<AssignmentDescriptor> assignmentDescriptorList) {
            StringBuilder sbAll = new StringBuilder();
            sbAll.append("<span class='top'>");
            for (AssignmentDescriptor descriptor: assignmentDescriptorList) {
                AssignmentFiles wrapper = descriptor.getAssignmentFiles();
                initialize(descriptor);
                sbAll.append("<span title='"+readYamlText(descriptor)+"'>");
                sbAll.append(createTable(wrapper));
                sbAll.append("</span>");
            }
            sbAll.append("</span>");
            return sbAll.toString();
        }
        private String readYamlText(AssignmentDescriptor descriptor) {
            String text = "";
            try {
                text = Files.readString(new File(descriptor.getDirectory().toFile(),"assignment.yaml").toPath()).replace("'","\"");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return text;
        }
        private void initialize(AssignmentDescriptor descriptor) {
            this.directory = descriptor.getDirectory().toFile();
            assignmentName = descriptor.getName();
            fileList = new ArrayList<>();
            errorList = new ArrayList<>();
        }
        private String createTable(AssignmentFiles wrapper) {
            StringBuilder sb = new StringBuilder();
            if (wrapper.getSolution().isEmpty()) {
                errorList.add("solution file missing");
            }
            if (wrapper.getSources().getEditable().isEmpty()) {
                errorList.add("editable file missing");
            }
            if (wrapper.getTestSources().getHiddenTests().isEmpty() && new File(directory, "src/test/java/HiddenTests.java").exists()) {
                errorList.add("hidden test found which is not configured correctly in yaml");
            }
            sb.append(toSimpleFileRow(wrapper.getAssignment(), "assignment"));
            sb.append(toSimpleFileRow(wrapper.getSecurityPolicy(), "security policy"));

            for (Path file: wrapper.getSolution()) {
                sb.append(toSimpleFileRow(file, "solution"));
            }
            for (Path file: wrapper.getSources().getEditable()) {
                sb.append(toSimpleFileRow(file, "src.editable"));
            }
            for (Path file: wrapper.getSources().getReadonly()) {
                sb.append(toSimpleFileRow(file, "src.readable"));
            }
            for (Path file: wrapper.getTestSources().getHiddenTests()) {
                sb.append(toSimpleFileRow(file, "src.test.hidden"));
            }
            for (Path file: wrapper.getTestSources().getTests()) {
                sb.append(toSimpleFileRow(file, "src.test.visible"));
            }
            StringBuffer header = new StringBuffer();
            header.append("<table class='roundGrayBorder table noBottomMargin' ><thead class='cursorPointer' onclick=\"$(this).closest('.top').find('.extra').addClass('hide');$(this).parent().find('.extra').toggleClass('hide')\"><tr><th class='minWidth300'>Files - Assignment '");
            header.append(assignmentName+"' - "+fileList.size()+" files</th><th class='extra hide'>Type</th><th class='extra hide'>Size</th><th class='extra hide'>Last Modified</th></tr>");
            if (errorList.size()>0) {
                header.append("<tr><td colspan=4 class='error'><center>ERROR</center></td></tr>");
                for (String error: errorList) {
                    header.append("<tr><td colspan=4 class='error'>- "+error+"</td></tr>");
                }
            }


            sb.insert(0, header.toString() +"</thead><tbody class='extra hide'>");
            sb.append("</tbody></table>");
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
            if (!file.exists()) {
                errorList.add("file bestaat niet: " + file);
            }
            if (file.getName().contains("Hidden") && !type.contains("hidden")) {
                errorList.add("file '"+file.getName()+"' is bedoeld als 'hidden', echter heeft het verkeerde type: " + type);
            }
            fileList.add(file);

            String viewType = type;
            if ("solution".equals(type)) {
                viewType = "<a class='bold' href='/assignmentAdmin?assignment="+assignmentName+"&solution' title='view solution'>" + type + "</a>";
            } else
            if ("src.editable".equals(type)) {
                viewType = "<a class='bold' href='/assignmentAdmin?assignment="+assignmentName+"' title='view assignment'>" + type + "</a>";
            }
            String viewFile = file.getPath().substring(localPath.length()).replace("\\","/");

            StringBuilder sb = new StringBuilder();
            sb.append("<tr><td class='minWidth300'>"+viewFile+"</td><td>"+viewType+"</td><td class='alignRight'>"+file.length()+"</td><td>"+SDF.format(new Date(file.lastModified()))+"</td></tr>");
            return sb.toString();
        }
    }

    public String toSimpleBootstrapTablesForFileDetails(List<AssignmentDescriptor> assignmentDescriptorList) {
        return new SimpleAssignmentDetailsPanel().createTableList(assignmentDescriptorList);
    }

    public String toSimpleBootstrapTable(List<AssignmentDescriptor> assignmentDescriptorList) {
        StringBuilder sb = new StringBuilder();
        String tokenForIndividualBonus = "<sup>(*1)</sup>";
        sb.append("<br/><table class='roundGrayBorder table' ><thead><tr><th>Opdracht</th><th>Auteur</th><th>Bonus</th><th>Tijd</th><th>Java</th><th>Level</th><th>Labels</th></tr></thead>");
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
            if (author.contains(" and ")) {
                author = author.replace(" and "," & ");
            }
            if (author.contains(" van ")) {
                author = author.replace(" van "," v. ");
            }
            long duration = descriptor.getDuration().toMinutes();

            String selectionBox = toSelectionBox(descriptor.getLabels());
            String postfix = "";
            boolean isNotReady = descriptor.getLabels().contains("not-ready")||descriptor.getLabels().contains("label1")||descriptor.getLabels().contains("label2");
            boolean isNotForCompetition = descriptor.getLabels().contains("internet-searchable");
            title = descriptor.getDisplayName();
            if (isNotReady) {
                postfix = "(NOT READY) ";
            } else
            if (isNotForCompetition) {
                postfix = "(NOT FOR ONLINE) ";
            }
            title += descriptor.getDisplayName() + " - VIEW LABELS FOR MORE DETAILS";
            sb.append("<tr title='"+title+"'><td>"+descriptor.getName()+" <i>"+postfix+"</i></td><td>"+author+"</td><td>"+bonus+"</td><td>"+duration+"</td><td>"+descriptor.getJavaVersion()+"</td><td>"+descriptor.getDifficulty() + "</td><td>"+selectionBox+"</td></tr>");
        }
        sb.append("</table>");
        return sb.toString();
    }

    private String toSelectionBox(List<String> list) {
        StringBuilder sb = new StringBuilder();
        sb.append("<select>");
        for (String item: list) {
            sb.append("<option>"+item+"</option>");
        }
        sb.append("</select>");
        return sb.toString();
    }
}
