alter table competition_sessions
    add column session_type varchar(255) not null default 'GROUP';
    
alter table team_assignment_statuses
    alter column date_time_start set null;
    
    
