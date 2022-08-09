-- ordering
CREATE USER ordering WITH ENCRYPTED PASSWORD 'ordering';
CREATE DATABASE ordering;
GRANT ALL PRIVILEGES ON DATABASE ordering TO ordering;

-- delivery
CREATE USER delivery WITH ENCRYPTED PASSWORD 'delivery';
CREATE DATABASE delivery;
GRANT ALL PRIVILEGES ON DATABASE delivery TO delivery;

-- delivery
CREATE USER customer WITH ENCRYPTED PASSWORD 'customer';
CREATE DATABASE customer;
GRANT ALL PRIVILEGES ON DATABASE customer TO customer;