# Therapeutists management system


Run mongodb with docker-compose:

```
docker-compose up -d
```

Create `db-users` database and `users` collection with mongo-express at [http://localhost:8081](http://localhost:8081)

Run the server with `sbt run` and try:

post some users :

```
curl -H "Content-Type: application/json" -H "Authorization: Bearer ABCD" http://localhost:8080/users -X POST -d '{"name": "asdf", "email": "adsf@test.com", "tags": [{"name": "youpie"}, {"name": "haha"}]}'
curl -H "Content-Type: application/json" -H "Authorization: Bearer ABCD" http://localhost:8080/users -X POST -d '{"name": "tutu", "email": "tutu@tutu.com", "tags": []}'
```

get all users:

```
curl -H "Content-Type: application/json" -H "Authorization: Bearer ABCD" http://localhost:8080/users
```
