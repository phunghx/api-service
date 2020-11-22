## Default account:





#### DEV
``` json
{
  "email":"content@x.education",
  "password":"123456",
  "full_name": "XED",
   "user_profile": {
        "username": "up-be198039-0170-4da5-a17f-50a1c05c4d67",
        "already_confirmed": true,
        "full_name": "XED",
        "email": "content@x.education",
        "updated_time": 1575559360980,
        "created_time": 1575559360980
      }
}

```

#### PRODUCTION
``` json
{
  "email":"content@x.education",
  "password":"xed@2019!",
  "full_name": "XED",
  "user_profile": {
      "username": "up-ac976ee6-158c-4e87-8b4a-39b55ca46968",
      "already_confirmed": true,
      "full_name": "XED",
      "email": "content@x.education",
      "updated_time": 1576221084508,
      "created_time": 1576221084508
    }
}
```

#### REDIS PUBSUB

- Channel: `deck_status_changed`
- Body: 
```json
{
    "object_type": "deck",
    "object_id": "wcfgv-w423xdfr32q22",
    "status" : 2
}
```





- Channel: `review_changed`
- Body: 
    + `context`: deck, bot
    + `action`: added, updated, removed

```json
{
    "context": "bot",
    "action": "added",
    "username": "up-w423xdfr32q22",
    "id" : "erfyvbb65y4wvc4"
}
```