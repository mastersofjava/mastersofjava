-- noinspection SqlNoDataSourceInspectionForFile
alter table teams ADD COLUMN uuid uuid NOT NULL DEFAULT random_uuid();
alter table teams
  add constraint teams_uuid_uc unique (uuid);
alter table teams DROP COLUMN cpassword;
