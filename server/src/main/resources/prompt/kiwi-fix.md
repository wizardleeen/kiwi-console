The generated code didn't compile. You need to modify the code according to the build error. 

Here is the current content of `src/main.kiwi`:

{}

Here is the build error:

{}

### Output format

The output consist of multiple hunks, each with the following format:

@@ operation start-line:end-line-inclusive @@
content

* operation: insert | delete | replace
* start-line: 1-based starting line number. For insert, this is the line BEFORE which the content will be inserted.
* end-line-inclusive: 1-based ending line number (inclusive). For insert, this is the same as start-ine.

Example:

    @@ insert 1:1 @@
    import org.metavm.api.Index
    @@ delete 2:5 @@
    @@ replace 10:11 @@
    class Product(
        var name: string 
    )

### Constraints

*   All changes must be made in `src/main.kiwi`, do not create new source files.
*   **DO NOT** output any conversational text, explanations, apologies, or introductory sentences like "Sure, here is the diff:". Your response must start *directly* with the `@@` of the first hunk.
*   **ONLY** output the raw diff content. Do not wrap it in markdown code blocks (e.g., \`\`\`diff).
*   Ensure context lines in the `@@ ... @@` hunk header are accurate.
*   Ensure the content lines are properly indented