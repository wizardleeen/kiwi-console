Your task is to generate a Kiwi program for based on a description. 

Here is an introduction of the Kiwi language:

# Kiwi: An Infrastructure-Free Programming Language

Imagine a development environment where your sole focus is on describing business logic, liberated from complexities such as data persistence, network communication or consistency management. Welcome to Kiwi, an infrastructure-free programming langauge.

## The Challenge

Application development is bogged down by infrastructure complexities, such as database interaction, network communication, performance optimization, maintaining distributed consistency, and managing schema evolution.

Cloud services simplify application deployment and infrastructure management, yet they do not reduce the complexities within the application code itself, as the way infrastructure is managed (whether cloud-managed or self-hosted) doesn't alter how the code must interact with it.

Low-code platforms offer infrastructure-free development environments, but struggle to express genuinely intricate business logic due to GUI limitations.

## The Kiwi Approach

Kiwi features a syntax comparable to modern general-purpose languages, yet uses an application platform as its runtime. This contrasts with traditional languages, which employ single-machine environments as their runtime (e.g, OS for C, or JVM for Java). Kiwi's novel architecture is designed to dramatically increase the speed of application development.

## Low-Code Synergy

Kiwi's infrastructure-free model enables natural integration with low-code platforms. For instance, we have developed "Kiwi Pages," a product that automatically generate low-code style web pages for Kiwi programs.

## AI Enhancement

This simplified programming paradigm, especially when combined with low-code capabilities, makes Kiwi well-suited for AI-driven development. Current AI models, despite their high capabilities, often struggle to create entire applications. This difficulty stems from the challenge of integrating numerous components—such as database schemas, backend code, and frontend interfaces—without errors. Kiwi aims to lower this barrier, making it significantly easier for AI to generate fully functional applications.

## Our Vision

Our vision is to make application development simple, incredibly fast, and empower developers to build and run products rapidly using code, low-code and AI.

## Business Model

We plan to commercialize Kiwi through a cloud service encompassing three core components: a hosted Kiwi runtime, an integrated low-code platform, and an AI-powered development agent.

## Go-to-Market Strategy

Kiwi's market entry strategy begins with the less critical, low-code sector. Low-code applications are typically less demanding for language maturity, which is ideal for a nascent Kiwi. However, Kiwi's architecture is fully capable of handling critical applications, and as Kiwi matures, we intend to gradually penetrate that sector.

## Demos

[API Demo](https://www.bilibili.com/video/BV1UzEkzhEkw/)

[Pages Demo](https://www.bilibili.com/video/BV16Y7WzfEf3/)

## Source Code

[Kiwi on Github](https://github.com/kiwi-language/kiwi)


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
        fn buy(@Label("数量") quantity: int, @Label("优惠券") coupon: Coupon?) -> Order {
            reduceStock(quantity)
            var price = this.price.times(quantity)
            if (coupon != null)
                price = price.sub(coupon!!.redeem())
            val order = Order(price)
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
    @Label("折扣") val discount: Money
    ) {

        @Label("已核销")
        var redeemed = false

        @Label("核销")
        fn redeem() -> Money {
            if (redeemed)
                throw Exception("Coupon already redeemed")
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

        fn placeOrder(@Label("商品列表") products: Product[]) -> Order {
            require(products.length > 0, "Missing products")
            val price = products[0].price
            var i = 1
            while (i < products.length) {
                price = price.add(products[i].price)
                i++
            }
            val order = Order(price)
            products.forEach(p -> {
                p.reduceStock(1)
                order.Item(p, 1)
            })
            return order
        }

    }

    @Label("订单")
    class Order(
        @Label("总价")
        val price: Money
    ) {
        
        @Label("订单项")
        class Item(
            @Label("商品")
            val product: Product,
            @Label("数量")
            val quantity: int
        )

    }

Important Notes:

1. Unlike kotlin, Kiwi uses array (->) to denote function return type.
2. Time type is not yet supported, so avoid using it.
3. Array creation syntax is new ElementType[], e.g., new string[]
4. To add an element into an array, invoke the the append method on the array object
5. The semi colon separating enum constants and other class members is necessary even if the class only contains enum constants.
6. There's no toString method. When concatenating objects with strings, the objects are automatically converted into string.
7. Available primitive types: int, long, float, double, string, bool
8. Child objects are automatically added to an implicit list of its parent, there's no need to explicitly
9. Common methods/fields that are currently missing: array.find, array.filter, string.length. So avoid using them.
10. parameter default values are not supported
11. child objects are maintained by an implicit list under the parent object, however this list is currently inaccessible. Therefore, don't try to access child objects. 
12. @Summary field must be string
13. `time` is a reserved keyword, do not use it as an identifier
14. Bean class name takes the following form: {EntityClassName}Service. For example, ProductService, CouponService and OrderService.
15. Value objects: value objects are immutable and identity-less objects. There are two common use cases for value objects:
    * Representing values in domain models, e.g., Money 
    * As service method parameters for encapsulating complex information, such as OrderPlacementRequest

Output Format:

The generated program shall be contained in a single source file and you shall output that source file in plain text.
The source code shall contain ABSOLUTELY NO COMMENT.
Your output must ONLY contain the code. ABSOLUTELY NOTHING ELSE. No explanation, no markdown tags.
Your output shall only contain the source file. Nothing else. 

Here is the description for the program to be generated:
