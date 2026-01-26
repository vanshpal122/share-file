# Share File

## 🚀 Project Overview
This is a file transfer system built using **Spring Boot** that allows users to securely send and receive files over a local area network (LAN). The project is currently under development as part of my learning in backend development with Spring Boot.

## 🎯 Features (Planned)
- File upload/download over local network
- User authentication (basic or token-based)
- File transfer progress tracking
- Logging and error handling
- Minimal web UI to interact with the system

---

# File Transfer Protocol - Phases

This document outlines the phases of a WebSocket-based file transfer system, from connection establishment to the actual file transfer process, including how to handle resuming a transfer in case of interruptions.

## 🎯 Phases

<details>
<summary>1. WebSocket Connection Phase</summary>

* **Receiver connects first**
  Example: `ws://server/ws?userId=R1`
  Server Handshake Interceptor runs:

  ```java
  String deviceId = request.getParameter("userId");
  accessor.setUser(() -> userId);
  ```

  Server stores:

  | WS Session | Principal |
  | ---------- | --------- |
  | Session#78 | R1        |

* **Receiver subscribes**:

  * `/user/queue/negotiation`
  * `/user/queue/ack`

* **Sender connects**
  Example: `ws://server/ws?userId=S1`
  Server stores:

  | WS Session | Principal |
  | ---------- | --------- |
  | Session#99 | S1        |

* **Sender subscribes**:

  * `/user/queue/negotiation`
  * `/user/queue/ack`
  * `/topic/getReceivers`

</details>

<details>
<summary>2. Discovery Phase</summary>

* **Receiver announces presence**
  SEND → `/app/broadcast`
  Body:

  ```json
  {
    "id": "R1",
    "deviceName": "Vansh Laptop"
  }
  ```

* **Controller sends to all senders**:

  ```java
  @SendTo("/topic/getReceivers")
  ```

* **All senders receive list of active receivers**.

</details>

<details>
<summary>3. Negotiation Phase</summary>

* **Step 1 — Sender proposes files**
  Sender sends metadata:
  SEND → `/app/sendTo/R1`
  Body:

  ```json
  {
    "senderId": "S1",
    "files": ["fileA", "fileB", "fileC"]
  }
  ```

* **Server delivers metadata to R1**:

  ```java
  convertAndSendToUser("R1", "/queue/negotiation", metadata);
  ```

* **Step 2 — Receiver selects files**
  Receiver replies:
  SEND → `/app/sendAck/S1`
  Body:

  ```json
  {
    "acceptedFiles": ["fileA", "fileC"]
  }
  ```

* **Server routes reply to S1 privately**.

</details>

<details>
<summary>4. Session Creation (Trigger Point)</summary>

* **Trigger**: Receiver ACK arrives

  * Server creates respective sessions with tokens:

  ```java
  UUID sessionId = UUID.randomUUID();
  createSession(sessionId, senderId = "S1", receiverId = "R1", files = ["fileA", "fileC"]);
  String senderToken = generateToken();
  String receiverToken = generateToken();
  ```
  
* **Server sends session info to both Sender and Receiver**:

  ```java
  convertAndSendToUser(S1, "/queue/session", sessionWithTokenContainer.senderSession());
  convertAndSendToUser("R1", "/queue/session", sessionWithTokenContainer.receiverSession());
  ```

* **WebSocket signaling job is done**.

</details>

<details>
<summary>5. File Transfer Phase (REST)</summary>

* WebSocket connection can stay or drop — doesn't matter now.

* **Sender uploads chunks**  
  POST → `/uploadChunk`  
  Headers:
  - `sessionId`  
  - **Authorization Token** (This token is required and will be validated by the `TokenFilter`)

  Body:
  ```json
  {
    "fileId": "fileA",
    "chunkIndex": 5,
    "totalChunks": 10,
    "chunkHash": "hash_value",
    "data": "chunk_data"
  }


* **Server stores chunk**:
  `/storage/sessionId/fileId/chunk_5`

* **Server marks chunk as uploaded** in DB:

  ```sql
  chunk_uploaded = true
  ```

* **Receiver downloads chunks** (pull model)
  GET → `/downloadChunk?sessionId=X&fileId=Y&chunkIndex=5` with its token which will also be verified by Token Filter

* **Receiver verifies chunk hash**.

* **Receiver sends chunk acknowledgment**:
  POST → `/chunkAck`
  Body:

  ```json
  {
    "sessionId": "X",
    "fileId": "Y",
    "chunkIndex": 5
  }
  ```

* **Server marks chunk as downloaded** and deletes chunk file from storage.

</details>

<details>
<summary>6. Resume Case</summary>

* **If receiver goes offline**:
  The database state says:

  | chunk | uploaded | downloaded |
  | ----- | -------- | ---------- |
  | 1     | true     | true       |
  | 2     | true     | true       |
  | 3     | true     | false      |

* **Receiver reconnects** and requests remaining chunks:
  GET → `/remainingChunks?sessionId=X&fileId=Y`

* **Server returns the list of remaining chunks**:

  ```json
  [3]
  ```

* **Transfer resumes** from the last downloaded chunk.

</details>

---

## 🎯 Database

<details>
  <summary>Entity Relationships</summary>

### 1. **SessionAuth Entity**

Represents an authentication session linked to a specific `Session`.

| Field       | Type            | Description                                                         |
| ----------- | --------------- | ------------------------------------------------------------------- |
| `id`        | `Long`          | Primary key for `SessionAuth`.                                      |
| `session`   | `Session`       | A `Session` object. Many `SessionAuth` can belong to one `Session`. |
| `role`      | `Role`          | The role assigned to this session (e.g., Sender, Receiver).         |
| `tokenHash` | `String`        | Hashed value of the Authorization token.                            |
| `expiresAt` | `LocalDateTime` | Expiration date and time of the token.                              |
| `revoked`   | `boolean`       | Indicates whether the token is revoked.                             |

**Relationship:**

* A `SessionAuth` belongs to a **Single `Session`**.

---

### 2. **Session Entity**

Represents a file transfer session, including sender and receiver information.

| Field        | Type                 | Description                                                                                     |
| ------------ | -------------------- | ----------------------------------------------------------------------------------------------- |
| `sessionId`  | `String` (UUID)      | Unique identifier for each session.                                                             |
| `senderId`   | `String`             | The ID of the sender in the session.                                                            |
| `receiverId` | `String`             | The ID of the receiver in the session.                                                          |
| `files`      | `List<FileMetadata>` | List of files associated with the session. Each file is represented by a `FileMetadata` object. |

**Relationship:**

* A `Session` can have **Multiple `FileMetadata` entries**.
* A `Session` is associated with **One or More `SessionAuth`** entries (linked by `sessionId`).

---

### 3. **FileMetadata Entity**

Represents metadata for each file being transferred in a session.

| Field         | Type      | Description                                        |
| ------------- | --------- | -------------------------------------------------- |
| `id`          | `Long`    | Primary key for `FileMetadata` (not the `fileId`). |
| `fileId`      | `String`  | Logical file identifier.                           |
| `fileName`    | `String`  | Name of the file.                                  |
| `fileSize`    | `Long`    | Size of the file in bytes.                         |
| `mimeType`    | `String`  | MIME type of the file (e.g., `image/png`).         |
| `totalChunks` | `Long`    | Total number of chunks for this file.              |
| `hash`        | `String`  | Hash of the file.                                  |
| `session`     | `Session` | The `Session` associated with this file.           |

**Relationship:**

* Each `FileMetadata` belongs to **One `Session`**.
* A `Session` can have **Multiple `FileMetadata`** entries.

---

### 4. **FileChunk Entity**

Represents the chunks of a file that is being transferred in a session.

| Field        | Type           | Description                               |
| ------------ | -------------- | ----------------------------------------- |
| `id`         | `Long`         | Primary key for `FileChunk`.              |
| `chunkIndex` | `int`          | Index of the chunk (e.g., 0, 1, 2, etc.). |
| `uploaded`   | `boolean`      | Whether the chunk has been uploaded.      |
| `downloaded` | `boolean`      | Whether the chunk has been downloaded.    |
| `hash`       | `String`       | Hash of the chunk to verify integrity.    |
| `file`       | `FileMetadata` | The `FileMetadata` this chunk belongs to. |

**Relationship:**

* Each `FileChunk` belongs to **One `FileMetadata`**.
* A `FileMetadata` can have **Multiple `FileChunk` entries**.

---

## Entity Relationship Diagram

Diagram to visually represent the relationships between these entities:

```plaintext
   +-------------------+              +-------------------+
   |    SessionAuth    |              |      Session      |
   +-------------------+              +-------------------+
   | id (PK)           |              | sessionId (PK)    |
   | role              |              | senderId          |
   | tokenHash         |              | receiverId        |
   | expiresAt         |              |                   |
   | revoked           |              |                   |
   +-------------------+              +-------------------+
            |                              | 1..*
            | Many                         | Files
            v                              v
   +--------------------+         +--------------------+
   |  Session (1)       |         |   FileMetadata     |
   +--------------------+         +--------------------+
   | sessionId (PK)     | 1..* -> | id (PK)            |
   | senderId           |         | fileId             |
   | receiverId         |         | fileName           |
   +--------------------+         | fileSize           |
                                  | mimeType           |
                                  | totalChunks        |
                                  | hash               |
                                  | session (FK)       |
                                  +--------------------+
                                           |
                                           | 1..*
                                           v
                                   +--------------------+
                                   |    FileChunk       |
                                   +--------------------+
                                   | id (PK)            |
                                   | chunkIndex         |
                                   | uploaded           |
                                   | downloaded         |
                                   | hash               |
                                   | file (FK)          |
                                   +--------------------+
```

### Explanation of the Diagram:

1. **SessionAuth** has a many-to-one relationship with **Session**.
2. **Session** can have multiple **FileMetadata** objects (one for each file in the session).
3. **FileMetadata** has a one-to-many relationship with **FileChunk**, as each file can be split into multiple chunks.
4. Each **FileChunk** is linked to a single **FileMetadata**.

---
</details>

## 🛠 Tech Stack
- Java 24
- Spring Boot
- Spring Web
- Spring Data JPA
- H2 Database
- Spring Security (planned)
- Socket Programming
- Thymeleaf (considering for frontend)

## 📅 Timeline
| Phase | Status |
|-------|--------|
| Project Setup | ✅ Done |
| Basic File Upload/Download | ✅ Done |
| WebSocket STOMP | ✅ Done |
| Authentication Layer | ✅ Done |
| UI Integration | ⏳ Planned |
| Testing & Documentation | ⏳ Planned |

## 🧑‍💻 Author
- **Vansh Pal** – [vanshpal22](https://github.com/vanshpal122)

## 💡 Learning Goals
- Deepen understanding of Spring Boot and Java backend development
- Learn to handle file operations securely and efficiently
- Practice deploying and documenting real-world projects

---

> 🚧 *This project is currently in active development. Contributions, feedback, or suggestions are welcome!*
