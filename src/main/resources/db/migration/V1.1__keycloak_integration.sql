alter table teams
    drop column role;

create sequence users_seq start with 1 increment by 50;
create table users
(
    id          bigint       not null,
    uuid        uuid         not null,
    name        varchar(255) not null,
    given_name  varchar(255) not null,
    family_name varchar(255) not null,
    email       varchar(255) not null,
    primary key (id)
);

alter table users
    add constraint users_uuid_unique unique (uuid);

create table team_users
(
    team_id bigint not null,
    user_id bigint not null,
    primary key (team_id, user_id)
)