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
package nl.moj.server;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import nl.moj.common.storage.StorageService;
import nl.moj.server.TaskControlController.ActiveAssignmentVO.ActiveAssignmentVOBuilder;
import nl.moj.server.TaskControlController.CompetitionSessionVO.CompetitionSessionVOBuilder;
import nl.moj.server.assignment.model.Assignment;
import nl.moj.server.assignment.repository.AssignmentRepository;
import nl.moj.server.assignment.service.AssignmentService;
import nl.moj.server.authorization.Role;
import nl.moj.server.competition.model.Competition;
import nl.moj.server.competition.model.CompetitionAssignment;
import nl.moj.server.competition.model.CompetitionSession;
import nl.moj.server.competition.model.CompetitionSession.SessionType;
import nl.moj.server.competition.repository.CompetitionRepository;
import nl.moj.server.competition.repository.CompetitionSessionRepository;
import nl.moj.server.competition.service.CompetitionService;
import nl.moj.server.competition.service.CompetitionServiceException;
import nl.moj.common.config.properties.MojServerProperties;
import nl.moj.server.runtime.CompetitionRuntime;
import nl.moj.server.runtime.model.ActiveAssignment;
import nl.moj.server.runtime.model.AssignmentStatus;
import nl.moj.server.user.model.User;
import nl.moj.server.user.service.UserService;
import nl.moj.server.util.TransactionHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import javax.annotation.security.RolesAllowed;
import javax.transaction.Transactional;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Controller
@RequiredArgsConstructor
public class TaskControlController {

	private static final Logger log = LoggerFactory.getLogger(TaskControlController.class);

	private final CompetitionRuntime competition;

	private final AssignmentService assignmentService;

	private final AssignmentRepository assignmentRepository;

	private final CompetitionRepository competitionRepository;

	private final CompetitionSessionRepository competitionSessionRepository;

	private final CompetitionService competitionService;

	private final UserService userService;

	private final StorageService storageService;

	private final TransactionHelper trx;

	@RolesAllowed({ Role.GAME_MASTER, Role.ADMIN })
	@PostMapping(value = "/api/assignment/discover", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, String>> discoverAssignments() {
		try {
			List<Assignment> assignments = assignmentService.updateAssignments();
			if (assignments.isEmpty()) {
				return ResponseEntity.ok().body(Map.of("m", "No assignments discovered.", "reload", "false"));
			}
			log.info("Found {} assignments in folder {}.", assignments.size(), storageService.getAssignmentsFolder());
			return ResponseEntity.ok().body(Map.of("m",
					String.format("Discovered %d assignments, reloading", assignments.size()), "reload", "true"));
		} catch (Exception e) {
			log.error("Assignment discovery failed.", e);
			return ResponseEntity.ok().body(
					Map.of("m", String.format("Assignment discovery failed: %s", e.getMessage()), "reload", "false"));
		}
	}

	@RolesAllowed({ Role.GAME_MASTER, Role.ADMIN })
	@PostMapping(value = "/api/competition", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Void> addCompetition(@RequestBody AddCompetition addCompetition) {
		try {
			competitionService.createCompetition(addCompetition.getName(), addCompetition.getAssignments());
			return ResponseEntity.noContent().build();
		} catch (CompetitionServiceException cse) {
			log.error("Unable to  create competition.", cse);
			return ResponseEntity.badRequest().build();
		}
	}

	@RolesAllowed({ Role.GAME_MASTER, Role.ADMIN })
	@PostMapping(value = "/api/competition/{cid}/groupSession", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, String>> startGroupSession(@PathVariable("cid") UUID id) {
		try {
			CompetitionSession session = competition.startSession(id, CompetitionSession.SessionType.GROUP);
			return ResponseEntity
					.ok(Map.of("name", session.getCompetition().getName(), "id", session.getUuid().toString()));
		} catch (CompetitionServiceException cse) {
			log.error("Unable to  start competition session for competition {}", id, cse);
			return ResponseEntity.badRequest().build();
		}
	}

	@RolesAllowed({ Role.GAME_MASTER, Role.ADMIN })
	@PostMapping(value = "/api/competition/{cid}/singleSession", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, String>> startSingleSession(@PathVariable("cid") UUID id) {
		try {
			CompetitionSession session = competition.startSession(id, CompetitionSession.SessionType.SINGLE);
			return ResponseEntity
					.ok(Map.of("name", session.getCompetition().getName(), "id", session.getUuid().toString()));
		} catch (CompetitionServiceException cse) {
			log.error("Unable to  start competition session for competition {}", id, cse);
			return ResponseEntity.badRequest().build();
		}
	}

	@RolesAllowed({ Role.GAME_MASTER, Role.ADMIN })
	@PostMapping(value = "/api/session/{sid}/assignment/{aid}/start", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<AssignmentVO> startAssignment(@PathVariable("sid") UUID sid, @PathVariable("aid") UUID aid) {
		try {
			AssignmentStatus as = competition.startAssignment(sid, aid);
			return ResponseEntity.ok(toAssignmentVO(as));
		} catch (CompetitionServiceException cse) {
			log.error("Unable to start assignment {} for session {}.", aid, sid, cse);
			return ResponseEntity.badRequest().build();
		}
	}

	@RolesAllowed({ Role.GAME_MASTER, Role.ADMIN })
	@PostMapping(value = "/api/session/{sid}/assignment/{aid}/stop", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<AssignmentVO> stopAssignment(@PathVariable("sid") UUID sid, @PathVariable("aid") UUID aid) {
		try {
			Optional<AssignmentStatus> as = competition.stopAssignment(sid, aid);
			return as.map(assignmentStatus -> ResponseEntity.ok(toAssignmentVO(assignmentStatus)))
					.orElseGet(() -> ResponseEntity.notFound().build());
		} catch (CompetitionServiceException cse) {
			log.error("Unable to stop assignment {} for session {}.", aid, sid, cse);
			return ResponseEntity.badRequest().build();
		}
	}

	@RolesAllowed({ Role.GAME_MASTER, Role.ADMIN })
	@PostMapping(value = "/api/session/{sid}/assignment/{aid}/pause", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, String>> pauseAssignment(@PathVariable("sid") UUID sid,
			@PathVariable("aid") UUID aid) {
		return ResponseEntity.badRequest().build();
	}

	@RolesAllowed({ Role.GAME_MASTER, Role.ADMIN })
	@PostMapping(value = "/api/session/{sid}/assignment/{aid}/resume", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, String>> resumeAssignment(@PathVariable("sid") UUID sid,
			@PathVariable("aid") UUID aid) {
		return ResponseEntity.badRequest().build();
	}

	@RolesAllowed({ Role.GAME_MASTER, Role.ADMIN })
	@PostMapping(value = "/api/session/{sid}/assignment/{aid}/reset", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Void> resetAssignment(@PathVariable("sid") UUID sid, @PathVariable("aid") UUID aid) {
		try {
			competition.resetAssignment(sid, aid);
			return ResponseEntity.noContent().build();
		} catch (CompetitionServiceException cse) {
			log.error("Unable to stop assignment {} for session {}.", aid, sid, cse);
			return ResponseEntity.badRequest().build();
		}
	}

	@RolesAllowed({ Role.GAME_MASTER, Role.ADMIN })
	@GetMapping("/control")
	@Transactional
	public String taskControl(Model model, Authentication principal) {
		// TODO maybe move this creat or update stuff to a filter.
		User user = userService.createOrUpdate(principal);

		model.addAttribute("assignments", allAssignments());
		model.addAttribute("competitions", allCompetitions());
		model.addAttribute("cs", toCompetitionSessionVO(competition));

		model.addAttribute("clockStyle", "active");
		return "control";
	}

	@Value
	@Builder
	@Jacksonized
	public static class AddCompetition {
		String name;
		List<UUID> assignments;

	}

	private List<AssignmentVO> allAssignments() {
		return assignmentRepository.findAll(Sort.by("collection", "name")).stream().map(this::toAssignmentVO).toList();
	}

	private List<CompetitionVO> allCompetitions() {
		return competitionRepository.findAll(Sort.by("name")).stream().map(this::toCompetitionVO).toList();
	}

	private CompetitionVO toCompetitionVO(Competition co) {
		return CompetitionVO.builder().uuid(co.getUuid()).name(co.getName())
				.assignments(co.getAssignments().stream().map(ca -> toAssignmentVO(ca.getAssignment())).toList())
				.build();
	}

	private CompetitionSessionVO toCompetitionSessionVO(CompetitionRuntime runtime) {

		if (runtime.getSessionId() == null) {
			return CompetitionSessionVO.builder().active(false).build();
		}

		return trx.required(() -> {
			CompetitionSession session = competitionSessionRepository.findByUuid(runtime.getSessionId());
			Objects.requireNonNull(session);

			Competition competition = session.getCompetition();
			// this is a global view, so no team specific fields
			ActiveAssignment activeAssignment = runtime.getActiveAssignment(null);

			List<AssignmentStatus> assignmentStatuses = session.getAssignmentStatuses();
			List<AssignmentVO> assignments = new ArrayList<>();
			competition.getAssignmentsInOrder().forEach(ca -> {
				Optional<AssignmentStatus> as = assignmentStatuses.stream()
						.filter(a -> a.getAssignment().equals(ca.getAssignment())).findFirst();
				assignments.add(toAssignmentVO(ca, as));
			});

			ActiveAssignmentVO active = null;
			if (activeAssignment.isRunning()) {
				Optional<CompetitionAssignment> oca = competition.getAssignments().stream()
						.filter(a -> a.getAssignment().equals(activeAssignment.getAssignment())).findFirst();
				if (oca.isPresent()) {
					Optional<AssignmentStatus> as = assignmentStatuses.stream()
							.filter(a -> a.getAssignment().equals(activeAssignment.getAssignment())).findFirst();
					ActiveAssignmentVOBuilder builder = ActiveAssignmentVO.builder()
							.assignment(toAssignmentVO(oca.get(), as))
							.seconds(oca.get().getAssignment().getAssignmentDuration().toSeconds());
					if (activeAssignment.getTimeRemaining() != null) {
						builder.secondsLeft(activeAssignment.getTimeRemaining().toSeconds());
					}
					active = builder.build();
				}
			}

			CompetitionSessionVOBuilder cs = CompetitionSessionVO.builder().assignments(assignments)
					.activeAssignment(active).uuid(session.getUuid()).name(competition.getName()).active(true);

			if (activeAssignment.getCompetitionSession() != null) {
				cs.type(activeAssignment.getCompetitionSession().getSessionType());
			}
			return cs.build();
		});
	}

	private AssignmentVO toAssignmentVO(CompetitionAssignment ca, Optional<AssignmentStatus> as) {
		return AssignmentVO.builder().idx(ca.getOrder()).uuid(ca.getAssignment().getUuid())
				.name(ca.getAssignment().getName()).collection(ca.getAssignment().getCollection())
				.started(as.map(AssignmentStatus::getDateTimeStart).orElse(null))
				.ended(as.map(AssignmentStatus::getDateTimeEnd).orElse(null))
				.duration(ca.getAssignment().getAssignmentDuration())
				.remaining(as.map(AssignmentStatus::getTimeRemaining).orElse(null))
				.submits(ca.getAssignment().getAllowedSubmits()).build();
	}

	private AssignmentVO toAssignmentVO(Assignment assignment) {
		return AssignmentVO.builder().idx(-1).uuid(assignment.getUuid()).name(assignment.getName())
				.collection(assignment.getCollection()).duration(assignment.getAssignmentDuration())
				.submits(assignment.getAllowedSubmits()).build();
	}

	private AssignmentVO toAssignmentVO(AssignmentStatus as) {
		return AssignmentVO.builder().idx(-1).uuid(as.getAssignment().getUuid()).name(as.getAssignment().getName())
				.collection(as.getAssignment().getCollection()).duration(as.getAssignment().getAssignmentDuration())
				.remaining(as.getTimeRemaining()).submits(as.getAssignment().getAllowedSubmits())
				.started(as.getDateTimeStart()).ended(as.getDateTimeEnd()).build();
	}

	@Value
	@Builder
	public static class CompetitionVO {
		UUID uuid;
		String name;
		List<AssignmentVO> assignments;
	}

	@Value
	@Builder
	public static class CompetitionSessionVO {
		UUID uuid;
		String name;
		boolean active;
		SessionType type;
		@Builder.Default
		List<AssignmentVO> assignments = new ArrayList<>();
		ActiveAssignmentVO activeAssignment;

		public boolean isAssignmentActive() {
			return activeAssignment != null;
		}
	}

	@Value
	@Builder
	public static class AssignmentVO {
		UUID uuid;
		String name;
		String collection;
		int idx;
		Instant started;
		Instant ended;
		Duration duration;
		Duration remaining;
		int submits;
	}

	@Value
	@Builder
	public static class ActiveAssignmentVO {
		AssignmentVO assignment;
		long secondsLeft;
		long seconds;
	}
}
