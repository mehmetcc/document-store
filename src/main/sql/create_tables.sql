CREATE DATABASE sinner;

\c sinner

CREATE TABLE person (
	person_id serial PRIMARY KEY,
	username TEXT UNIQUE NOT NULL,
	password TEXT NOT NULL,
	email TEXT UNIQUE NOT NULL,
	role TEXT NOT NULL
);

CREATE TABLE document (
	document_id serial PRIMARY KEY,
	content TEXT NOT NULL
);