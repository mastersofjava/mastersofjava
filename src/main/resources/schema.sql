drop table result if exists;

create table result (

	id int not null primary key auto_increment,
	team varchar(200) not null,
	assignment varchar(200) not null, 
	score int default 0 not null,
	FOREIGN KEY (team) REFERENCES team(name)
);
	
drop table team if exists;

create table team (

	name varchar(200) primary key not null,
	password varchar(200),
	role varchar(200),

);