# Generation API

API for interacting with AI agents to generate application.

## Endpoints

All endpoints require an `Authorization: Bearer {token}` header. Error responses use the [ErrorResponse](#errorresponse) schema.

### 1. Generate

Initiates an AI generation process. This endpoint establishes a long-lived connection using **Server-Sent Events (SSE)** to provide real-time progress updates to the client.

*   `POST /generate`
*   **Request Body:**

| Field                | Type       | Description                                                                                             |
|:---------------------|:-----------|:--------------------------------------------------------------------------------------------------------|
| `appId`              | `string`   | (Optional) ID of the application to modify. If omitted, a new application will be created.              |
| `prompt`             | `string`   | A prompt describing the program to create or the changes to make.                                       |
| `attachmentUrls`     | `string[]` | (Optional) A list of URLs for attachments (e.g., screenshots) to provide context to the AI.            |
| `skipPageGeneration` | `boolean`  | When `true`, the AI will not generate a web page for the application.                                   |

*   **SSE Event Stream:**
    The stream sends events named generation. The data payload for each event is a JSON-serialized Exchange object, reflecting the current state of the process.

*   **Example:**
    *   **Request:**
    ```http
    POST /generate
    Content-Type: application/json
    Authorization: Bearer {token}
    
    {
        "prompt": "Create a simple application with a user login page",
        "attachmentUrls": [ "/uploads/uuid1-screenshot1.png" ]
    }
    ```
    * **Response (SSE Stream):** The server will send a series of events. Below is an example of the data payload for each event in a successful generation flow. Since this is a new application, `first` is `true`.

    ```text/event-stream
    # Event 1: The generation request is received and the AI begins planning.
    # The overall status is "PLANNING".
    event: generation
    data: {
      "id": "e9z8y7x6",
      "appId": "{app-id}",
      "userId": "{user-id}",
      "first": true,
      "prompt": "Create a new application with a simple user login page.",
      "status": "PLANNING",
      "stages": []
    }

    # Event 2: The AI has planned the stages and starts generating the backend.
    # A new application ID is created. The overall status is "GENERATING".
    # The first stage (BACKEND) is added with a running attempt.
    event: generation
    data: {
      "id": "e9z8y7x6",
      "appId": "{app-id}",
      "userId": "{user-id}",
      "first": true,
      "prompt": "Create a new application with a simple user login page.",
      "status": "GENERATING",
      "stages": [
        {
          "id": "s1a2b3c4",
          "type": "BACKEND",
          "status": "GENERATING",
          "attempts": [
            { "id": "a1b2c3d4", "status": "RUNNING", "errorMessage": null }
          ]
        }
      ]
    }

    # Event 3: The backend stage is completed successfully.
    # The `managementURL` is now available.
    # The frontend stage is now added and begins generating.
    event: generation
    data: {
      "id": "e9z8y7x6",
      "appId": "{app-id}",
      "userId": "{user-id}",
      "first": true,
      "prompt": "Create a new application with a simple user login page.",
      "status": "GENERATING",
      "managementURL": "https://manage.xyz001.metavm.tech",
      "stages": [
        {
          "id": "s1a2b3c4",
          "type": "BACKEND",
          "status": "SUCCESSFUL",
          "attempts": [
            { "id": "a1b2c3d4", "status": "SUCCESSFUL", "errorMessage": null }
          ]
        },
        {
          "id": "s5d6e7f8",
          "type": "FRONTEND",
          "status": "GENERATING",
          "attempts": [
            { "id": "a5b6c7d8", "status": "RUNNING", "errorMessage": null }
          ]
        }
      ]
    }

    # Event 4: All stages are complete. The overall process is successful.
    # The final event sets the status to "SUCCESSFUL".
    # The connection is closed by the server after this event.
    event: generation
    data: {
      "id": "e9z8y7x6",
      "appId": "{app-id}",
      "userId": "{user-id}",
      "first": true,
      "prompt": "Create a new application with a simple user login page.",
      "status": "SUCCESSFUL",
      "productURL": "https://xyz001.metavm.tech",
      "managementURL": "https://manage.xyz001.metavm.tech",
      "stages": [
        {
          "id": "s1a2b3c4",
          "type": "BACKEND",
          "status": "SUCCESSFUL",
          "attempts": [
            { "id": "a1b2c3d4", "status": "SUCCESSFUL", "errorMessage": null }
          ]
        },
        {
          "id": "s5d6e7f8",
          "type": "FRONTEND",
          "status": "SUCCESSFUL",
          "attempts": [
            { "id": "a5b6c7d8", "status": "SUCCESSFUL", "errorMessage": null }
          ]
        }
      ]
    }
    ```
    > **Note on Failures:** If an attempt fails, its status will change to `FAILED` and an `errorMessage` will be provided. The AI may create a new attempt to retry the stage. If the entire process cannot be completed, the final event will have `status: "FAILED"`.

### 2. Upload Attachments

Uploads one or more files to be used as attachments in a generation request. Returns a list of URLs for the uploaded files.

*   `POST /generate/attachments`
*   **Supported File Types:** JPEG, PNG, GIF, PDF, MP4, TXT, HTML, JSON.
*   **Request:** `multipart/form-data`. The form field for the files must be named `files`.
*   **Response Body:** `MultiUploadResult`
*   **Example:**
    *   **Request:**
    ```http
    POST /generate/attachments
    Authorization: Bearer {token}
    Content-Type: multipart/form-data; boundary=----WebKitFormBoundary7MA4YWxkTrZu0gW

    ------WebKitFormBoundary7MA4YWxkTrZu0gW
    Content-Disposition: form-data; name="files"; filename="screenshot1.png"
    Content-Type: image/png

    (binary data)
    ------WebKitFormBoundary7MA4YWxkTrZu0gW
    Content-Disposition: form-data; name="files"; filename="screenshot2.png"
    Content-Type: image/png

    (binary data)
    ------WebKitFormBoundary7MA4YWxkTrZu0gW--
    ```
    * **Response:**
    ```json
    {
      "urls": [
        "/uploads/uuid1-screenshot1.png",
        "/uploads/uuid2-screenshot2.png"
      ]
    }
    ```

### 3. Reconnect

Reconnects to an existing SSE stream for an in-progress generation `Exchange`. This is useful if the client disconnects for any reason.

*  `GET /generate/reconnect?exchange-id={exchange-id}`
*  **Response:** SSE stream. See #[Generate](#1-generate) for details on the event stream.

### 4. History

Retrieves a paginated list of past `Exchange` interactions for a specific application.

*   `POST /generate/history`
*   **Request Body:**

| Parameter  | Type     | Required | Description                                  |
|:-----------|:---------|:---------|:---------------------------------------------|
| `appId`    | `string` | Yes      | The ID of the application to search within.  |
| `page`     | `int`    | No       | Page number for pagination. Defaults to `1`. |
| `pageSize` | `int`    | No       | Number of items per page. Defaults to `20`.  |

*   **Response Body:** `SearchResult<Exchange>`.
*   **Example:**
    *   **Request:**
    ```http
    POST /generate/history
    Authorization: Bearer {token}
    Content-Type: application/json

    {
      "appId": "{app-id}",
      "prompt": "user login",
      "page": 1,
      "pageSize": 10
    }
    ```
    * **Response:**
    ```json
    {
      "items": [
        {
          "id": "e8z7y6x5",
          "appId": "{app-id}",
          "userId": "{user-id}",
          "first": false,
          "prompt": "Add a feature to track user login history",
          "status": "SUCCESSFUL",
          "productURL": "https://xyz123.metavm.tech",
          "managementURL": "https://manage.xyz123.metavm.tech",
          "stages": [
            { "id": "c89b3dfa", "type": "BACKEND", "status": "SUCCESSFUL", "attempts": [] },
            { "id": "d90c4e0b", "type": "FRONTEND", "status": "SUCCESSFUL", "attempts": [] }
          ]
        }
      ],
      "total": 1
    }
    ```

### 5. Cancel Generation

Cancels an in-progress generation `Exchange`.

*   `POST /generate/cancel`
*   **Request Body:**

| Field        | Type     | Required | Description                         |
|:-------------|:---------|:---------|:------------------------------------|
| `exchangeId` | `string` | Yes      | The ID of the `Exchange` to cancel. |

*   **Response Body:** No content
*   **Example:**
    *   **Request:**
    ```http
    POST /generate/cancel
    Authorization: Bearer {token}
    Content-Type: application/json

    {
      "exchangeId": "e9z8y7x6"
    }
    ```

### 6. Retry Generation

Retries a failed `Exchange`.

*   `POST /generate/retry`
*   **Request Body:**

| Field        | Type     | Description                        |
|:-------------|:---------|:-----------------------------------|
| `exchangeId` | `string` | The ID of the `Exchange` to retry. |

*   **Response:** SSE stream. See #[Generate](#1-generate) for details on the event stream.
*   **Example:**
    *   **Request:**
    ```http
    POST /generate/retry
    Authorization: Bearer {token}
    Content-Type: application/json

    {
      "exchangeId": "e9z8y7x6"
    }
    ```

### 7. Revert Generation

Revert an `Exchange`. The exchange **must** be the last one for the application and it **must not** be running.

*   `POST /generate/revert`
*   **Request Body:**

| Field        | Type     | Description                         |
|:-------------|:---------|:------------------------------------|
| `exchangeId` | `string` | The ID of the `Exchange` to revert. |

*   **Example:**
    *   **Request:**
    ```http
    POST /generate/revert
    Authorization: Bearer {token}
    Content-Type: application/json

    {
      "exchangeId": "e9z8y7x6"
    }
    ```

## Data Structures

### `ErrorResponse`
Represents an error response from the API.

| Field      | Type     | Description     |
|:-----------|:---------|:----------------|
| `code`     | `int`    | Error code      |
| `message`  | `string` | Error message   |

### `MultiUploadResult`
Represents the result of a file upload operation.

| Field   | Type      | Description                          |
|:--------|:----------|:-------------------------------------|
| `urls`  | `string[]`| A list of URLs for the uploaded files|

### `SearchResult<T>`
Represents a paginated list of items.

| Field   | Type      | Description                             |
|:--------|:----------|:----------------------------------------|
| `items` | `T[]`     | Array of items for the current page     |
| `total` | `long`    | Total number of items across all pages  |

### `Exchange`
Represents a single interaction with the generation AI.

| Field          | Type       | Description                                                                                                                      |
|:---------------|:-----------|:---------------------------------------------------------------------------------------------------------------------------------|
| `id`           | `string`   | The unique identifier for the exchange.                                                                                          |
| `appId`        | `string`   | The ID of the application this exchange is associated with.                                                                      |
| `userId`       | `string`   | The ID of the user who initiated the exchange.                                                                                   |
| `first`        | `boolean`  | Indicates if this is the first exchange for the application (i.e., the one that created it).                                     |
| `prompt`       | `string`   | The initial user prompt that started the generation process.                                                                     |
| `status`       | `string`   | The overall status of the exchange. Possible values: `PLANNING`, `GENERATING`, `SUCCESSFUL`, `FAILED`, `CANCELLED`, `REVERTED`.  |
| `stages`       | `Stage[]`  | An ordered list of processing stages the AI undertakes to fulfill the request.                                                   |
| `errorMessage` | `string`   | An error message if the exchange failed. Will be `null` if successful.                                                           |
| `productURL`   | `string`   | The URL where the generated product can be accessed if the exchange was successful. Will be `null` otherwise.                    |
| `managementURL`| `string`   | The URL for managing the application. This is populated after the BACKEND stage completes successfully.                          |

### `Stage`
Represents a major step within an `Exchange`.

| Field      | Type         | Description                                                                                          |
|:-----------|:-------------|:-----------------------------------------------------------------------------------------------------|
| `id`       | `string`     | The unique identifier for the stage.                                                                 |
| `type`     | `string`     | Stage type. Possible values: `BACKEND`, `FRONTEND`                                                   |
| `status`   | `string`     | The current status of the stage. Possible values: `GENERATING`, `COMMITTING`, `SUCCESSFUL`, `FAILED` |
| `attempts` | `Attempt[]`  | A list of attempts made to complete this stage. A stage may be retried if an attempt fails.          |

### `Attempt`
Represents a single try at completing a `Stage`. If an attempt fails, a new one might be created to retry the stage.

| Field          | Type      | Description                                                                                                |
|:---------------|:----------|:-----------------------------------------------------------------------------------------------------------|
| `id`           | `string`  | The unique identifier for the attempt.                                                                     |
| `status`       | `string`  | The status of this specific attempt. Possible values: `RUNNING`, `SUCCESSFUL`, `FAILED`.                   |
| `errorMessage` | `string`  | An error message explaining why the attempt failed, if the `status` is `FAILED`. Will be `null` otherwise. |