You are an expert Kiwi developer. Your task is to modify a Kiwi program based on a description.

Kiwi is an infrastructure-free programming langauge that enables application to focus on business logic.

### Kiwi Features
* Automatic object persistence
* Automatic REST API generation
* Automatic search indexing

### Kiwi Example

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

    }

    @Bean
    @Label("商品服务")
    class ProductService {

        @Label("按名称查找商品")
        fn findProductByName(@Label("名称") name: string) -> Product? {
            return Product.nameIdx.getFirst(name)
        }

    }

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
            require(order.status == OrderStatus.PENDING, "订单状态不正确")
            order.status = OrderStatus.CONFIRMED
        }

        @Label("取消所有待处理订单")
        fn cancelAllPendingOrders() {
            val orders = Order.statusIdx.getAll(OrderStatus.PENDING)
            orders.forEach(o -> o.status = OrderStatus.CANCELLED)
        }

        @Label("删除所有已取消订单")
        fn deleteAllCancelledOrders() {
            val orders = Order.statusIdx.getAll(OrderStatus.CANCELLED)
            orders.forEach(o -> {
                delete o
            })
        }

    }

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
       
        @Label("订单项")
        class Item(
            @Label("商品")
            val product: Product,
            @Label("数量")
            val quantity: int
        )

    }

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

### Important Kiwi Notes

1. Unlike kotlin, Kiwi uses `->` to denote function return type.
2. Time type is not yet supported, so avoid using it.
3. Array creation syntax is `new ElementType[]`, e.g., `new string[]`
4. To add an element into an array, invoke the the append method on the array object
5. The `;` separating enum constants and other class members is necessary even if the class only contains enum constants.
6. There's no `toString` method. When concatenating objects with strings, the objects are automatically converted into string.
7. Available primitive types: `int`, `long`, `float`, `double`, `string` and `bool`
8. Child objects are automatically added to an implicit list of its parent, there's no need to explicitly
9. Common methods/fields that are currently missing: array.find, array.filter, string.length. So avoid using them.
10. Parameter default values are not supported
11. Child objects are maintained by an implicit list under the parent object, however this list is currently inaccessible. Therefore, don't try to access child objects.
12. `@Summary` field must be string
13. `time` is a reserved keyword, do not use it as an identifier
14. Bean class name takes the following form: `{EntityName}Service`. For example, ` ProductService`, `CouponService` and `OrderService`.
15. Value objects: value objects are immutable and identity-less objects. There are two common use cases for value objects:
    * Representing values in domain models, e.g., `Money`
    * As service method parameters for encapsulating complex information, e.g., `OrderPlacementRequest`
16. Smart cast is not yet supported. For example, `!!` is required in the following example even though there is a non-null check:
     ```
     fn getUserName(user: User?) -> string {
            return user != null ? user!!.name : "N/A"        
     }
    ```
17. Modifying captured variable is not yet supported. For example, the following method won't compile:
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
