Your task is to generate a Kiwi program for based on a description. 

Kiwi is an infrastructure-free programming langauge that enables application to focus on business logic.

Here is a list of Kiwi's key features:
* Automatic object persistence
* Automatic REST API generation
* Automatic search indexing

Here is an code example:

{example}

Important Notes:

1. Unlike kotlin, Kiwi uses `->` to denote function return type.
2. Array creation syntax is `new ElementType[]`, e.g., `new string[]`
3. To add an element into an array, invoke the the `append` method on the array object
4. There's no `toString` method. When concatenating objects with strings, the objects are automatically converted into string.
5. Available primitive types: `int`, `long`, `float`, `double`, `string` and `bool`
6. Parameter default values are not supported
7. `@Summary` field must be string
8. Value objects: value objects are immutable and identity-less objects. There are two common use cases for value objects:
    * Representing values in domain models, e.g., `Money`
    * As service method parameters for encapsulating complex information, e.g., `OrderPlacementRequest`
9. Smart cast is not yet supported. For example, `!!` is required in the following example even though there is a non-null check:
    ```
    fn getUserName(user: User?) -> string {
        return user != null ? user!!.name : "N/A"        
    }
   ```
10. Modifying captured variable is not yet supported. For example, the following method won't compile:
    ```
    fn sum(values: int[]) -> int {
        var sum = 0
        values.forEach(v -> sum += v)
        return sum
    }
    ```
    To resolve the issue, you can use `for-in` loop to avoid variable capturing:
    ```
    fn sum(values: int[]) -> int {
        var sum = 0
        for (v in values) sum += v
        return sum
    }
    ```

Output Format:

The generated program shall be contained in a single source file and you shall output that source file in plain text.
Your output must ONLY contain the code. ABSOLUTELY NOTHING ELSE. No explanation, no markdown tags.
The first line shall be a line comment for application name, e.g., // Shopping, other than that, the code shall contain no comment.

Here is the description for the program to be generated:
{}