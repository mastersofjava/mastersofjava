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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.moj.server.assignment.model.Assignment;
import nl.moj.server.competition.model.Competition;
import nl.moj.server.competition.model.CompetitionAssignment;
import nl.moj.server.competition.repository.CompetitionRepository;
import nl.moj.server.competition.repository.CompetitionSessionRepository;
import nl.moj.server.config.properties.MojServerProperties;
import nl.moj.server.runtime.CompetitionRuntime;
import nl.moj.server.runtime.repository.AssignmentResultRepository;
import nl.moj.server.runtime.repository.AssignmentStatusRepository;
import nl.moj.server.runtime.repository.TeamAssignmentStatusRepository;
import nl.moj.server.teams.model.Team;
import nl.moj.server.teams.service.TeamService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class CompetitionService {
    private static final String YEAR_PREFIX = "20";

    private final MojServerProperties mojServerProperties;

    private final AssignmentResultRepository assignmentResultRepository;

    private final TeamAssignmentStatusRepository teamAssignmentStatusRepository;

    private final CompetitionRuntime competitionRuntime;

    private final CompetitionRepository competitionRepository;

    private final CompetitionSessionRepository competitionSessionRepository;

    private final TeamService teamService;

    private final AssignmentStatusRepository assignmentStatusRepository;

    // TODO: Sort out if we can do this lazily when the team first submits.
    public void addTeam(Team team) {
        Path teamdir = teamService.getTeamDirectory(competitionRuntime.getCompetitionSession()
                .getUuid(), team.getUuid());
        if (!Files.exists(teamdir)) {
            try {
                Files.createDirectory(teamdir);
            } catch (IOException e) {
                log.error("error creating teamdir", e);
            }
        }
    }

    public String getSelectedYearLabel() {
        String year = getSelectedYearValue();
        if (!StringUtils.isNumeric(year)) {
            year = "";
        } else {
            year = " (" + year + ")";
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
        defaultLocation.mkdirs();
        String token = "" + year + "-";
        for (File file : locationList()) {
            if (file.getName().startsWith(token)) {
                return file;
            }
        }
        return defaultLocation;
    }

    public Function<Assignment, CompetitionAssignment> createOrderedAssignments(Competition c) {
        AtomicInteger count = new AtomicInteger(0);
        return a -> {
            CompetitionAssignment oa = new CompetitionAssignment();
            oa.setAssignment(a);
            oa.setCompetition(c);
            oa.setOrder(count.getAndIncrement());
            return oa;
        };
    }

    public List<File> locationList() {
        List<File> locationList = new ArrayList<>();
        File defaultLocation = mojServerProperties.getAssignmentRepo().toFile();
        if (!defaultLocation.exists() || !defaultLocation.getParentFile().isDirectory()) {
            mojServerProperties.getAssignmentRepo().toFile().mkdirs();
            return locationList;
        }
        return Arrays.stream(defaultLocation.getParentFile().listFiles())
                .filter(file -> file.getName().startsWith(YEAR_PREFIX) && file.isDirectory())
                .collect(Collectors.toList());
    }


    public File getSelectedLocation() {
        File file = mojServerProperties.getAssignmentRepo().toFile();
        Competition c = competitionRuntime.getCompetition();
        boolean isUseDefaultLocation = c.getName().contains("|" + YEAR_PREFIX);
        if (!isUseDefaultLocation) {
            return file;
        }
        var name = c.getName().split("\\|")[1];
        if (new File(file.getParentFile(), name).isDirectory()) {
            file = new File(file.getParentFile(), name);
        }
        return file;
    }
}
