alter table competition_sessions
    add column session_type varchar(255) not null default 'GROUP';