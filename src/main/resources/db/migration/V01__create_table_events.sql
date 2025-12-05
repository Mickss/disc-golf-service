create table events (
  id char(36) not null primary key,
  tournamentDate varchar(255),
  registrationStart varchar(255),
  registrationEnd varchar(255),
  pdga varchar(255),
  tournamentTitle varchar(255) not null,
  region varchar(255),
  externalLink varchar(255),
  status varchar(255) not null
);
