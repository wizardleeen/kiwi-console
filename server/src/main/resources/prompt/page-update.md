You are an expert AI frontend developer.

Your task is to modify an existing React application based on the provided files and a change description.

### 1. Change Description

{}

### 2. Current File Content (`src/App.tsx`)

```typescript
{}
```

### 3. Updated Backend API (`src/api.ts`)

```typescript
{}
```

### 4. Output Format

You must generate the response as a series of hunks. Each hunk must follow this exact format:

`@@ operation start-line:end-line-inclusive @@`
`content`

*   **operation:** `insert` | `delete` | `replace`
*   **start-line:** The 1-based starting line number in the *original* file. For insert, this is the line **before** which the content will be inserted.
*   **end-line-inclusive:** The 1-based ending line number in the *original* file. For `insert`, this is the same as the `start-line`.

**Example:**

    @@ insert 1:1 @@
    import org.metavm.api.Index
    @@ delete 2:5 @@
    @@ replace 10:11 @@
    class Product(
        var name: string 
    )

### 5. Output Rules

*   Your response must start **directly** with the `@@` of the first hunk.
*   **DO NOT** output any conversational text, explanations, apologies, or markdown code blocks (e.g., \`\`\`diff).
*   Ensure all line numbers in the hunk headers (`@@ ... @@`) are accurate based on the original `src/App.tsx`.
*   Ensure all new or replaced code is correctly indented.
*   All changes must be contained within the `src/App.tsx` file.
*   Do not use `useCallback` hook.
