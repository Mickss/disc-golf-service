create table users (
    user_id char(36) not null primary key,
    email varchar(255) unique not null,
    password_hash varchar(255) unique not null,
    created_at timestamp default current_timestamp,
    role ENUM('ADMIN', 'PLAYER') default 'PLAYER'
);
