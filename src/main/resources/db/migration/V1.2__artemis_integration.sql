-- drop previous schema
drop table test_cases;
drop table submit_attempts;
drop table test_attempts;
drop table compile_attempts;
drop table assignment_results;
drop table assignment_statuses;
drop table ordered_assignments;
drop table assignments;
drop table competition_sessions;
drop table competitions;
drop table team_users;
drop table teams;
drop table users;

drop sequence assignment_results_seq;
drop sequence assignment_statuses_seq;
drop sequence assignments_seq;
drop sequence competition_sessions_seq;
drop sequence competitions_seq;
drop sequence compile_attempts_seq;
drop sequence ordered_assignments_seq;
drop sequence submit_attempts_seq;
drop sequence teams_seq;
drop sequence test_attempts_seq;
drop sequence test_cases_seq;
drop sequence users_seq;

-- create new schema
create sequence assignment_results_seq start with 1 increment by 50;
create sequence assignment_status_seq start with 1 increment by 50;
create sequence assignments_seq start with 1 increment by 50;
create sequence competition_sessions_seq start with 1 increment by 50;
create sequence competitions_seq start with 1 increment by 50;
create sequence compile_attempts_seq start with 1 increment by 50;
create sequence competition_assignments_seq start with 1 increment by 50;
create sequence submit_attempts_seq start with 1 increment by 50;
create sequence team_assignment_statuses_seq start with 1 increment by 50;
create sequence teams_seq start with 1 increment by 50;
create sequence test_attempts_seq start with 1 increment by 50;
create sequence test_cases_seq start with 1 increment by 50;
create sequence users_seq start with 1 increment by 50;

create table assignment_results
(
    id                        bigint not null,
    bonus                     bigint not null,
    final_score               bigint not null,
    initial_score             bigint not null,
    penalty                   bigint not null,
    team_assignment_status_id bigint not null,
    primary key (id)
);

create table team_assignment_statuses
(
    id                     bigint    not null,
    date_time_completed    timestamp,
    date_time_end          timestamp,
    date_time_start        timestamp not null,
    uuid                   uuid      not null,
    assignment_id          bigint    not null,
    competition_session_id bigint    not null,
    team_id                bigint    not null,
    primary key (id)
);

create table assignments
(
    id                    bigint       not null,
    allowed_submits       integer      not null,
    assignment_descriptor varchar(255) not null,
    assignment_duration   bigint       not null,
    name                  varchar(255) not null,
    uuid                  uuid         not null,
    collection            varchar(255) not null,
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
    aborted              boolean,
    compiler_output      text,
    date_time_end        timestamp,
    date_time_register   timestamp not null,
    date_time_start      timestamp,
    reason               text,
    success              boolean,
    timeout              boolean,
    trace                text,
    uuid                 uuid      not null,
    worker               text,
    assignment_status_id bigint    not null,
    primary key (id)
);
create table competition_assignments
(
    id             bigint  not null,
    idx            integer not null,
    assignment_id  bigint  not null,
    competition_id bigint  not null,
    primary key (id)
);
create table submit_attempts
(
    id                        bigint    not null,
    aborted                   boolean,
    assignment_time_remaining bigint    not null,
    date_time_end             timestamp,
    date_time_register        timestamp not null,
    date_time_start           timestamp,
    reason                    text,
    success                   boolean,
    trace                     text,
    uuid                      uuid      not null,
    worker                    text,
    assignment_status_id      bigint    not null,
    test_attempt_id           bigint,
    primary key (id)
);

create table assignment_statuses
(
    id                     bigint    not null,
    date_time_end          timestamp,
    date_time_start        timestamp not null,
    time_remaining         bigint    not null,
    assignment_id          bigint    not null,
    competition_session_id bigint    not null,
    primary key (id)
);

create table teams
(
    id         bigint       not null,
    company    varchar(255),
    country    varchar(255),
    indication varchar(255),
    name       varchar(255) not null,
    uuid       uuid         not null,
    primary key (id)
);
create table test_attempts
(
    id                   bigint    not null,
    aborted              boolean,
    date_time_end        timestamp,
    date_time_register   timestamp not null,
    date_time_start      timestamp,
    reason               text,
    trace                text,
    uuid                 uuid      not null,
    worker               text,
    assignment_status_id bigint    not null,
    compile_attempt_id   bigint,
    primary key (id)
);
create table test_cases
(
    id                 bigint       not null,
    aborted            boolean,
    date_time_end      timestamp,
    date_time_register timestamp    not null,
    date_time_start    timestamp,
    name               varchar(255) not null,
    reason             text,
    success            boolean,
    test_output        text,
    timeout            boolean,
    trace              text,
    uuid               uuid         not null,
    worker             text,
    test_attempt_id    bigint       not null,
    primary key (id)
);
create table users
(
    id          bigint       not null,
    email       varchar(255) not null,
    family_name varchar(255) not null,
    given_name  varchar(255) not null,
    name        varchar(255) not null,
    uuid        uuid         not null,
    team_id     bigint,
    primary key (id)
);
alter table assignments
    add constraint assignments_assignment_descriptor_uk unique (assignment_descriptor);
alter table assignments
    add constraint assignments_assignment_name_uk unique (name);
alter table assignments
    add constraint assignments_assignment_uuid_uk unique (uuid);
alter table competition_sessions
    add constraint competition_sessions_uuid_uk unique (uuid);
alter table competitions
    add constraint competitions_uuid_uk unique (uuid);
alter table compile_attempts
    add constraint compile_attempts_uuid_uk unique (uuid);
alter table competition_assignments
    add constraint competition_assignments_competition_idx_uk unique (idx, competition_id);
alter table submit_attempts
    add constraint submit_attempts_uuid_uk unique (uuid);
alter table team_assignment_statuses
    add constraint team_assignment_statuses_cs_a_t_uk unique (competition_session_id, assignment_id, team_id);
alter table team_assignment_statuses
    add constraint team_assignment_statuses_uuid_uk unique (uuid);
alter table teams
    add constraint teams_name_uk unique (name);
alter table teams
    add constraint teams_uuid_uk unique (uuid);
alter table test_attempts
    add constraint test_attempts_uuid_uk unique (uuid);
alter table test_cases
    add constraint test_cases_uuid_uk unique (uuid);
alter table users
    add constraint users_uuid_uk unique (uuid);
alter table assignment_results
    add constraint assignment_results_assignment_status_fk foreign key (team_assignment_status_id) references team_assignment_statuses;
alter table assignment_statuses
    add constraint assignment_statuses_assignment_fk foreign key (assignment_id) references assignments;
alter table assignment_statuses
    add constraint assignment_statuses_competition_session_fk foreign key (competition_session_id) references competition_sessions;
alter table competition_sessions
    add constraint competition_sessions_competition_fk foreign key (competition_id) references competitions;
alter table compile_attempts
    add constraint compile_attempts_assignment_status_fk foreign key (assignment_status_id) references team_assignment_statuses;
alter table competition_assignments
    add constraint competition_assignments_assignment_fk foreign key (assignment_id) references assignments;
alter table competition_assignments
    add constraint competition_assignments_competition_fk foreign key (competition_id) references competitions;
alter table submit_attempts
    add constraint submit_attempts_assignment_status_fk foreign key (assignment_status_id) references team_assignment_statuses;
alter table submit_attempts
    add constraint submit_attempts_test_attempt_fk foreign key (test_attempt_id) references test_attempts;
alter table team_assignment_statuses
    add constraint team_assignment_statuses_assignment_fk foreign key (assignment_id) references assignments;
alter table team_assignment_statuses
    add constraint team_assignment_statuses_competition_session_fk foreign key (competition_session_id) references competition_sessions;
alter table team_assignment_statuses
    add constraint team_assignment_statuses_team_fk foreign key (team_id) references teams;
alter table test_attempts
    add constraint test_attempts_assignment_status_fk foreign key (assignment_status_id) references team_assignment_statuses;
alter table test_attempts
    add constraint test_attempts_compile_attempt_fk foreign key (compile_attempt_id) references compile_attempts;
alter table test_cases
    add constraint test_cases_test_attempt_fk foreign key (test_attempt_id) references test_attempts;
alter table users
    add constraint users_team_fk foreign key (team_id) references teams;