# AIGC API

API for interacting with AI agents to generate Kiwi programs.

## Endpoints

All endpoints require a logged-in user and an `X-App-ID: 2` header. See the [Login API](https://github.com/kiwi-language/kiwi/blob/main/docs/rest/login_app_api.md).

### 1. Generate

Sends a prompt to an AI model to generate or modify a Kiwi program.

*   `POST /aigc/generate`
*   **Request Body:**

    | Field    | Type     | Description                                                      |
    |:---------|:---------|:-----------------------------------------------------------------|
    | `appId`  | `long`   | Application ID                                                   |
    | `prompt` | `string` | A prompt describing the program to create or the changes to make |

*   **Response:**

    | Field     | Type     | Description                           |
    |:----------|:---------|:--------------------------------------|
    | `code`    | `int`    | `0` for success, else error code      |
    | `message` | `string` | Error message when `code` is non-zero |

*   **Example:**
    ```http
    POST /aigc/generate
    Content-Type: application/json
    X-App-ID: 2
    
    {
      "appId": ...
      "prompt": "Create a todolist application"
    }
    ```
    * Response
    ```json
    {
      "code": 0
    }
    ```
*   *Note:* This is a long-running operation and may take 1-2 minutes to complete.