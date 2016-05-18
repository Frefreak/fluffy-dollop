## Database

- `user`
	- uid		Integer
	- username	string
	- md5password	hex string
	- key		hex string, used for AES encryption (generated from username/pwd)

- `token_map`
	- token		hex string, used for uniquely identify client
	- uid		Integer, user id

- `id_map`
	- uid		Integer, user id
	- [token]	hex string, same meaning as above

- `message_pool`
	- token		hex string, used for uniquely identify client
	- [Text]	store messages to be send to this token (encrypt later)

## API
--------------------------------------------------------------------------------
In the following json format, server-sent "code" are always of type Number.
Things like 400/422 are merely used to represent multiple possible values.
Also, this document may be out of date due to modification to the server code.
If found inconsistency, checkout the code in [Api.hs](src/Api.hs) and 
[JSON.hs](src/JSON.hs).

### REGISTER
(server respond as soon as possible)

**/register**

client:
```json
	{
		"username": "theusername(RSA)",
		"password": "md5'ed password(RSA)"
	}
```

server: store `(username, md5'ed password, token)` in database where 
token is generated from username and password. and respond.

```json
	{
		"code": 200,
		"msg": ""
	}
```	
on success,
```json
	{
		"code": 400/422,
		"msg": "bad request, or username already existed"
	}
```
invalid json/invalid field or username existed

--------------------------------------------------------------------------------

### LOGIN

(server respond as soon as possible)

**/login**

client:
```json
	{
		"username": "theusername(RSA)",
		"password": "md5'ed password(RSA)",
		"deviceName": "Nexus, Huawei, Xiaomi, Chromium...(RSA)"
	}
```
server: autheticate by querying database, return
```json
	{
		"code": 200,
		"token": "aes encrypted token(possibly base64'ed)",
		"msg": ""
	}
```
on success,
```json
	{
		"code": 400/422,
		"msg": "bad request or wrong username/pwd"
	}
```
invalid json or wrong username / pwd

then the server will add this token to a memory/physical database as
tuple: ~~`(uid, connection, token, devicename)`~~(see the datatype at end)
token is used to identify different devices owned by one account

the client should keep the token carefully and include it to communicate
with server afterwards.

--------------------------------------------------------------------------------

### POST

(server response as soon as possible)

**/post**

client:
```json
	{
		"token": "previously gotten token(RSA)",
		"data": "rsa encrypted data (RSA)"
	}
```
server: server add data to message queue which is to be send
```json
	{
		"code": 200,
		"msg": ""
	}
```
on success or
```json
	{
		"code": 400,
		"msg": "bad request, etc"
	}
```

--------------------------------------------------------------------------------
### SYNC

(server hold the connection, send back message whenever the message queue
is not empty.)

**/sync**

client:
```json
	{
		"token": "previously gotten token(RSA)"
	}
```

if the token validated:
server:
```json
	{
		"code": 200,
		"msg": ""
	}
```

then the connection is **not** closed.

server: when there's new message, send that to client
```json
	{
		"msg": "aes encrypted message, (AES and base64)",
		"msgid": "a random string indicating the msg id, (AES and base64)"
	}
```

then, if the client received the message, send back an ACK:

client:
```json
	{
		"msgid": "received msg id",
		"status": "ok"
	}
```
after the server gets this ack, it deleted the msg stored and proceed 
to the next one(wait when msg queue empty)
if the client don't managed to respond the desired json in time (currently
3 second) or the responded json fail to be decoded, connection will be closed
after the server send his last message to tell what happended.
If the json format is correct but the `msgid` is not the desired one (which
should be rare), the message will be resend.
[`Api.hs`](src/Api.hs#L214-L242)
		
~~It is the client's duty to start the ping frame (to keep the connection),~~
The server will fork a ping thread for every sync client (after bind this
websockets app to `wai`, it must do so or the warp reaper will terminate
this connection itself) and reconnect whenever it detects disconnect. 
Server will only delete the message after receiving the correct `msgid` or the
unsend message will be kept.  The message are now stored on database,
table `message_pool`.

--------------------------------------------------------------------------------

### LOGOUT

(server respond as soon as possible)

**/logout**

client:
```json
	{
		"token": "previously gotten token (RSA)",
		"(optional)reason": "reason, (maybe added later)"
	}
```
then server send back an ack.

server:
```json
	{
		"code": 200
	}
```
on success or
```json
	{
		"code": 400,
		"msg": "bad request"
	}
```
when bad formatted json or no such token then the server clears 
all the message related to that token.

--------------------------------------------------------------------------------

### PING

(server respond as soon as possible)

**/ping**

client:
```json
	{
		"token":"previously got token"
	}
```
then server send back **ack** when success
server:
```json
	{
		"code": 200,
		"msg": "ack"
	}
```
`400` or `422` when on error, which is explained in msg. (similar to above)

