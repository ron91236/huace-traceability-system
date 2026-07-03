UPDATE sys_user SET password_hash='$2b$12$Jiis25AeeowuwHBA34Lb.eaQOIYrqbrooeC0uJtuXHlnVM.kiztHm' WHERE username='admin';
SELECT id, username, SUBSTRING(password_hash, 1, 20) as hash_prefix FROM sys_user;
