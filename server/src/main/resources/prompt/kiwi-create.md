Your task is to generate a Kiwi program for based on a description. 

Kiwi is an infrastructure-free programming langauge that enables application to focus on business logic.

Here is a list of Kiwi's key features:
* Automatic object persistence
* Automatic REST API generation
* Automatic search indexing

Here is an code example:

    import org.metavm.api.Index

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

        @Label("购买")
        fn buy(customer: Customer, @Label("数量") quantity: int, @Label("优惠券") coupon: Coupon?) -> Order {
            reduceStock(quantity)
            var price = this.price.times(quantity)
            if (coupon != null)
                price = price.sub(coupon!!.redeem())
            val order = Order(customer, price)
            order.Item(this, quantity)
            return order
        }

    }

    @Bean
    class ProductService {
        
        fn findByName(name: string) -> Product? {
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

        fn add(@Label("待加金额") that: Money) -> Money {
            return Money(amount + that.getAmount(currency), currency)
        }

        fn sub(that: Money) -> Money {
            return Money(amount - that.getAmount(currency), currency)
        }

        fn getAmount(@Label("目标币种") currency: Currency) -> double {
            return currency.rate / this.currency.rate * amount
        }

        fn times(n: int) -> Money {
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

            fn label() -> string {
                return "元"
            }

        },
        @Label("美元")
        DOLLAR(1) {

            fn label() -> string {
                return "美元"
            }

        },
        @Label("英镑")
        POUND(0.75) {

            fn label() -> string {
                return "英镑"
            }

        },
    ;

        abstract fn label() -> string

        fn getRate() -> double {
            return rate
        }

    }

    @Bean
    @Label("订单服务")
    class OrderService {

        @Label("下单")
        fn placeOrder(customer: Customer, @Label("商品列表") products: Product[]) -> Order {
            require(products.length > 0, "Missing products")
            val price = products[0].price
            var i = 1
            while (i < products.length) {
                price = price.add(products[i].price)
                i++
            }
            val order = Order(customer, price)
            products.forEach(p -> {
                p.reduceStock(1)
                order.Item(p, 1)
            })
            return order
        }

        @Label("确认订单")
        fn confirmOrder(order: Order) {
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

        val createdAt = now() 
        var status = OrderStatus.PENDING
       
        @Label("订单项")
        class Item(
            @Label("商品")
            val product: Product,
            @Label("数量")
            val quantity: int
        )

    }

    enum OrderStatus {
        PENDING,
        CONFIRMED,
        CANCELLED,
        ;
    }

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

    @Bean
    class CustomerService {
    
        fn login(email: string, password: string) -> Customer? {
            val customer = Customer.emailIdx.getFirst(email)
            return customer != null && customer!!.checkPassword(password) ? customer : null
        }
    
    }

Important Notes:

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

Output Format:

The generated program shall be contained in a single source file and you shall output that source file in plain text.
Your output must ONLY contain the code. ABSOLUTELY NOTHING ELSE. No explanation, no markdown tags.
The first line shall be a line comment for application name, e.g., // Shopping, other than that, the code shall contain no comment.

Here is the description for the program to be generated:
{}