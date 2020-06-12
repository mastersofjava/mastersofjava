create sequence assignment_results_seq start with 1 increment by 50;
create sequence assignment_statuses_seq start with 1 increment by 50;
create sequence assignments_seq start with 1 increment by 50;
create sequence competition_sessions_seq start with 1 increment by 50;
create sequence competitions_seq start with 1 increment by 50;
create sequence compile_attempts_seq start with 1 increment by 50;
create sequence ordered_assignments_seq start with 1 increment by 50;
create sequence submit_attempts_seq start with 1 increment by 50;
create sequence teams_seq start with 1 increment by 50;
create sequence test_attempts_seq start with 1 increment by 50;
create sequence test_cases_seq start with 1 increment by 50;

create table assignment_results
(
  id                   bigint not null,
  bonus                bigint not null,
  final_score          bigint not null,
  initial_score        bigint not null,
  penalty              bigint not null,
  uuid                 uuid   not null,
  assignment_status_id bigint not null,
  primary key (id)
);

create table assignment_statuses
(
  id                     bigint not null,
  assignment_duration    bigint,
  date_time_end          timestamp,
  date_time_start        timestamp,
  uuid                   uuid   not null,
  assignment_id          bigint not null,
  competition_session_id bigint not null,
  team_id                bigint not null,
  primary key (id)
);

create table assignments
(
  id                    bigint       not null,
  assignment_descriptor varchar(255) not null,
  name                  varchar(255) not null,
  uuid                  uuid         not null,
  primary key (id)
);

create table competition_sessions
(
  id             bigint not null,
  uuid           uuid   not null,
  competition_id bigint not null,
  primary key (id)
);

create table competitions
(
  id   bigint       not null,
  name varchar(255) not null,
  uuid uuid         not null,
  primary key (id)
);

create table compile_attempts
(
  id                   bigint    not null,
  compiler_output      TEXT,
  date_time_end        timestamp not null,
  date_time_start      timestamp not null,
  success              boolean   not null,
  uuid                 uuid      not null,
  assignment_status_id bigint    not null,
  primary key (id)
);

create table ordered_assignments
(
  id             bigint  not null,
  idx            integer not null,
  uuid           uuid    not null,
  assignment_id  bigint  not null,
  competition_id bigint  not null,
  primary key (id)
);

create table submit_attempts
(
  id                      bigint    not null,
  assignment_time_elapsed bigint,
  date_time_end           timestamp not null,
  date_time_start         timestamp not null,
  success                 boolean   not null,
  uuid                    uuid      not null,
  assignment_status_id    bigint    not null,
  compile_attempt_id      bigint,
  test_attempt_id         bigint,
  primary key (id)
);

create table teams
(
  id         bigint       not null,
  company    varchar(255),
  country    varchar(255),
  name       varchar(255) not null,
  password   varchar(255),
  role       varchar(255),
  indication varchar(255),
  uuid       uuid         not null,
  primary key (id)
);

create table test_attempts
(
  id                   bigint    not null,
  date_time_end        timestamp,
  date_time_start      timestamp not null,
  uuid                 uuid      not null,
  assignment_status_id bigint    not null,
  primary key (id)
);

create table test_cases
(
  id              bigint       not null,
  date_time_end   timestamp    not null,
  date_time_start timestamp    not null,
  name            varchar(255) not null,
  success         boolean      not null,
  test_output     TEXT,
  timeout         boolean      not null,
  uuid            uuid         not null,
  test_attempt_id bigint       not null,
  primary key (id)
);

alter table assignment_statuses
  add constraint competition_assignment_team_unique unique (competition_session_id, assignment_id, team_id);
alter table assignments
  add constraint assignments_assignment_descriptor_unique unique (assignment_descriptor);
alter table assignments
  add constraint assignments_assignment_name_unique unique (name);
alter table assignments
  add constraint assignments_assignment_uuid_unique unique (uuid);

alter table competition_sessions
  add constraint competition_sessions_uuid_unique unique (uuid);

alter table competitions
  add constraint competitions_uuid_unique unique (uuid);

alter table ordered_assignments
  add constraint ordered_assignments_idx_competition_id_unique unique (idx, competition_id);
alter table ordered_assignments
  add constraint ordered_assignments_uuid_unique unique (uuid);

alter table teams
  add constraint teams_name_unique unique (name);
alter table teams
  add constraint teams_uuid_unique unique (uuid);

alter table assignment_results
  add constraint assignment_results_assignment_status_fk foreign key (assignment_status_id) references assignment_statuses;

alter table assignment_statuses
  add constraint assignment_statuses_assignment_fk foreign key (assignment_id) references assignments;
alter table assignment_statuses
  add constraint assignment_statuses_competition_session_fk foreign key (competition_session_id) references competition_sessions;
alter table assignment_statuses
  add constraint assignment_statuses_team_fk foreign key (team_id) references teams;
alter table competition_sessions
  add constraint assignment_statuses_competition_fk foreign key (competition_id) references competitions;

alter table compile_attempts
  add constraint compile_attempts_assignment_status_fk foreign key (assignment_status_id) references assignment_statuses;

alter table ordered_assignments
  add constraint ordered_assignments_assignment_fk foreign key (assignment_id) references assignments;
alter table ordered_assignments
  add constraint ordered_assignments_competition_fk foreign key (competition_id) references competitions;

alter table submit_attempts
  add constraint submit_attempts_assignment_status_fk foreign key (assignment_status_id) references assignment_statuses;
alter table submit_attempts
  add constraint submit_attempts_compile_attempt_fk foreign key (compile_attempt_id) references compile_attempts;
alter table submit_attempts
  add constraint submit_attempts_test_attempt_fk foreign key (test_attempt_id) references test_attempts;

alter table test_attempts
  add constraint test_attempts_assignment_status_fk foreign key (assignment_status_id) references assignment_statuses;
alter table test_cases
  add constraint test_cases_test_attempt_fk foreign key (test_attempt_id) references test_attempts;
