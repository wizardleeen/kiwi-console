Your task is to generate a Kiwi program for based on a description.

Kiwi is an infrastructure-free programming langauge that enables application to focus on business logic.

### Kiwi's Key Features
* Automatic object persistence
* Automatic REST API generation
* Automatic search indexing

### Kiwi Code Example

@@ src/value/login_result.kiwi @@
package value

import domain.Customer

value class LoginResult(
    val successful: bool,
    val token: string?
)
@@ src/service/product_service.kiwi @@
package service

import domain.Product

@Bean
class ProductService {

    fn findProductByName(name: string) -> Product? {
        return Product.nameIdx.getFirst(name)
    }

}

@@ src/service/customer_service.kiwi @@
package service

import domain.Customer
import domain.Session
import value.LoginResult

@Bean
class CustomerService {

    fn login(email: string, password: string) -> LoginResult {
        val customer = Customer.emailIdx.getFirst(email)
        if (customer != null) {
            val session = Session(customer!!)
            return LoginResult(true, session.token)
        } else
            return LoginResult(false, null)
    }

    fn register(name: string, email: string, password: string) -> LoginResult {
        val c = Customer(name, email, password)
        var s = Session(c)
        return LoginResult(true, s.token)
    }

}

@@ src/service/init.kiwi @@
import domain.Customer
import domain.Product
import domain.Money
import domain.Currency
import domain.Category

@Bean
class Init {

    {
        Customer("leen", "leen@kiwi.com", "123456")
        Product("MacBook Pro", Money(14000, Currency.YUAN), 100, Category.ELECTRONICS)
    }

}
@@ src/service/order_service.kiwi @@
package service

import domain.Customer
import domain.Product
import domain.Coupon
import domain.Order
import domain.OrderStatus
import domain.OrderStatusAndTime

@Bean
class OrderService {

    static val PENDING_TIMEOUT = 15 * 60 * 1000

        fn placeOrder(@CurrentUser customer: Customer, products: Product[], coupon: Coupon?) -> Order {
        require(products.length > 0, "Missing products")
        var price = products[0].price
        for (i in 1...products.length) {
            price = price.add(products[i].price)
        }
        if (coupon != null) {
            price = price.sub(coupon!!.redeem())
        }
        val order = Order(customer, price)
        products.forEach(p -> {
            p.reduceStock(1)
            order.Item(p, 1)
        })
        return order
    }

    fn getCustomerOrders(@CurrentUser customer: Customer) -> Order[] {
        val orders = Order.customerIdx.getAll(customer)
        // Recent orders come first
        orders.sort((o1, o2) -> o1.createdAt < o2.createdAt ? 1 : o1.createdAt == o2.createdAt ? 0 : -1)
        return orders
    }

    fn confirmOrder(order: Order) {
        order.confirm()
    }

    fn cancelOrder(order: Order) {
        order.cancel()
    }

    fn cancelExpiredPendingOrders() {
        val orders = Order.statusAndCreatedAtIdx.query(
            OrderStatusAndTime(OrderStatus.PENDING, 0),
            OrderStatusAndTime(OrderStatus.PENDING, now() - PENDING_TIMEOUT)
        )
        orders.forEach(o -> o.cancel())
    }

    fn deleteAllCancelledOrders() {
        val orders = Order.statusAndCreatedAtIdx.query(
            OrderStatusAndTime(OrderStatus.CANCELLED, 0),
            OrderStatusAndTime(OrderStatus.CANCELLED, now())
        )
        orders.forEach(o -> {
            delete o
        })
    }

    fn deleteAllCustomerOrders(@CurrentUser customer: Customer) {
        val orders = Order.customerIdx.getAll(customer)
        orders.forEach(o -> {
            delete o
        })
    }

}

@@ src/service/token_validator.kiwi @@
package service

import domain.Session

@Bean
internal class TokenValidator: security.TokenValidator {

    fn validate(token: string) -> any? {
        val s = Session.tokenIdx.getFirst(token)
        if (s != null && s!!.isValid())
            return s!!.customer
        else
            return null
    }

}
@@ src/domain/category.kiwi @@
package domain

enum Category {
        ELECTRONICS,
        CLOTHING,
        OTHER
    ;
}

@@ src/domain/OrderStatusAndTime.kiwi @@
package domain

value class OrderStatusAndTime(val status: OrderStatus, val time: long)
@@ src/domain/order.kiwi @@
package domain

class Order(
    val customer: Customer,
    val price: Money
) {

    static val statusAndCreatedAtIdx = Index<OrderStatusAndTime, Order>(false, o -> OrderStatusAndTime(o.status, o.createdAt))

    static val customerIdx = Index<Customer, Order>(false, o -> o.customer)

    val createdAt = now()
    var status = OrderStatus.PENDING

    fn confirm() {
        require(status == OrderStatus.PENDING, "订单状态不允许确认")
        status = OrderStatus.CONFIRMED
    }

    fn cancel() {
        require(status == OrderStatus.PENDING, "订单状态不允许取消")
        status = OrderStatus.CANCELLED
        for (child in children) {
            if (child is Item item)
                item.product.restock(item.quantity)
        }
    }

    class Item(
        val product: Product,
        val quantity: int
    )

}
@@ src/domain/order_status.kiwi @@
package domain

enum OrderStatus {
        PENDING,
        CONFIRMED,
        CANCELLED,
    ;
}

@@ src/domain/customer.kiwi @@
package domain

class Customer(
    @Summary
        var name: string,
        val email: string,
        password: string
) {

    priv var passwordHash = secureHash(password, null)

    static val emailIdx = Index<string, Customer>(true, c -> c.email)

        fn checkPassword(password: string) -> bool {
        return passwordHash == secureHash(password, null)
    }

}

@@ src/domain/product.kiwi @@
package domain

class Product(
    @Summary
    var name: string,
    var price: Money,
    var stock: int,
    var category: Category
) {

    static val nameIdx = Index<string, Product>(true, p -> p.name)

    fn reduceStock(quantity: int) {
        require(stock >= quantity, "库存不足")
        stock -= quantity
    }

    fn restock(quantity: int) {
        require(quantity > 0, "补充数量必须大于0")
        stock += quantity
    }

}

@@ src/domain/session.kiwi @@
package domain

class Session(
    val customer: Customer
) {

    val token = uuid()

    var expiry = now() + 30l * 24 * 60 * 60 * 1000

    static val tokenIdx = Index<string, Session>(true, s -> s.token)

    fn isValid() -> bool {
        return expiry > now()
    }

}
@@ src/domain/coupon.kiwi @@
package domain

class Coupon(
    @Summary
        val title: string,
        val discount: Money,
        val expiry: long
) {

        var redeemed = false

        fn redeem() -> Money {
        require(now() > expiry, "优惠券已过期")
        require(redeemed, "优惠券已核销")
        redeemed = true
        return discount
    }

}
@@ src/domain/currency.kiwi @@
package domain

enum Currency(
        val rate: double
) {

        YUAN(7.2) {

                fn label() -> string {
            return "元"
        }

    },
        DOLLAR(1) {

                fn label() -> string {
            return "美元"
        }

    },
        POUND(0.75) {

                fn label() -> string {
            return "英镑"
        }

    },
;

        abstract fn label() -> string

}

@@ src/domain/money.kiwi @@
package domain

value class Money(
        val amount: double,
        val currency: Currency
) {

    @Summary
    priv val summary = amount + " " + currency.label()

        fn add(that: Money) -> Money {
        return Money(amount + that.getAmount(currency), currency)
    }

        fn sub(that: Money) -> Money {
        return Money(amount - that.getAmount(currency), currency)
    }

        fn getAmount(currency: Currency) -> double {
        return currency.rate / this.currency.rate * amount
    }

        fn times(n: int) -> Money {
        return Money(amount * n, currency)
    }

}



### Important Notes

1. Unlike Kotlin, Kiwi uses `->` to denote function return type.
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
    *Note:* This constraint only apply to bean methods, other method names don't have to be unique across classes.
17. Import asterisk is not supported (e.g., `import domain.*`).
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
21. Only direct fields are allowed for computing index keys.
    * The following example is invalid because it uses an indirect field `name` in index key computation:
    ```
    class Product(var name: string)
    
    class Order(val product: Product) {
        
        static productNameIdx = Index<name, Order>(false, o.product.name)
    
    }
    ```
22. Integration with external systems is not yet supported such as payments system or AI.
23. Be very careful with the `!!` operator, you have to be absolutely sure that the value is not null.

### Output Format:

You shall output the full content of all created source files. Each source file starts with the header line: @@ {file-path} @@, e.g. @@ src/product.kiwi @@, and followed by the file content.
*  The response must **only** contain the source code. You **must not** include conversational text, such as greetings, explanations.
*  **Do not** add markdown code fences around code, such as \`\`\`kiwi  ... \`\`\`
*  Put every top-level class in a separate file.

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
```

### Description For The New Application
{}
