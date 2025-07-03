# Login & Application Management API

## Overview

This API manages user authentication and application lifecycle.

## Endpoints

All endpoints except `/auth/login` and `/auth/register` require an `Authorization: Bearer {token}` header. Error responses use the [ErrorResponse](#errorresponse) schema.

### 1. Login

Authenticates a user.

*   `POST /auth/login`
*   **Request Body:**

| Field      | Type     | Description   |
|:-----------|:---------|:--------------|
| `userName` | `string` | User name     |
| `password` | `string` | Password      |

*   **Response Body:**

| Field      | Type     | Description                    |
|:-----------|:---------|:-------------------------------|
| `token`    | `string` | token for authentication       |

*   **Example:**
    ```http
    POST /auth/login
    Content-Type: application/json

    {
      "userName": "demo",
      "password": "123456"
    }
    ```
    * Response:
    ```json
    {
      "token": "{token}"
    }
    ```

### 2. Logout

Logs the current user out.

*   `POST /auth/logout`
*   **Response Body:** No content
*   **Example:**
    ```http
    POST /auth/logout
    Authorization: Bearer {token}
    ```

### 3. Register

Creates a new user account.

*   `POST /auth/register`
*   **Request Body:**

| Field      | Type     | Description                                |
|:-----------|:---------|:-------------------------------------------|
| `userName` | `string` | The desired user name for the new account. |
| `password` | `string` | The password for the new account.          |

*   **Response Body:** No content
*   **Example:**
    ```http
    POST /auth/register
    Content-Type: application/json

    {
      "userName": "newuser",
      "password": "newpassword123"
    }
    ```

### 4. Search Applications

Retrieves a paginated list of applications.

*   `POST /app/v2/search`
*   **Request Body:**

| Parameter        | Required | Default | Description                                                                                                                          |
|:-----------------|:---------|:--------|:-------------------------------------------------------------------------------------------------------------------------------------|
| `name`           | No       |         | Filter applications by name                                                                                                          |
| `page`           | Yes      | `1`     | Page number                                                                                                                          |
| `pageSize`       | Yes      | `20`    | Number of items per page                                                                                                             |
| `newlyChangedId` | No       |         | Newly created or updated application ID. Use for searches immediately after change to ensure inclusion despite potential system lag. |

*   **Response Body:** `SearchResult<Application>`
*   **Example:**
    ```http
    POST /app/v2/search
    Authorization: Bearer {token}
    Content-Type: application/json

    {
        "name": "demo"
    }
    ```
    * Response
    ```json
    {
      "items": [
        {
          "id": "{app-id}",
          "name": "demo",
          "ownerId": "{user-id}"
        }
      ],
      "total": 1
    }
    ```

### 5. Save Application

Creates a new application or updates an existing one.

*   `POST /app/v2`
*   **Request Body:** `Application`
    *   If `id` is `null` or omitted: Creates a new application.
    *   If `id` is provided: Updates the existing application with that ID.
    *   `ownerId` is not required in request.
*   **Response Body:** `string` (The application ID)
*   **Example:**
    ```http
    POST /app/v2
    Autorization: Bearer {token}
    Content-Type: application/json

    {
      "name": "shopping"
    }
    ```
    * Response
    ```json
    "{id}"
    ```

### 6. Retrieve Application

Retrieves an Application.

*   `GET /app/v2/{id}`
*   **Path Parameter:** `id` - The ID of the application to retrieve.
*   **Response Body:** `Application`
*   **Example:**
    ```http
    GET /app/v2/{id}
    Authorization: Bearer {token}
    ```
    * Response
    ```json
    {
      "id": "{id}",
      "name": "shopping",
      "ownerId": "{owner-id}"
    }
    ```

### 7. Delete Application

Deletes an application.

*   `DELETE /app/v2/{id}`
*   **Path Parameter:** `id` - The ID of the application to delete.
*   **Response Body:** No content
*   **Example:**
    ```http
    DELETE /app/v2/{id}
    Authorization: Bearer {token}
    ```

## Data Structures

### `ErrorResponse`
Represents an error response from the API.

| Field      | Type     | Description     |
|:-----------|:---------|:----------------|
| `code`     | `int`    | Error code      |
| `message`  | `string` | Error message   |

### `SearchResult<T>`
Represents a paginated list of items.

| Field   | Type      | Description                             |
|:--------|:----------|:----------------------------------------|
| `items` | `T[]`     | Array of items for the current page     |
| `total` | `long`    | Total number of items across all pages  |

### `Application`
Represents an application.

| Field      | Type     | Description          |
|:-----------|:---------|:---------------------|
| `id`       | `string` | Application ID       |
| `name`     | `string` | Application          |
| `ownerId`  | `string` | Application owner ID |
