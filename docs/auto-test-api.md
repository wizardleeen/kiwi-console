# Auto Test API

API for interacting with an AI agent to perform automated testing of a generated application.

## Endpoints

All endpoints require an `Authorization: Bearer {token}` header. Error responses use a standard error schema.

### 1. Get Next Action

Retrieves the next action from the AI agent for an iterative auto-test session. The client executes the returned action, provides the results (e.g., a screenshot) in the next request, and continues this loop until the test concludes with a `PASSED` or `FAILED` status.

*   `POST /auto-test/next-action`
*   **Request Body:** `AutoTestStepRequest`

| Field            | Type       | Description                                                                                             |
|:-----------------|:-----------|:--------------------------------------------------------------------------------------------------------|
| `exchangeId`     | `string`   | The ID of the `Exchange` session this test is for.                                                      |
| `attachmentUrls` | `string[]` | (Optional) A list of URLs for attachments (e.g., screenshots of the current application state) to provide context for the next step. |

*   **Response Body:** `AutoTestAction`

*   **Example:**
    *   **Request:**
    ```http
    POST /auto-test/next-action
    Content-Type: application/json
    Authorization: Bearer {token}
    
    {
        "exchangeId": "e9z8y7x6",
        "attachmentUrls": [ "/uploads/uuid3-test-step1.png" ]
    }
    ```
    *   **Response (Next Step):** The test is in progress. The AI has determined the next command to execute.
    ```json
    {
      "type": "STEP",
      "desc": "Fill in the username and password, then click the login button.",
      "content": "type(selector='#username', text='testuser')\ntype(selector='#password', text='password123')\nclick(selector='#login-button')"
    }
    ```
    *   **Response (Test Passed):** The test has concluded because the application has met the testing goal.
    ```json
    {
      "type": "PASSED",
      "desc": "User successfully logged in. Test complete.",
      "content": "The test scenario has been successfully verified."
    }
    ```
    *   **Response (Test Failed):** The test has concluded because the application did not behave as expected and failed to meet the testing goal.
    ```json
    {
      "type": "FAILED",
      "desc": "Login failed with correct credentials. An unexpected error message was displayed.",
      "content": "Verification failed: After submitting valid credentials, expected redirection to '/dashboard', but an error message 'Invalid credentials' was shown instead."
    }
    ```

## Data Structures

### `AutoTestStepRequest`

Represents the context for a single step in an auto-test session, provided to the AI to determine the next action.

| Field            | Type       | Description                                                              |
|:-----------------|:-----------|:-------------------------------------------------------------------------|
| `exchangeId`     | `string`   | The ID of the `Exchange` session this test is for.                       |
| `attachmentUrls` | `string[]` | (Optional) A list of URLs for attachments (e.g., screenshots).           |

### `AutoTestAction`

Represents an action determined by the AI for the auto-test process. This can be an intermediate step or a final conclusion of the test.

| Field     | Type     | Description                                                                                                                      |
|:----------|:---------|:---------------------------------------------------------------------------------------------------------------------------------|
| `type`    | `string` | The type of action, indicating the state of the test:<br><ul><li>`STEP`: The test is ongoing; `content` contains the next command to execute.</li><li>`PASSED`: A terminal state indicating the application has passed the test.</li><li>`FAILED`: A terminal state indicating the application does not meet the test goal.</li></ul> |
| `desc`    | `string` | A human-readable description of the action or the final result of the test.                                                      |
| `content` | `string` | The machine-readable content or payload. For `STEP`, this is typically a command or script. For `PASSED` or `FAILED`, this is a final summary message. |