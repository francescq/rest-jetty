CREATE TABLE IF NOT EXISTS ENTITIES (
	ID VARCHAR(36) PRIMARY KEY, 
	CONTENT VARCHAR(2000), 
	CREATE_DATE TIMESTAMP, 
	CREATE_USER VARCHAR(100)
);