drop table result if exists;

create table result (

	id int,
	team varchar(200),
	result varchar(200)

);
drop table team if exists;

create table team (

	id int not null primary key auto_increment,
	name varchar(200),
	password varchar(200),
	role varchar(200)

);