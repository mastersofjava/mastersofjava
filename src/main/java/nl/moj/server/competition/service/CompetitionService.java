/*
   Copyright 2020 First Eight BV (The Netherlands)
 

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file / these files except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package nl.moj.server.competition.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.moj.server.assignment.model.Assignment;
import nl.moj.server.competition.model.Competition;
import nl.moj.server.competition.model.CompetitionSession;
import nl.moj.server.competition.model.OrderedAssignment;
import nl.moj.server.competition.repository.CompetitionRepository;
import nl.moj.server.competition.repository.CompetitionSessionRepository;
import nl.moj.server.config.properties.MojServerProperties;
import nl.moj.server.login.SignupForm;
import nl.moj.server.runtime.CompetitionRuntime;
import nl.moj.server.runtime.model.AssignmentStatus;
import nl.moj.server.runtime.repository.AssignmentResultRepository;
import nl.moj.server.runtime.repository.AssignmentStatusRepository;
import nl.moj.server.teams.model.Role;
import nl.moj.server.teams.model.Team;
import nl.moj.server.teams.repository.TeamRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.json.JsonParser;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class CompetitionService {
    private static final String YEAR_PREFIX = "20";

    private final TeamRepository teamRepository;

    private final PasswordEncoder encoder;

    private final MojServerProperties mojServerProperties;

    private final AssignmentResultRepository assignmentResultRepository;

    private final AssignmentStatusRepository assignmentStatusRepository;

    private final CompetitionRuntime competitionRuntime;

    private final CompetitionRepository competitionRepository;

    private final CompetitionSessionRepository competitionSessionRepository;

    public PasswordEncoder getEncoder() {
        return encoder;
    }

    public void createNewTeam(SignupForm form) {
        createNewTeam(form, Role.USER);
    }
    public void createNewTeam(SignupForm form, String role) {
        SecurityContext context = SecurityContextHolder.getContext();
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(form.getName(), form
                .getPassword(), Arrays.asList(new SimpleGrantedAuthority(Role.USER)));

        Team team = Team.builder()
                .company(form.getCompany())
                .country(form.getCountry())
                .name(form.getName())
                .role(role)
                .uuid(UUID.randomUUID())
                .build();

        context.setAuthentication(authentication);
        saveNewTeam(team);
    }

    private void saveNewTeam(Team team) {
        teamRepository.save(team);

        Path teamdir = mojServerProperties.getDirectories().getBaseDirectory()
                .resolve(mojServerProperties.getDirectories().getTeamDirectory())
                .resolve(team.getName());
        if (!Files.exists(teamdir)) {
            try {
                Files.createDirectory(teamdir);
            } catch (IOException e) {
                log.error("error creating teamdir", e);
            }
        }
    }

    public class UserImporter {
        private List<String> lines;
        private List<Team> teamList = new ArrayList<>();
        private Map<String,String> teamScoresMap = new TreeMap<>();

        public UserImporter(List<String> lines) {
            this.lines = lines;
            parseLines();
        }
        private class TeamParser {
            private Team parse(String trLine) {
                String role = Role.USER;
                String[] parts = trLine.split("\\|");
                if (parts.length>=3 && parts[2].equals(Role.GAME_MASTER)) {
                    role = Role.GAME_MASTER;
                }
                if (parts.length>=3 && parts[2].equals(Role.ANONYMOUS)) {
                    role = Role.ANONYMOUS;
                }
                String name = parts[0];
                String lastPart = parts[parts.length-1];
                boolean isWithUpdateScores =  lastPart.startsWith("{")&&lastPart.endsWith("}");
                if (isWithUpdateScores) {
                    teamScoresMap.put(name, lastPart);
                }
                return Team.builder()
                        .company(parts[1])
                        .country("Nederland")
                        .name(name)
                        .role(role)
                        .uuid(UUID.randomUUID())
                        .build();
            }
        }

        private void parseLines() {
            for (String line: lines) {
                String trLine = line.trim();

                if (trLine.isEmpty()||trLine.startsWith("##")) {
                    continue;
                }
                TeamParser parser = new TeamParser();
                Team team = parser.parse(trLine);
                if (team.getName().equalsIgnoreCase("admin")) {
                    continue;
                }
                teamList.add(team);
            }
        }
        private void updateScoresForTeam(Team team, String jsonString) {
            JsonParser parser = JsonParserFactory.getJsonParser();
            Map<String, Object> scoreInputMap = parser.parseMap(jsonString);// numbers are parsed as Integers
            List<AssignmentStatus> statusList = assignmentStatusRepository.findByCompetitionSessionAndTeam(competitionRuntime.getCompetitionSession(), team);

            int counter = 0;
            for (AssignmentStatus status: statusList) {
                String id = status.getAssignment().getId().toString();
                boolean isUserHasNewScoreImport = scoreInputMap.get(id)!=null;

                if (!isUserHasNewScoreImport || status.getAssignmentResult()==null) {
                    continue;
                }
                long newScore = (Integer) scoreInputMap.get(id);

                status.getAssignmentResult().setFinalScore(newScore);
                assignmentResultRepository.save(status.getAssignmentResult());
                counter++;
            }
            log.info("imported scores: team " + team.getName() + "=" +jsonString + " (updates counted: " +counter+", statusList: " +statusList.size()+ ")");
        }

        public void addOrUpdateAllImportableTeams() {
            for (Team teamInput: this.teamList) {
                Team team = teamRepository.findByName(teamInput.getName());
                boolean isNew = team==null;
                log.info("imported team: " + teamInput.getName() + " " +teamInput.getCompany() + " " + teamInput.getRole()  + " isNew " + isNew);

                if (isNew) {
                    saveNewTeam(teamInput);
                } else {// update just the role and company
                    team.setRole(teamInput.getRole());
                    team.setCompany(teamInput.getCompany());
                    teamRepository.save(team);
                    if (teamScoresMap.containsKey(team.getName())) {
                        updateScoresForTeam(team,teamScoresMap.get(team.getName()));
                    }
                }
            }
        }
    }

    public void importTeams(List<String> lines) {
        UserImporter importer = new UserImporter( lines);
        importer.addOrUpdateAllImportableTeams();
    }

    public String getSelectedYearLabel() {
        String year = getSelectedYearValue();
        if (!StringUtils.isNumeric(year)) {
            year = "";
        } else {
            year = " ("+year + ")";
        }
        return year;
    }
    public String getSelectedYearValue() {
        String year = getSelectedLocation().getName().split("-")[0];
        if (!StringUtils.isNumeric(year)) {
            year = "";
        }
        return year;
    }
    public File getLocationByYear(int year) {
        File defaultLocation = mojServerProperties.getAssignmentRepo().toFile();

        String token = ""+year  +"-";
        for (File file: locationList()) {
            if (file.getName().startsWith(token)) {
                return file;
            }
        }
        return defaultLocation;
    }

    public Function<Assignment, OrderedAssignment> createOrderedAssignments(Competition c) {
        AtomicInteger count = new AtomicInteger(0);
        return a -> {
            OrderedAssignment oa = new OrderedAssignment();
            oa.setAssignment(a);
            oa.setCompetition(c);
            oa.setUuid(UUID.randomUUID());
            oa.setOrder(count.getAndIncrement());
            return oa;
        };
    }
    public List<File> locationList() {
        List<File> locationList = new ArrayList<>();
        File defaultLocation = mojServerProperties.getAssignmentRepo().toFile();
        if (!defaultLocation.exists()||!defaultLocation.getParentFile().isDirectory()) {
            return locationList;
        }
        return Arrays.stream(defaultLocation.getParentFile().listFiles())
                .filter(file -> file.getName().startsWith(YEAR_PREFIX) && file.isDirectory())
                .collect(Collectors.toList());
    }


    public File getSelectedLocation() {
        File file = mojServerProperties.getAssignmentRepo().toFile();
        Competition c = competitionRuntime.getCompetition();
        boolean isUseDefaultLocation = c.getName().contains("|" +YEAR_PREFIX);
        if (!isUseDefaultLocation) {
            return file;
        }
        var name = c.getName().split("\\|")[1];
        if (new File(file.getParentFile(),name).isDirectory()) {
            file = new File(file.getParentFile(),name);
        }
        return file;
    }

    public @ResponseBody
    List<Competition> getAvailableCompetitions() {
        List<Competition> listAll = competitionRepository.findAll();
        List<Competition> result = new ArrayList<>();
        for (Competition competition : listAll) {
            List<CompetitionSession> sessions = competitionSessionRepository.findByCompetition(competition);

            for (CompetitionSession session : sessions) {
                if (session.isAvailable()) {
                    result.add(competition);
                }
            }
        }
        return result;
    }

}
