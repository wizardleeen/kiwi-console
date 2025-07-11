Your task is to generate a Kiwi program for based on a description.

Kiwi is an infrastructure-free programming langauge that enables application to focus on business logic.

### Kiwi's Key Features
* Automatic object persistence
* Automatic REST API generation
* Automatic search indexing

### Kiwi Code Example

@@ src/service/product_service.kiwi @@
package service

import domain.Product

@Bean
@Label("商品服务")
class ProductService {

    @Label("按名称查找商品")
    fn findProductByName(@Label("名称") name: string) -> Product? {
        return Product.nameIdx.getFirst(name)
    }

}

@@ src/service/customer_service.kiwi @@
package service

import domain.Customer

@Bean
@Label("客户服务")
class CustomerService {

    @Label("登录")
    fn login(@Label("邮箱") email: string, @Label("密码") password: string) -> Customer? {
        val customer = Customer.emailIdx.getFirst(email)
        return customer != null && customer!!.checkPassword(password) ? customer : null
    }

    @Label("注册")
    fn register(@Label("用户名") name: string, @Label("邮箱") email: string, @Label("密码") password: string) -> Customer {
        return Customer(name, email, password)
    }

}

@@ src/service/order_service.kiwi @@
package service

import domain.Customer
import domain.Product
import domain.Coupon
import domain.Order
import domain.OrderStatus

@Bean
@Label("订单服务")
class OrderService {

    @Label("下单")
    fn placeOrder(@Label("客户") customer: Customer, @Label("商品列表") products: Product[], @Label("优惠券") coupon: Coupon?) -> Order {
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

    @Label("确认订单")
    fn confirmOrder(@Label("订单") order: Order) {
        order.confirm()
    }

    @Label("取消订单")
    fn confirmOrder(@Label("订单") order: Order) {
        order.cancel()
    }

    @Label("取消所有待处理订单")
    fn cancelAllPendingOrders() {
        val orders = Order.statusIdx.getAll(OrderStatus.PENDING)
        orders.forEach(o -> o.cancel())
    }

    @Label("删除所有已取消订单")
    fn deleteAllCancelledOrders() {
        val orders = Order.statusIdx.getAll(OrderStatus.CANCELLED)
        orders.forEach(o -> {
            delete o
        })
    }

}

@@ src/domain/category.kiwi @@
package domain

@Label("类目")
enum Category {
    @Label("电子产品")
    ELECTRONICS,
    @Label("服装")
    CLOTHING,
    @Label("其他")
    OTHER
    ;
}

@@ src/domain/order.kiwi @@
package domain

@Label("订单")
class Order(
    @Label("客户")
    val customer: Customer,
    @Label("总价")
    val price: Money
) {

    static val statusIdx = Index<OrderStatus, Order>(false, o -> o.status)

    @Label("创建时间")
    val createdAt = now()
    @Label("状态")
    var status = OrderStatus.PENDING

    @Label("确认")
    fn confirm() {
        require(status == OrderStatus.PENDING, "订单状态不允许确认")
        status = OrderStatus.CONFIRMED
    }

    @Label("取消")
    fn cancel() {
        require(status == OrderStatus.PENDING, "订单状态不允许取消")
        status = OrderStatus.CANCELLED
        for (child in children) {
            if (child is Item item)
                item.product.restock(item.quantity)
        }
    }

    @Label("订单项")
    class Item(
        @Label("商品")
        val product: Product,
        @Label("数量")
        val quantity: int
    )

}

@@ src/domain/order_status.kiwi @@
package domain

@Label("订单状态")
enum OrderStatus {
    @Label("待确认")
    PENDING,
    @Label("已确认")
    CONFIRMED,
    @Label("已取消")
    CANCELLED,
    ;
}

@@ src/domain/customer.kiwi @@
package domain

@Label("客户")
class Customer(
    @Summary
    @Label("名称")
    var name: string,
    @Label("邮箱")
    val email: string,
    @Label("密码")
    password: string
) {

    priv var passwordHash = secureHash(password, null)

    static val emailIdx = Index<string, Customer>(true, c -> c.email)

    @Label("校验密码")
    fn checkPassword(@Label("密码") password: string) -> bool {
        return passwordHash == secureHash(password, null)
    }

}

@@ src/domain/product.kiwi @@
package domain

@Label("商品")
class Product(
    @Summary
    @Label("名称")
    var name: string,
    @Label("价格")
    var price: Money,
    @Label("库存")
    var stock: int,
    @Label("类目")
    var category: Category
) {

    static val nameIdx = Index<string, Product>(true, p -> p.name)

    @Label("扣减库存")
    fn reduceStock(@Label("数量") quantity: int) {
        require(stock >= quantity, "库存不足")
        stock -= quantity
    }

    @Label("补充库存")
    fn restock(@Label("数量") quantity: int) {
        require(quantity > 0, "补充数量必须大于0")
        stock += quantity
    }

}

@@ src/domain/coupon.kiwi @@
package domain

@Label("优惠券")
class Coupon(
    @Summary
    @Label("标题")
    val title: string,
    @Label("折扣")
    val discount: Money,
    @Label("过期时间")
    val expiry: long
) {

    @Label("已核销")
    var redeemed = false

    @Label("核销")
    fn redeem() -> Money {
        require(now() > expiry, "优惠券已过期")
        require(redeemed, "优惠券已核销")
        redeemed = true
        return discount
    }

}
@@ src/domain/currency.kiwi @@
package domain

@Label("币种")
enum Currency(
    @Label("汇率")
    val rate: double
) {

    @Label("人民币")
    YUAN(7.2) {

        @Label("标签")
        fn label() -> string {
            return "元"
        }

    },
    @Label("美元")
    DOLLAR(1) {

        @Label("标签")
        fn label() -> string {
            return "美元"
        }

    },
    @Label("英镑")
    POUND(0.75) {

        @Label("标签")
        fn label() -> string {
            return "英镑"
        }

    },
;

    @Label("标签")
    abstract fn label() -> string

}

@@ src/domain/money.kiwi @@
package domain

@Label("金额")
value class Money(
    @Label("数额")
    val amount: double,
    @Label("币种")
    val currency: Currency
) {

    @Summary
    priv val summary = amount + " " + currency.label()

    @Label("加")
    fn add(@Label("待加金额") that: Money) -> Money {
        return Money(amount + that.getAmount(currency), currency)
    }

    @Label("减")
    fn sub(@Label("待减金额") that: Money) -> Money {
        return Money(amount - that.getAmount(currency), currency)
    }

    @Label("汇率转化")
    fn getAmount(@Label("目标币种") currency: Currency) -> double {
        return currency.rate / this.currency.rate * amount
    }

    @Label("乘")
    fn times(@Label("倍数") n: int) -> Money {
        return Money(amount * n, currency)
    }

}



### Important Notes

1. Unlike Kotlin, Kiwi uses `->` to denote function return type.
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
19. Integration with external systems is not yet supported such as payments system or AI.

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
