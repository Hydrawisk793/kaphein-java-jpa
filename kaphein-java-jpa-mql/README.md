# kaphein-java-jpa-mql

Use MongoDB-like JSON syntaxes to query JPA entities.

Although the word `MQL` from the name `JPA-MQL` is actually came from [MongoDB Query Language](https://www.mongodb.com/docs/manual/tutorial/query-documents), there are many differences between relational databases and document databases like MongoDB and each query language cannot be easily converted to each other.  
So, this library provides quite different syntaxes to represent JSON-based query language for JPA.

## Requirements

### JDK
`11` or later.

### JPA
A JPA implementation that follows [Jakarta Persistence API(JPA 3.x) specification](https://jakarta.ee/specifications/persistence/3.0).  
Currently, **only Jakarta Persistence API(JPA 3.x)** is supported and Java Persistence API(JPA 2.x) **IS NOT SUPPORTED**.

### Dependencies
* `jackson-databind` : `2.14.0` or later.
* `slf4j-api` : `2.0.0` or later.

## How to use

There are following JPA entity classes:

```Java
@Table(name = "t_department")
@Entity
public class Department
{
  protected Department()
  {
    // Empty.
  }

  public String getId()
  {
    return id;
  }

  public String getDepartmentName()
  {
    return departmentName;
  }

  public Instant getCreatedAt()
  {
    return createdAt;
  }

  public Instant getModifiedAt()
  {
    return modifiedAt;
  }

  @Id
  @Column(length = 32, nullable = false)
  private String id;

  @Column(length = 128)
  private String departmentName;

  @Column(nullable = false)
  private Instant createdAt;

  private Instant modifiedAt;
}
```

```Java
@Table(name = "t_user")
@Entity
public class User
{
  protected User()
  {
    // Empty.
  }

  public String getId()
  {
    return id;
  }

  public String getUserName()
  {
    return userName;
  }

  public Integer getAge()
  {
    return age;
  }

  public Instant getCreatedAt()
  {
    return createdAt;
  }

  public Instant getModifiedAt()
  {
    return modifiedAt;
  }

  @Id
  @Column(length = 32, nullable = false)
  private String id;

  @Column(length = 64)
  private String userName;

  private Integer age;

  @Column(length = 32)
  private String departmentId;

  @Column(nullable = false)
  private Instant createdAt;

  private Instant modifiedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "departmentId", insertable = false, updatable = false)
  private Department department;
}
```

To create MQL queries, use `MqlQueryBuilder`.

```Java
final var queryBuildResult = new MqlQueryBuilder<User>()
  .entityManager(entityManager)
  .entityType(entityType)
  .objectMapper(objectMapper)
  .filter("""
    {
      "$entityName" : "User",
      "$alias" : "u",
      "$jpqlWhere" : {
        "u.userName" : { "$like" : "%foo%" }
      }
    }
  """)
  .orderByTerms(List.of(
    new Sort.Order("u.age", true),
    new Sort.Order("u.id", false)
  ))
  .limit(5)
  .offset(0)
  .build();
```

`MqlQueryBuilder` creates an item query and a total item count query.  

Use item query to fetch JPA entities.

```Java
final var users = queryBuildResult
  .getItemQuery()
  .getResultList();
```

Use count query to fetch the total number of items.

```Java
final var totalUserCount = queryPair
  .getCountQuery()
  .getSingleResult();
```

### Value operators

`$eq`, `$ne`, `$gte`, `$gt`, `$lte`, `$lt`, `$in` and `$nin` are supported.  

Use `$attrPath` to specify a path to JPA entity attribute on the right hand side of a condition expression.  

```JSON
{
  "$entityName" : "Department",
  "$alias" : "d",
  "$jpqlWhere" : {
    "d.departmentName" : { "$like" : "%foo%" },
    "$jpqlExists" : {
        "$entityName" : "User",
        "$alias" : "u",
        "$jpqlWhere" : {
          "u.departmentId" : { "$eq" : { "$attrPath" : "d.id" } },
          "u.age" : { "$gte" : 20 }
        }
    }
  }
}
```

`$like` and `$nlike` operators are supported. These operators are similar to `LIKE` operator in JPQL.  
Set `$caseSensitive` to `false` for case-insensitive string matching.  
Specify `$escape` to change the escape character.

```JSON
{
  "$entityName" : "User",
  "$alias" : "u",
  "$jpqlWhere" : {
    "u.userName" : {
      "$like" : "%foo%",
      "$caseSensitive" : false,
      "$escape" : "$"
    }
  }
}
```

### Logical operators

`$and` and `$or` are supported.

### `JpqlExists` operator

`$jpqlExists` and `$nJpqlExists` are different to the `$exists` operator in the MongoDB query language, but similar to `EXISTS` operator in JPQL.

```JSON
{
  "$entityName" : "Department",
  "$alias" : "d",
  "$jpqlWhere" : {
    "d.departmentName" : { "$like" : "%foo%" },
    "$jpqlExists" : {
        "$entityName" : "User",
        "$alias" : "u",
        "$jpqlWhere" : {
          "u.departmentId" : { "$eq" : { "$attrPath" : "d.id" } },
          "u.age" : { "$gte" : 20 }
        }
    }
  }
}
```

## License

MIT
