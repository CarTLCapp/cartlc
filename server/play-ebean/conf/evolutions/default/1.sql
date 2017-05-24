# --- Database schema

# --- !Ups

create table client (
  id                int auto_increment primary key,
  first_name        varchar(255),
  last_name         varchar(255),
  disabled          bit
);

create table company (
  id                int auto_increment primary key,
  name              varchar(255),
  street            varchar(255),
  city              varchar(128),
  state             varchar(64),
  disabled          bit
);

create table project (
  id                int auto_increment primary key,
  name              varchar(64),
  disabled          bit
);

create table equipment (
  id                int auto_increment primary key,
  name              varchar(128),
  disabled          bit
);

create table projectEquipmentCollection (
  id                int auto_increment primary key,
  project_id        int,
  equipment_id      int
);

alter table projectEquipmentCollection add constraint fk_cpe_project_id foreign key (project_id) references project (id) on delete restrict on update restrict;
alter table projectEquipmentCollection add constraint fk_cpe_equipment_id foreign key (equipment_id) references equipment (id) on delete restrict on update restrict;

create table note (
  id                int auto_increment primary key,
  name              varchar(128),
  type              smallint,
  disabled          bit
);

create table projectNoteCollection (
  id                int auto_increment primary key,
  project_id        int,
  note_id           int
);

alter table projectNoteCollection add constraint fk_cpn_project_id foreign key (project_id) references project (id) on delete restrict on update restrict;
alter table projectNoteCollection add constraint fk_cpn_note_id foreign key (note_id) references note (id) on delete restrict on update restrict;

create table picture (
  id                int auto_increment primary key,
  filename          varchar(255)
);

create table pictureCollection (
  id                int auto_increment primary key,
  collection_id     int,
  picture_id        int
);

alter table pictureCollection add constraint fk_picture_id foreign key (picture_id) references picture (id) on delete restrict on update restrict;

create table entry (
  id                       int auto_increment primary key,
  entry_time               date,
  project_id               int,
  address_id               int,
  equipment_id             int,
  picture_collection_id    int,
  truck_number             int
);

alter table entry add constraint fk_entry_project_id foreign key (project_id) references project (id) on delete restrict on update restrict;
alter table entry add constraint fk_entry_address_id foreign key (address_id) references company (id) on delete restrict on update restrict;
alter table entry add constraint fk_entry_equipment_id foreign key (equipment_id) references equipment (id) on delete restrict on update restrict;
alter table entry add constraint fk_entry_picture_collection_id foreign key (picture_collection_id) references pictureCollection (id) on delete restrict on update restrict;

create table state (
  id        int auto_increment primary key,
  name      varchar(64),
  abbr      varchar(8)
);

# --- !Downs

SET REFERENTIAL_INTEGRITY FALSE;

drop table if exists client;
drop table if exists company;
drop table if exists project;
drop table if exists equipment;
drop table if exists projectEquipmentCollection;
drop table if exists note;
drop table if exists projectNoteCollection;
drop table if exists picture;
drop table if exists pictureCollection;
drop table if exists entry;
drop table if exists state;

SET REFERENTIAL_INTEGRITY TRUE;
