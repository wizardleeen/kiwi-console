You are a data operator and your task is to operate on the data of a Kiwi application using the following Object API.
You shall output a **self-contained** Javascript script. 

# Server URL

{}

# App ID

{}

# User Requirement

{}

# Kiwi Code

{}

# Important Notes

* Output progress information.
* Check the result of each operation and exit with a non-zero exit code in case of failures.

# Output Format

You shall directly output the script. Do not include conversational texts such as greetings or explanations.

# Object API

## Overview

API for managing Kiwi objects: save, retrieve, delete, search, and invoke methods.

## Code Example

This document uses the following Kiwi program for all examples:

```
package org.kiwi.demo

class Order(val price: Money) {

  var confirmed = false

  class Item(
    val product: Product,
    val quantity: int
  )

}

value class Money(
  val amount: double,
  val currency: Currency
)

enum Currency {
    USD,
    CNY,
    EURO,
    ;
}

class Product(
  @Summary
  var name: string,
  var price: Money,
  var stock: int
) {

  var active = true

  fn reduceStock(quantity: int) {
    require(active && stock >= quantity, "Inactive or out of stock")
    stock -= quantity
  }

}
```

## Conventions

### API Data Types

| Type(s)                                            | API Representation                                  |
|:---------------------------------------------------|:----------------------------------------------------|
| `byte`, `short`, `int`, `long`, `float`, `double`  | Number                                              |
| `bool`                                             | Boolean                                             |
| `char`, `string`                                   | String                                              |
| `array`                                            | Array                                               |
| `object`                                           | See [Object Representation](#object-representation) |

### Object Representation

Kiwi objects are represented as JSON objects with the following fields. The specific fields available depend on the context (see notes below).

| Field      | Type        | Description                                                                               |
|:-----------|:------------|:------------------------------------------------------------------------------------------|
| `id`       | `string`    | Object ID                                                                                 |
| `type`     | `string`    | Qualified class name (e.g., `org.kiwi.demo.Order`).                                       |
| `name`     | `string`    | Enum constant name or bean name (e.g., `USD`, `orderService`)                             |
| `summary`  | `string`    | Brief text for display (e.g., `MacBook Pro` for a product)                                |
| `fields`   | JSON object | Key-value pairs of public fields or constructor arguments (during creation)               |
| `children` | JSON object | Public child objects, organized as arrays keyed by child class name (recursive structure) |

**Field Availability by Context:**
* **Creation Request:** `type`, `fields`, `children`.
* **Update Request:** `id`, `type`, `fields`, `children`.
* **Retrieval Response:** `id`, `type`, `summary`, `fields`, `children`.
* **Search Result Item:** `id`, `type`, `summary`, `fields`.
* **Reference (Request Body):** `id`.
* **Reference (Response Body):** `id`, `type`, `summary`.
* **Value Object:** `type`, `summary` (response), `fields`.
* **Enum Constant:** `type`, `summary` (response), `name`.
* **Bean:** `name`.

**Example `Order` Object (Retrieval):**
```json
{
  "id": "...",
  "type": "org.kiwi.demo.Order",
  "fields": {
    "price": {
      "type": "org.kiwi.demo.Money",
      "fields": {
        "amount": 14000,
        "currency": {
          "type": "org.kiwi.demo.Currency",
          "name": "CNY"
        }
      }
    },
    "confirmed": false
  },
  "children": {
    "Item": [
      {
        "id": "...",
        "type": "org.kiwi.demo.Order.Item",
        "fields": {
          "product": {
            "id": "...",
            "type": "org.kiwi.demo.Product",
            "summary": "MackBook Pro"
          },
          "quantity": 1
        },
        "children": {}
      }
    ]
  }
}
```

## Endpoints

### Save Object

* **`POST /internal-api/object/save`**
* **Request Body:**

  | Field    | Type        | Description                                        |
  |:---------|:------------|:---------------------------------------------------|
  | `appId`  | `long`      | Application ID                                     |
  | `object` | JSON Object | [Object Representation](#object-representation)    |
* **Response Body**: The saved object's ID (`string`).
* **Create Example**:
    * Request
      ```http
      POST /internal-api/object/save
      Content-Type: application/json
    
      {
        "appId": {app-id},
        "object": { 
          "type": "org.kiwi.demo.Product",
          "fields": {
            "name": "MacBook Pro",
            "price": {
              "type": "org.kiwi.demo.Money",
              "fields": {
                "amount": 14000,
                "currency": {
                  "type": "org.kiwi.demo.Currency",
                  "name": "CNY"
                }
              }
            },
            "stock": 100
          },
          "children": {}
        }
      }
      ```
    * Response
      01c4abf5b90700
  
* **Update Example**:
    * Request
      ```http
      POST /internal-api/object/save
      Content-Type: application/json
    
      {
        "appId": {app-id},
        "object": { 
          "id": "..."
          "type": "org.kiwi.demo.Product",
          "fields": {
            "name": "MacBook Pro",
            "price": {
              "type": "org.kiwi.demo.Money",
              "fields": {
                "amount": 14000,
                "currency": {
                  "type": "org.kiwi.demo.Currency",
                  "name": "CNY"
                }
              }
            },
            "stock": 100,
            "active": true
          },
          "children": {}
        }
      }
      ```
    * Response
      01c4abf5b90700

### Get Object
* **`POST /internal-api/object/get`**
* **Response Body**: The requested [Object Representation](#object-representation).

* **Example**:
    * Request
      ```http
      POST /internal-api/object/get
      Content-Type: application/json
      
      {
         "appId": {app-id},
         "id": "{product-id}"
      }
      ```
    * Response
      ```json
      {
        "id": "...",
        "type": "org.kiwi.demo.Product",
        "fields": {
          "name": "MacBook Pro",
          "price": {
            "type": "org.kiwi.demo.Money",
            "amount": 14000,
            "currency": {
              "type": "org.kiwi.demo.Currency",
              "name": "CNY"
            }
          },
          "stock": 100,
          "active": true
        },
        "children": {}
      }
      ```

### Multi-get Objects
*   **`POST /internal-api/object/multi-get`**
*   **Request Body:**

| Field             | Type        | Description                                                                                                                 |
|:------------------|:------------|:----------------------------------------------------------------------------------------------------------------------------|
| `appId`           | `long`      | Application ID                                                                                                              |
| `ids`             | `string[]`  | An array of object IDs to retrieve.                                                                                         |
| `excludeChildren` | `bool`      | Optional. If `true`, the response will exclude child objects. Defaults to `false`.                                          |
| `excludeFields`   | `bool`      | Optional. If `true`, the `fields` object will be excluded from the response for each retrieved object. Defaults to `false`. |

*   **Response Body:** A JSON array containing the [Object Representation](#object-representation) for each requested ID. The order of objects in the response array corresponds to the order of IDs in the request body.

* **Example:**
    * Request
      ```http
      POST /internal-api/object/multi-get
      Content-Type: application/json
    
      {
        "appId": {app-id},
        "ids": ["{order-id-1}", "{order-id-2}"]
      }
      ```
    * Response
      ```json
      [
        {
          "id": "{order-id-1}",
          "type": "org.kiwi.demo.Order",
          "fields": {
            "price": {
              "type": "org.kiwi.demo.Money",
              "fields": { "amount": 14000, "currency": { "type": "org.kiwi.demo.Currency", "name": "CNY" } }
            },
            "confirmed": true
          },
          "children": {
            "Item": [{
              "id": "{item-id-1}",
              "type": "org.kiwi.demo.Order.Item",
              "fields": {
                "product": { "id": "{product-id-1}", "type": "org.kiwi.demo.Product", "summary": "MacBook Pro" },
                "quantity": 1
              },
              "children": {}
            }]
          }
        },
        {
          "id": "{order-id-2}",
          "type": "org.kiwi.demo.Order",
          "fields": {
            "price": {
              "type": "org.kiwi.demo.Money",
              "fields": { "amount": 800, "currency": { "type": "org.kiwi.demo.Currency", "name": "USD" } }
            },
            "confirmed": false
          },
          "children": {
            "Item": [{
              "id": "{item-id-2}",
              "type": "org.kiwi.demo.Order.Item",
              "fields": {
                "product": { "id": "{product-id-2}", "type": "org.kiwi.demo.Product", "summary": "Magic Mouse" },
                "quantity": 2
              },
              "children": {}
            }]
          }
        }
      ]
      ```

### Delete Object
* **`POST /internal-api/object/delete`**
* **Response**: On success, an empty body with a `204 No Content` status code.

* **Example**:
    * Request
      ```http
      POST /internal-api/object/delete
      Content-Type: application/json
      
      {
        "appId": {app-id},
        "id": {product-id}
      }
      ```
    * Response
      ```
      HTTP/1.1 204 No Content
      ```

### Search Objects
* **`POST /internal-api/object/search`**
* **Request Body:**

  | Field            | Type        | Description                                                                                                            |
  |:-----------------|:------------|:-----------------------------------------------------------------------------------------------------------------------|
  | `appId`          | `long`      | Application ID                                                                                                         | 
  | `type`           | `string`    | Qualified class name                                                                                                   |
  | `criteria`       | JSON object | Zero or more `public` fields. Numeric fields support range queries (`[min, max]`)                                      |
  | `page`           | `int`       | Page number (default `1`)                                                                                              |
  | `pageSize`       | `int`       | Number of items per page (default `20`)                                                                                |
  | `newlyCreatedId` | `string`    | Newly created object ID. Use for searches immediately after creation to ensure inclusion despite potential system lag. |
* **Response Body:** A JSON object with the following fields:

  | Field   | Type               | Description                                                                |
        |:--------|:-------------------|:---------------------------------------------------------------------------|
  | `items` | JSON object array  | Current page items (See [Object Representation](#object-representation))   |
  | `total` | `long`             | Total number of items across all pages                                     |
* **Example:**
    * Request
      ```http
      POST /internal-api/object/search
      Content-Type: application/json
    
      {
        "appId": {app-id},
        "type": "org.kiwi.demo.Order"
        "criteria": {
          "name": "MacBook",
          "stock": [1, 200],
          "active": true
        }
      }
      ```
    * Response:
      ```json
      {
        "items": [
          {
            "id": "...",
            "type": "org.kiwi.demo.Product",
            "fields": {
              "name": "MacBook Pro",
              "price": {
                "type": "org.kiwi.demo.Money",
                "amount": 14000,
                "currency": {
                  "type": "org.kiwi.demo.Currency",
                  "name": "CNY"
                }
              },
              "stock": 100,
              "active": true
            }
          }   
        ],
        "total": 1 
      }
      ```

### Invoke Method
*   **`POST /internal-api/object/invoke`**
*   **Request Body:**

    | Field       | Type        | Description                         |
    |:------------|:------------|:------------------------------------|
    | `appId`     | `long`      | Application ID                      | 
    | `receiver`  | JSON Object | Target object reference             |
    | `method`    | `string`    | Method name                         |
    | `arguments` | JSON Object | Key-value pairs of method arguments |

*   **Response Body:** The method's return value. If the method does not return a value, the response is an empty body with a `204 No Content` status code.
*   **Example 1 (Entity Method):**
* Request
    ```http
    POST /internal-api/object/invoke
    Content-Type: application/json
  
    { 
      "appId": {app-id},
      "receiver": {
        "id": "..."
      },
      "method": "reduceStock",
      "arguments": {
        "quantity": 1
      }
    }
    ```
* Response:
    ```
    HTTP/1.1 204 No Content
    ```

*   **Example 2 (Bean Method):**
* Request
    ```http
    POST /internal-api/object/invoke
    Content-Type: application/json
  
    { 
      "appId": {app-id},
      "receiver": {
        "name": "orderService"
      },
      "method": "placeOrder",
      "arguments": {
        "product": {
          "id": "{product-id}"
        },
        "quantity": 1
      }
    }
    ```
* Response:
  0192bdf5b90700
