package nl.moj.server.competition.service;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;
import lombok.Data;
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
import nl.moj.server.rankings.model.Ranking;
import nl.moj.server.runtime.CompetitionRuntime;
import nl.moj.server.runtime.repository.AssignmentStatusRepository;
import nl.moj.server.teams.model.Role;
import nl.moj.server.teams.model.Team;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    private final AssignmentStatusRepository assignmentStatusRepository;
    @JsonSerialize
    public static class DtoAssignmentState implements Serializable {
        private long order;
        private String name;
        private String state;

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
        List<OrderedAssignment> orderedList = competition.getCompetition().getAssignmentsInOrder();
        if (orderedList.isEmpty()) {
            return Collections.emptyList();
        }
        List<DtoAssignmentState> result = new ArrayList<>();

        for (OrderedAssignment orderedAssignment: orderedList) {
            DtoAssignmentState state = new DtoAssignmentState();
            state.name = orderedAssignment.getAssignment().getName();
            state.state = toViewStatus(orderedAssignment);
            state.order = orderedAssignment.getOrder();
            result.add(state);
        }
        return result;
    }
    private String toViewStatus(OrderedAssignment orderedAssignment) {
        boolean isCompleted = false;
        boolean isSelectedAndNotCompleted = orderedAssignment.equals(competition.getCurrentAssignment())
                &&  competition.getActiveAssignment().getTimeRemaining()>0;
        List<OrderedAssignment> completedList = competition.getCompetitionState().getCompletedAssignments();
        for (OrderedAssignment completedAssignment: completedList) {
            if (completedAssignment.getAssignment().getName().equals(orderedAssignment.getAssignment().getName())) {
                isCompleted = true;
            }
        }
        String status = "-";
        if (isSelectedAndNotCompleted ) {
            status = "CURRENT";
        } else
        if (isCompleted) {
            status = "COMPLETED";
        }
        return status;
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
            log.info("deleteCurrentSessionResources.before {}, {}", rootFile , rootFile.exists() );
            FileUtils.deleteQuietly(rootFile);
            log.info("deleteCurrentSessionResources.after {}, {}", rootFile , rootFile.exists() );
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }
    }
    public String toSimpleBootstrapTableForSessions() {
        return new BootstrapTableForSessions().createHtml();
    }
    public class BootstrapTableForSessions {
        String selectedSession = competition.getCompetitionSession().getUuid().toString();
        String selectedCompetition = competition.getCompetition().getName();
        int competitionCounter = 1;
        List<Competition> competitionList = competitionRepository.findAll();
        public String createHtmlFooterRow() {
            StringBuilder html = new StringBuilder();
            html.append("<tr><th colspan=4></th><th><button class='btn btn-secondary' data-toggle='modal' data-target='#createNewCompetition-modal'>Nieuwe competitie</button></th></tr>");
            return html.toString();
        }
        public String createHtmlHeaderRow() {
            StringBuilder html = new StringBuilder();
            html.append("<tr><th></th><th>Competities</th><th></th><th>Aantal sessies</th><th>Acties</th></tr>");
            return html.toString();
        }
        public String createHtmlTableBody() {
            StringBuilder html = new StringBuilder();
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
                html.append("<tbody title='sessiepanel van competitie "+competition.getId()+"' ><tr class='"+ styleCompetition+" tableSubHeader' id='"+competition.getUuid()+"'><td><button class='btn btn-secondary' onclick='clientSelectSubtable(this)'><span class='fa fa-angle-double-right pr-1'>&nbsp;&nbsp;"+competitionCounter+"</span></button></td><td contentEditable=true spellcheck=false onfocusout=\"doCompetitionSaveName(this.innerHTML, this.parentNode.id)\" >"+name+"</td><td>"+collection+"</td><td >"+actionsAdd+"</td><td>"+actionsDelete+"</td></tr>");

                int sessionCounter = 1;

                for (CompetitionSession session: sessionList) {
                    String styleCompetitionSession = selectedSession.equals(session.getUuid().toString())?" bold ":"";
                    String status = "-";
                    if (sessionCounter==sessionList.size()) {
                        status = "<button class='btn btn-secondary' data-toggle='modal' onclick=\"confirm('start sessie')\">start</button>";
                    }
                    html.append("<tr class='"+styleCompetitionSession+" subrows hide'><td colspan=2></td><td>- Sessie "+sessionCounter+"</td><td>"+status+"</td><td></td></tr>");
                    sessionCounter ++;
                }
                html.append("</tbody>");

                competitionCounter++;
            }
            return html.toString();
        }
        public String createHtml() {
            StringBuilder html = new StringBuilder();
            html.append("<table class='roundGrayBorder table sessionTable' ><thead>"+createHtmlHeaderRow()+"</thead>"+createHtmlTableBody());
            html.append("<tfoot>" +createHtmlFooterRow());
            html.append("</tfoot>");
            html.append("</table>");
            return html.toString();
        }
    }
    public class BootstrapTableForTeams {
        private List<String> orderedList = new ArrayList<>();
        private Map<String,Long> rankingMap = new LinkedHashMap<>();
        private Map<String,Team> teamStore = new LinkedHashMap<>();
        private List<String> usersWithWorkspaceList = new ArrayList<>();
        private File rootFile;
        private boolean isShowAllUsers;
        private int counter;

        private void initializeOrderedListOfTeamReferences(List<Team> teams, List<Ranking> rankings) {
            // provide 10 best scoring teams first (when enough ranking).
            for (Ranking ranking: rankings) {
                rankingMap.put(ranking.getTeam(), ranking.getTotalScore());
                if (orderedList.size()<10) {
                    orderedList.add(ranking.getTeam());
                }
            }
            for (Team team: teams) {
                teamStore.put(team.getName(), team);
            }
            if (isShowAllUsers) {
                // also show all other users when required
                for (Team team: teams) {
                    if (!orderedList.contains(team.getName())) {
                        orderedList.add(team.getName());
                    }
                }
            }
        }
        private void registerTeamWorkspaces() {
            String uuid = competition.getCompetitionSession().getUuid().toString();

            rootFile = new File(mojServerProperties.getDirectories().getBaseDirectory().toFile(), "sessions\\"+uuid+"\\teams");
            if (rootFile.exists()) {
                File[] usersInSession = rootFile.listFiles();
                for (File file: usersInSession) {
                    usersWithWorkspaceList.add(file.getName());
                }
            }
        }

        public BootstrapTableForTeams(List<Team> teams, boolean isShowAllUsers, List<Ranking> rankings) {
            this.isShowAllUsers = isShowAllUsers;
            registerTeamWorkspaces();
            initializeOrderedListOfTeamReferences(teams, rankings);
        }


        private class TableComponentForTeam {
            private List<String> personalWorkspaceList = new ArrayList<>();
            private String specialRoleIndication = "";
            private String rowClass = isShowAllUsers?"cursorPointer":"";
            private Team team;
            private Long totalScore;
            private void ensureUserRoleIndication() {
                if (!team.getRole().equals(Role.USER)) {
                    specialRoleIndication = "("+team.getRole().replace("ROLE_","")+")";
                }
            }
            public TableComponentForTeam(Team team) {
                this.team = team;

                ensureUserRoleIndication();

                totalScore = rankingMap.getOrDefault(team.getName(),0L);
            }

            public boolean isShowUserButtons() {
                return isShowAllUsers;
            }
            public String createHtml() {
                StringBuilder sb = new StringBuilder();
                String simpleClickEvent = isShowAllUsers?"clientSelectSubtable(this, '"+team.getUuid()+"')":"";

                String viewTitle = personalWorkspaceList.isEmpty()?"deze gebruiker heeft nog geen opdrachten gedaan": personalWorkspaceList.toString();

                sb.append("<tbody id='"+team.getUuid()+"'><tr class='"+ rowClass +"' onclick=\""+simpleClickEvent+"\"><td class='alignRight'>"+counter+"</td><td class='minWidth100'>"+team.getName()+ specialRoleIndication +"</td><td title='"+viewTitle+"' class='alignRight'>"+ personalWorkspaceList.size()+"</td><td class='alignRight minWidth100'>"+totalScore+"</td></tr>");

                if (isShowUserButtons()) {
                    String inputField = "<i>registreer contactgegevens:</i><br/><input type='text' placeholder='registreer contactgegevens' onchange=\"updateTeamStatus($(this).closest('tbody').attr('id'),this.value)\" value='"+team.getCompany()+"' class='minWidth200 font10px'/>";
                    String deleteButton = "<button class='font10px cursorPointer smallBlackBorder minWidth100' onclick=\"updateTeamStatus($(this).closest('tbody').attr('id'),'ARCHIVE');$(this).closest('tbody').addClass('hide')\">archive team</button><br/>";
                    String disqualifyButton = "<button class='font10px cursorPointer smallBlackBorder minWidth100' onclick=\"updateTeamStatus($(this).closest('tbody').attr('id'),'DISQUALIFY');$(this).closest('tbody').addClass('hide')\">disqualify</button><br/>";
                    if (team.getRole().equals(Role.ADMIN)) {
                        deleteButton = "";
                        inputField = "";
                        disqualifyButton = "";
                    }
                    sb.append("<tr class='subrows hide font10px'><td colspan=2>");
                    sb.append(inputField);
                    sb.append("</td><td colspan=2 class='alignCenter'>"+deleteButton+disqualifyButton+"<button onclick=\"$('.passwordModal').toggleClass('hide')\" class='font10px minWidth100 smallBlackBorder cursorPointer nowrap'>password update</button></td></tr>");
                }
                sb.append("</tbody>");
                return sb.toString();
            }
        }

        public String createHtmlTableRows() {
            StringBuilder sb = new StringBuilder();
            for (String name: orderedList) {
                Team team = teamStore.get(name);
                if (!team.isDisabled()) {
                    continue;
                }
                sb.append(new TableComponentForTeam(team).createHtml());
                counter ++;
            }
            return sb.toString();
        }
        private boolean isNoCompetitionFound() {
            return counter==1;
        }
        private String createHtmlHeader() {
            return "<tr><th>Nr</th><th>Team</th><th>Games</th><th>Score</th></tr>";
        }
        public String createHtml() {
            counter = 1;

            String displayTable = orderedList.size()>10 ?" scrollableTable ":"";
            StringBuilder sb = new StringBuilder();
            sb.append("<table class='roundGrayBorder table "+displayTable+"' ><thead>"+createHtmlHeader()+"</thead>" + createHtmlTableRows());

            if (isNoCompetitionFound()) {
                sb.append("<tr><td colspan=4>");
                sb.append("De competitie is nog niet gestart.");
                sb.append("</td></tr>");
            }
            sb.append("</table>");
            return sb.toString();
        }
    }

    public String toSimpleBootstrapTableForTeams(List<Team> teams, boolean isShowAllUsers, List<Ranking> rankings) {
        return new BootstrapTableForTeams(teams, isShowAllUsers, rankings).createHtml();
    }

    public String toSimpleBootstrapTableForAssignmentStatus() {
        StringBuilder sb = new StringBuilder();
        List<DtoAssignmentState> list = createAssignmentStatusMap();
        if (list.isEmpty()) {
            return "";
        }
        sb.append("<br/><table class='roundGrayBorder table' ><thead><tr><th>Nr</th><th>Opdracht</th><th>Status</th><th>Starttijd</th><th>Highscore</th></tr></thead>");

        List<String[]> highscoreList = assignmentStatusRepository.getHighscoreList();
        Map<String, String[]> highscoreMap = new LinkedHashMap<>();

        for (String[] item : highscoreList) {
            highscoreMap.put(item[0],item);
        }

        int counter = 1;
        for (DtoAssignmentState orderedAssignment: list) {
            boolean isStateCurrent = orderedAssignment.state.contains("CURRENT");
            String viewState = orderedAssignment.state;
            if (isStateCurrent) {
                viewState = "<a href='./'>"+viewState+"</a>";
            }
            String viewName = "<a href='./assignmentAdmin?assignment="+orderedAssignment.name+"' title='view assignment'>"+orderedAssignment.name+"</a>";
            String viewOrder = "<a href='./assignmentAdmin?assignment="+orderedAssignment.name+"&solution' title='view solution'>"+counter+"</a>";
            String viewTime = "";
            String viewScore = "0";
            if (highscoreMap.containsKey(orderedAssignment.name)) {
                viewTime = highscoreMap.get(orderedAssignment.name)[3].split("\\.")[0];
                viewScore = highscoreMap.get(orderedAssignment.name)[2];
            }

            sb.append("<tr><td>"+viewOrder+"</td><td>"+viewName+"</td><td>"+viewState + "</td><td>"+viewTime+"</td><td>"+viewScore+"</td></tr>");
            counter++;
        }
        sb.append("</table>");
        return sb.toString();
    }

    private class SimpleAssignmentFileDetailsPanel {
        private String assignmentName;
        private File directory;
        private List<File> fileList = new ArrayList<>();
        private List<String> errorList = new ArrayList<>();
        private String localPath = mojServerProperties.getAssignmentRepo().toFile().getParentFile().getPath();
        public String createTableFromAssignmentList(List<AssignmentDescriptor> assignmentDescriptorList) {
            StringBuilder sb = new StringBuilder();
            sb.append("<span class='top'>");
            for (AssignmentDescriptor descriptor: assignmentDescriptorList) {
                AssignmentFiles wrapper = descriptor.getAssignmentFiles();
                initialize(descriptor);
                sb.append("<span title='"+readYamlText(descriptor)+"'>");
                sb.append(createTable(wrapper));
                sb.append("</span>");
            }
            sb.append("</span>");
            return sb.toString();
        }
        private String readYamlText(AssignmentDescriptor descriptor) {
            String text = "";
            try {
                text = Files.readString(new File(descriptor.getDirectory().toFile(),"assignment.yaml").toPath()).replace("'","\"");
            } catch (Exception ex) {
                log.error(ex.getMessage(), ex);
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
            header.append(assignmentName+"' - files:"+fileList.size());
            header.append("<div  class='extra hide'></div></th><th class='extra hide'>Type</th><th class='extra hide'>Size</th><th class='extra hide'>Last Modified</th></tr>");
            if (!errorList.isEmpty()) {
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
        return new SimpleAssignmentFileDetailsPanel().createTableFromAssignmentList(assignmentDescriptorList);
    }

    public String toSimpleBootstrapTable(List<AssignmentDescriptor> assignmentDescriptorList) {
        return new SimpleAssignmentPanel().createTableFromAssignmentList(assignmentDescriptorList);
    }

    private static class SimpleAssignmentPanel {

        public String createTableFromAssignmentList(List<AssignmentDescriptor> assignmentDescriptorList) {
            StringBuilder sb = new StringBuilder();

            sb.append("<br/><table class='roundGrayBorder table' ><thead><tr><th>Opdracht</th><th>Auteur</th><th>Bonus</th><th>Tijd</th><th>Java</th><th>Level</th><th>Labels</th></tr></thead>");
            for (AssignmentDescriptor descriptor: assignmentDescriptorList) {
                String bonus = createBonusInfo(descriptor);
                String title = descriptor.getDisplayName()+ " - VIEW LABELS FOR MORE DETAILS";
                String author = createShortAuthorColumn(descriptor);
                long duration = descriptor.getDuration().toMinutes();
                String selectionBox = toSelectionBox(descriptor.getLabels());
                String postfix = createAssignmentWarningIfNeeded(descriptor);
                List<Path> list = descriptor.getAssignmentFiles().getSolution();
                File directory = descriptor.getDirectory().toFile();
                AssignmentModel model = new AssignmentModel();
                model.solutionFile = new File(directory, "assets/" +list.get(0).toFile().getName());
                model.problemFile = new File(directory, "src/main/java/" +descriptor.getAssignmentFiles().getSources().getEditable().get(0).toFile().getName());

                sb.append("<tr title=\""+title+"\"  class='cursorPointer' onclick=\"doViewDeltaSolution('"+descriptor.getName()+"',this)\"><td>");
                sb.append(descriptor.getName()+" <i>"+postfix+"</i></td><td>"+author+"</td><td>"+bonus+"</td><td>"+duration+"</td><td>"+descriptor.getJavaVersion()+"</td><td>"+descriptor.getDifficulty() + "</td><td>"+selectionBox);

                sb.append("<textarea class='hide'>"+model.createDiffString()+"</textarea>");
                sb.append("</td></tr>");
            }
            sb.append("</table>");
            return sb.toString();
        }
        private String createBonusInfo(AssignmentDescriptor descriptor) {
            boolean isWithHiddenTests = !descriptor.getAssignmentFiles().getTestSources().getHiddenTests().isEmpty();
            String bonus = "" + descriptor.getScoringRules().getSuccessBonus() ;
            boolean isWithIndividualTestBonus = descriptor.getLabels().toString().contains("[test");
            if (isWithHiddenTests) {
                File directory = descriptor.getDirectory().toFile();
                File hiddenTestFile = new File(directory, "src/test/java/" +descriptor.getAssignmentFiles().getTestSources().getHiddenTests().get(0).toFile().getName());
                try {
                    String content = FileUtils.readFileToString(hiddenTestFile, Charset.defaultCharset()).replace("\"","'");
                    String tokenForHiddenTestBonus = "<sup title=\""+content+"\">(*1)</sup>";
                    bonus += tokenForHiddenTestBonus;
                } catch (Exception ex) {
                    log.error(ex.getMessage(), ex);
                }

            }
            if (isWithIndividualTestBonus) {
                String tokenForIndividualBonus = "<sup title='"+descriptor.getLabels().toString().replace("_","=")+"'>(*2)</sup>";
                bonus += tokenForIndividualBonus;
            }
            return bonus;
        }
        private String createAssignmentWarningIfNeeded(AssignmentDescriptor descriptor) {
            boolean isNotReady = descriptor.getLabels().contains("not-ready")||descriptor.getLabels().contains("label1")||descriptor.getLabels().contains("label2");
            boolean isNotForCompetition = descriptor.getLabels().contains("internet-searchable");
            String postfix = "";
            if (isNotReady) {
                postfix = "(NOT READY) ";
            } else
            if (isNotForCompetition) {
                postfix = "(NOT FOR DIGITAL EVENT) ";
            }
            return postfix;
        }
        private String createShortAuthorColumn(AssignmentDescriptor descriptor) {
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
            return author;
        }
        private String toSelectionBox(List<String> list) {
            StringBuilder sb = new StringBuilder();
            sb.append("<select >");
            for (String item: list) {
                sb.append("<option>"+item+"</option>");
            }
            sb.append("</select>");
            return sb.toString();
        }
        @Data
        private class AssignmentModel {
            private File problemFile;
            private File solutionFile;

            private String createDiffString() {
                StringBuilder html = new StringBuilder();
                try {
                    List<String> original = FileUtils.readLines(problemFile, Charset.defaultCharset());
                    List<String> revised = FileUtils.readLines(solutionFile, Charset.defaultCharset());
                    String label = getProblemFile().getName();
                    for (String line: UnifiedDiffUtils.generateUnifiedDiff(label, label, original, DiffUtils.diff(original, revised), 10)) {
                        html.append(line + "\n");
                    }
                } catch (Exception ex) {
                    html.append("ERROR during DIFF");
                    log.error(ex.getMessage(), ex);
                }
                return html.toString();
            }
        }
    }
}
