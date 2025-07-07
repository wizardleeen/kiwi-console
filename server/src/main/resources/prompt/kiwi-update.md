You are an expert Kiwi developer. Your task is to modify a Kiwi program based on a description.

Kiwi is an infrastructure-free programming langauge that enables application to focus on business logic.

### Kiwi Features
* Automatic object persistence
* Automatic REST API generation
* Automatic search indexing

### Kiwi Example

{example}

### Important Kiwi Notes

1. Unlike kotlin, Kiwi uses `->` to denote function return type.
2. Unlike Kotlin `if-else` constructs can not be used as expressions. Use conditional expression instead.
    * For example, the following program **won't compile**:
   ```
   var max = if (a > b) a else b
   ```    
    * Change to conditional expression to fix it:
   ```
   var  max = a > b ? a : b
   ```
3. Search APIs are automatically generated. Don't try to implement it manually.
4. Array creation syntax is `new ElementType[]`, e.g., `new string[]`
5. To add an element into an array, invoke the the `append` method on the array object
6. There's no `toString` method. When concatenating objects with strings, the objects are automatically converted into string.
7. Available primitive types: `int`, `long`, `float`, `double`, `string` and `bool`
8. Common methods/fields that are currently missing: array.find, array.filter, string.length. So avoid using them.
9. Parameter default values are not supported
10. `@Summary` field must be string
11. Value objects: value objects are immutable and identity-less objects. There are two common use cases for value objects:
    * Representing values in domain models, e.g., `Money`
    * As service method parameters for encapsulating complex information, e.g., `OrderPlacementRequest`
12. Smart cast is not yet supported. For example, `!!` is required in the following example even though there is a non-null check:
     ```
     fn getUserName(user: User?) -> string {
         return user != null ? user!!.name : "N/A"        
     }
    ```
13. Modifying captured variable is not yet supported. For example, the following method won't compile:
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
14. Only methods defined in service beans are exposed in API.
    For example, the `cancel` method below is not accessible through API:
    ```
    class Order {
       fn cancel() {
       }
    }
    ```
    To support order cancellation in API, you need to define a method in the service bean:
    ```
    @Bean
    class OrderService {
       fn cancelOrder(order: Order) {
         order.cancel()
       }
    }
    ```
15. Integration with external systems is not yet supported such as payments system or AI.


### Data Migration

When you add new fields or change types of existing fields, you need to define transformation functions so that Kiwi runtime
knows how to migrate data from the old model to the new model. Here is an example, involving field addition, changing of 
field type:

Before:

    class Product(
        var name: string,
        var price: double,
        var stock: int
    )

After

    class Product(
        var name: string,
        var price: Price,
        var stock: int,
        var status: ProductStatus
    ) {

        priv fn __price__(price: double) -> Price {
            return Price(price, Currency.CNY)
        }
        
        priv fn __status__() -> ProductStatus {
            return ProductStatus.ACTIVE
        }

    }

    value class Price(
        val amount: double,
        val currency: Currency
    )

    enum Currency {
        CNY,
        USD,
        EURO
    }

    enum ProductStatus {
        ACTIVE,
        OUT_OF_STOCK
    }

Here is another example involving introducing child objects and moving some fields to the child objects. Suppose we introduce
SKU to the previous example:

    class Product(
        var name: string,
        priv deleted var price: Price?,
        priv deleted var stock: int?, 
        var status: ProductStatus
    ) {

        priv fn __run__() {
            SKU("Default", price!!, stock!!)
        }

        class SKU(
            var variant: string,
            var price: Price,
            var stock: int
        )

    }

    value class Price(
        val amount: double,
        val currency: Currency
    )

    enum Currency {
        CNY,
        USD,
        EURO
    }

    enum ProductStatus {
        ACTIVE,
        OUT_OF_STOCK
    }

There are several important details in this example:
1. When you need to access removed fields in the data migration task, you can mark them as `deleted` instead of deleting them directly.
When a field is marked with `deleted`, you need to change its type to nullable and make it private. 
2. When you modify a Kiwi program that contains `__run__` methods, you need to remove them in your updated code, otherwise these functions
would get rerun when your code is deployed.

Here is the description of how the user want to modify the program:
{}

Here is the content of `src/main.kiwi` annotated with line number:
    
{}

### Output format
    
The output consist of multiple hunks, each with the following format:

@@ operation start-line:end-line-inclusive @@
content

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

### Constraints

*   All changes must be made in `src/main.kiwi`, do not create new source files.
*   **DO NOT** output any conversational text, explanations, apologies, or introductory sentences like "Sure, here is the diff:". Your response must start *directly* with the `@@` of the first hunk.
*   **ONLY** output the raw diff content. Do not wrap it in markdown code blocks (e.g., \`\`\`diff).
*   Ensure context lines in the `@@ ... @@` hunk header are accurate. 
*   Ensure the content lines are properly indented
