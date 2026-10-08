INSERT INTO app_user(email,keycloak_subject,locale,time_format,default_timezone)
VALUES ('search-actor@example.test','user','en-US','H24','UTC');
INSERT INTO schedule(id,code,name) VALUES (1,'DAY','Day'),(2,'NIGHT','Night');
INSERT INTO employee(id,employee_number,name,surname,email,schedule_id) VALUES
(10,'EMP-0002','Alice','Anders','a%_!@test.invalid',1),
(11,'EMP-0001','Alice','Brown','alice@test.invalid',1),
(12,'EMP-0003','Carol','Clark','carol@test.invalid',2),
(13,'EMP-0004','Dave','Brown','dave@test.invalid',1);
INSERT INTO worksite(id,code,name,time_zone,scope)
VALUES (20,'HQ','Headquarters','UTC','ASSIGNED'),(21,'REMOTE','Remote','UTC','ASSIGNED');
INSERT INTO employee_worksite(employee_id,worksite_id) VALUES(10,20),(10,21),(11,21),(12,20);
