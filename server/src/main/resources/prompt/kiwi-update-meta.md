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
2. Unlike Kotlin, `if-else` constructs can not be used as expressions. Use conditional expression instead.
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
15. Tuples are not supported.
    For example, the following code won't compile:
    ```
    var userAndTaskStatus = (user, task.status)
    ```
    To fix the problem, you need to create a value class to represent the pair:
    ```
    value class UserAndTaskStatus(val user: User, val taskStatus: TaskStatus)

    var userAndTaskStatus = UserAndTaskStatus(user, task.status)
    ```
16. Do not introduce duplicate method names across beans because bean method names are used as API names.
    For example the following program is problematic:
    ```
    @Bean
    class ProductionTaskService {
       fn assignTask(task: ProductionTask, employee: Employee) {
          task.assignTo(employee)
       }
    }

    @Bean
    class InspectionTaskService {
       fn assignTask(task: InspectionTask, employee: Employee) {
          task.assignTo(employee)
       }
    }
    ```
    To fix the issue, you need to use different method names:

    ```
    @Bean
    class ProductionTaskService {
       fn assignProductionTask(task: ProductionTask, employee: Employee) {
          task.assignTo(employee)
       }
    }

    @Bean
    class InspectionTaskService {
       fn assignInspectionTask(task: InspectionTask, employee: Employee) {
          task.assignTo(employee)
       }
    }
    ```
17. Import asterisk is not supported (e.g., `import domain.*`).   *Note:* This constraint only apply to bean methods, other method names don't have to be unique across classes.
18. Child objects are instances of nested classes. The syntax to create a child object is: {parent-expression}.{child-class-name}({argument-list})
    * Nested class example:
    ```
    class Order(val totalPrice: double) {
    
        class Item(val product: Product, val quantity: int)
                
    }
    ```
    * Child creation example:
    ```
    order.Item(product, 1)
    ```
19. Use `as` to perform type cast. for example: `var i = l as int`
20. Avoid using the 'Id' suffix in field names and parameter names.
    * Bad Code:
    ```
    class Employee(
        var employeeId: string
    ) {
    
        fn updateEmployeeId(employeeId: string) {
            this.employeeId = employeeId
        }

    }
    ```
    * Recommended Code:
    ```
    class Employee(
        var employeeNumber: string
    ) {
    
        fn updateEmployeeNumber(employeeNumber: string) {
            this.employeeNumber = employeeNumber
        }

    }
    ```
21. Integration with external systems is not yet supported such as payments system or AI.


### Data Migration

When you add new fields or change types of existing fields, you need to define migration functions so that Kiwi runtime
knows how to migrate data to the new model. Note that you **must not** throw exceptions in migration functions.

**Example 1:**

This example involves field addition and changing of  field type:


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

**Example 2:**

This example involving introducing child objects and moving some fields to the child objects. Suppose we introduce
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


**Example 3:**

This example involves creating a new class and a field referencing the new class at the same time.
The key challenge is that to successfully migrate the new field, a object of the new class is required. However, as it is a new class, there must be no existing instances.
The solution is to try to find an existing instance of the new class and if not found create a new one in the migration function of the new field.

Before

    class ProductionTask(val plannedAmount: double, var finishedAmount: double)

After

    class ProductionLine(var name: string) {
        static allIdx = Index<bool, ProductionLine>(false, p -> true)
    }

    class ProductionTask(val plannedAmount: double, val productionLine: ProductionLine) {
        
        var finishedAmount: double

        fn __productionLine__() -> ProductionLine {
            val existingPl = ProductionLine.allIdx.getFirst(true)
            return existingPl == null ? ProductionLine("N/A") : existingPl!!
        }

    }

Note: The technique used in this example is also applicable to other situations where you need an instance of a class in a migration function but you are not sure if the instance exists.

Here is the description of how the user want to modify the program:

{}

Here is current source files:

{}

### Output Format:

You shall output the full content of all changed source files. Each source file starts with the header line: @@ {file-path} @@, e.g. @@ src/product.kiwi @@, and followed by the file content.
*  The response must **only** contain the source code. You **must not** include conversational text, such as greetings, explanations.
*  **Do not** add markdown code fences around code, such as \`\`\`kiwi  ... \`\`\`
*  Put every top-level class in a separate file.
*  Prefix the file path with `--` to indicate removal. You shall omit content in this case.

**Example:**
```
@@ src/domain/product.kiwi @@
package domain

class Product(
    var name: string, 
    var price: double, 
    var stock: int
) {

    fn reduceStocK(quantity: int) {
        require(stock >= quantity, "库存不足")
    }

}
@@ src/domain/order.kiwi @@
package domain

class Order(
    val totalPrice
) {

    class Item(val product: Product, val quantity: int)

}
@@ src/service/order_service.kiwi @@
package service

import domain.Product
import domain.Order

@Bean
class OrderService {
    
    fn placeOrder(product: Product) -> Order {
        val order = Order(product.price
        order.Item(product, 1)
        return order
    }
    
}
@@ --src/domain/coupon.kiwi @@
```
