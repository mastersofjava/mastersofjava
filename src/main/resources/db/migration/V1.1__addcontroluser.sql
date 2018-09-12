-- noinspection SqlNoDataSourceInspectionForFile
INSERT INTO TEAMS (id, name, password, role)
VALUES (nextval('teams_seq'),
        'control',
        '$2a$10$.kF3H9JfKnuozxIQ7XHTZOaOrvK1rZ8DIEHzZ.ZpulSfI/5RCJCf.',
        'ROLE_CONTROL');

