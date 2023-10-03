slick-jdbc-extension [![CI][ci-img]][ci] [![Maven Central][maven-img]][maven]
====================

An extension to `slick.jdbc`, [Slick][slick]'s plain SQL queries,
including the following features.

- [More types in SQL interpolation](#setparameter)
    - Literal parameters
    - Non-empty lists
    - Case classes (products)
- [Extensible raw query translator](#translator)
    - Embedding caller information as a comment
    - Stripping margin
- [Result mapper by column name](#getresult)

These features can be selectively enabled.

## <a name="install"></a> Getting started

Add dependency in your `build.sbt` as the following.

```scala
    libraryDependencies ++= Seq(
      ...
      "com.github.tarao" %% "slick-jdbc-extension" % "0.1.0"
    )
```

The library is available on [Maven Central][maven].  Currently,
supported Scala versions are 2.12, and 2.13.

## <a name="overview"></a> Overview

[Slick][slick] supports plain SQL queries but it lacks some essential
features.  For example, there is no way to bind a list parameter to a
prepared statement.

This extension provides additional features to Slick-style plain SQL
queries.  The extension consists of a family of traits and each of
them can be enabled selectively.  The easiest way to use the extension
is to define a class with the traits.

Let's see an example of the usage of the extension.  To start with, we
assume to have an abstract class to run a DB query something like
this.

```scala
abstract class DBHandler {
  import scala.concurrent.{Future, Await}
  import scala.concurrent.duration.Duration
  import slick.dbio.{DBIOAction, NoStream, Effect}
  import slick.backend.DatabaseComponent

  type Database = DatabaseComponent#DatabaseDef

  def run[R](a: DBIOAction[R, NoStream, Nothing])(implicit db: Database): R =
    Await.result(db.run(a), Duration.Inf)
}
```

Then a repository class to use plain SQL queries with the extension will look like this.  (We are not interested in how the concrete DB object is supplied.)

```scala
import com.github.tarao.slickjdbc._
import interpolation.{SQLInterpolation, CompoundParameter, TableName}
import getresult.{GetResult, AutoUnwrapOption}

class SampleRepository extends DBHandler
    with SQLInterpolation with CompoundParameter
    with GetResult with AutoUnwrapOption {
  implicit def db: Database = ???

  ...
}
```

`SQLInterpolation`, `CompoundParameter`, `GetResult` and
`AutoUnwrapOption` are feature selectors of the extension.

For the rest of the example, we use `Entry` model class defined as the
following.

```scala
case class Entry(id: Long, url: String)
```

To define a mapping from a column name to a field of the model in the
repository class, use `getResult` method defined in `GetResult` trait.

```scala
  implicit val getEntryResult = getResult { Entry(
    column("entry_id"),
    column("url")
  ) }
```

This will map `entry_id` column of a query result to the first field
of `Entry` and `url` column to the second.  This is very similar to
Slick's `GetResult.apply`.  The type of the column is inferred by the
field type of `Entry.id` (it is `Long` in this case).  Note that this
example also uses `AutoUnwrapOption` to convert from `Option[Long]` to
`Long`.  The default column type is `Option[_]`ed since the resulting
column value may be `NULL`.

Let's see an example of SQL interpolation.  In addition to Slick's SQL
interpolation, we can specify a structured value directly as a
parameter.

```scala
  val table = TableName("entry")

  def add(entry: Entry) =
    run { sqlu"""
    | INSERT INTO ${table} (entry_id, url)
    | VALUES ${entry}
    """
  }
```

This actually illustrates three things.

- You can specify a table name by a value of `TableName`.  This value
  will be embedded literally in the raw query.
- A value of case class (`entry` in this case) is expanded to
  placeholders like `(?, ?)` and the fields of the value are bound
  to them.
- Characters before `|` is stripped in the raw query.

You can also specify a list as a parameter.  But in this case, you
must say that it is not empty.

```scala
  import eu.timepit.refined.api.Refined
  import eu.timepit.refined.collection.NonEmpty

  def findAll(entryIds: Seq[Long] Refined NonEmpty): Seq[Entry] = run {
    sql"""
    | SELECT * FROM ${table}
    | WHERE entry_id IN $ids
    """.as[Entry]
  }
```

`$ids` must be refined by `NonEmpty` and it expands to `(?, ?, ...,
?)`.  You can use `refineV` to refine values.

```scala
  import eu.timepit.refined.collection.NonEmpty
  import eu.timepit.refined.refineV

  val idsOrError = refineV[NonEmpty](Seq(101L, 102L, 103L))
  val entries = ids match {
    case Right(ids) => repository.findAll(ids)
    case Left(_)    => sys.error("Never happen in this case")
  }
```

See the documentation of [refined][] for the detail about refined
types.

If you were using [nonempty][] from older versions, it still works as
expected because `nonempty.NonEmpty` is a kind of refined collection
type.

```scala
  import com.github.tarao.nonempty.NonEmpty

  def findAll(entryIds: Option[NonEmpty[Long]]): Seq[Entry] = entryIds match {
    case Some(ids) => run {
      sql"""
      | SELECT * FROM ${table}
      | WHERE entry_id IN $ids
      """.as[Entry]
    }
    case None => Seq.empty
  }
```

In this case, you call `findAll` method like the following since there is an implicit conversion from `Seq[Long]` to `Option[NonEmpty[Long]]`.

```scala
repository.findAll(Seq(101L, 102L, 103L))
```

See the documentation of [nonempty][] for the detail.

## <a name="setparameter"></a> Additional types in SQL interpolation

To use the extended SQL interpolation, you have to first enable it.
To enable the interpolation inside a particular trait or class, add
`SQLInterpolation` trait to the trait or class.

```scala
import com.github.tarao.slickjdbc._
class YourRepositoryClass extends interpolation.SQLInterpolation
```

If you want to explicitly `import` the feature, then `import` methods
in `SQLInterpolation` object.

```scala
import com.github.tarao.slickjdbc._
import interpolation.SQLInterpolation._
```

If you are `import`ing a Slick driver API, doing that by `import
slick.driver.SomeDriver.api._` causes a conflict with the extended SQL
interpolation since the API also defines a SQL interpolation of Slick.
In this case, `import` only desired features to avoid the conflict.
For example, if you are using `Database` from the API, import it as
`import slick.driver.SomeDriver.api.Database`.

### Literal parameters

When a value with `Literal` is passed as a parameter of the
interpolation, a string returned by `toString` on the value is
embedded literally in the raw query as if it was embedded by `#${}`.
There are two predefined `Literal`s.

- `SimpleString`
    - takes a string as a parameter of the constructor and embed that string.
- `TableName`
    - takes a name as a parameter of the constructor and embed that name.

Note that a `Literal` value is **not** expanded to a placeholder `?`.
If the value needs to be escaped as a part of a SQL string, then you
should do it by yourself.

### Non-empty lists

This feature requires `interpolation.ComboundParameter` trait or
`import interpolation.ComboundParameter._`.

When a non-empty list is passed as a parameter of the interpolation,
the value expands to `?`s in the prepared statement, and items in the
value are bound to the statement.

For example, if you have `val Right(ne) = refineV[NonEmpty](Seq(1, 2,
3))` and embed it as `sql"... ($ne) ..."`, the raw query will be
`... (?, ?, ?) ...` and `1`, `2`, `3` are bound to the placeholders.
The resulting raw query will be the same if you omit parentheses like
`sql"... $ne ..."`.

If you pass a non-empty list of tuples, for example pairs, it expands
to `(?, ?), (?, ?), ..., (?, ?)`.  This is useful to `INSERT` multiple
values by `INSERT INTO table (col1, col2) VALUES $aListOfPairs`.

### Case classes (products)

This feature requires `interpolation.ComboundParameter` trait or
`import interpolation.ComboundParameter._`.

When a product especially an instance of a case class is passed as a
parameter of the interpolation, the value expands to `?`s in the
prepared statement, and fields in the value are bound to the
statement.

For example, if you have `val p = SomeCaseClass(1, "foo")` and embed
it as `sql"... ($p) ..."`, the raw query will be `... (?, ?) ...` and
`1`, `"foo"` are bound to the placeholders.  The resulting raw query
will be the same if you omit parentheses like `sql"... $p ..."`.

If you have another product value in a field of the passed value, it
also be expanded and the placeholders are flattened.

You can also put a product value in a non-empty list and it has the
same effect as putting tuples **except when the arity of the product
is 1**.  When the product arity is exactly 1, the value is treated as
if it is a primitive value.  In other words, a value of `Seq[[Single]
Refined NonEmpty` (where `Single` is a `Product` of arity 1) expands
to `(?, ?, ..., ?)` not to `(?), (?), ..., (?)`.  In contrast, a value
of `Seq[Tuple1[_]] Refined NonEmpty` expands to `(?), (?), ..., (?)`
not to `(?, ?, ..., ?)`.

Note that passing `Option[_]` as an interpolation parameter is
statically rejected and passing a product of arity zero (this can be
made by for example `case class Empty()`) causes
`java.sql.SQLException` at runtime.

## <a name="translator"></a> Raw query translator

To use this feature, you have to enable the extended SQL
interpolation.  Follow the instruction [above](#setparameter) to
enable it.

The query translator translates a raw query generated by the
interpolation.  A translation is simply from string to string and you
can stack multiple translators.

### The default translators

- `MarginStripper`
    - Strip characters before `|` from each line of a query string.
      This makes a query log a bit more readable.
- `CallerCommenter`
    - Record the code position that the interpolation is invoked as a
      comment of SQL query.  You can trace a line number, a file name
      and a method name in a class from an ordinary query log on the
      DB server.

### Custom translators

You can use a custom translator stack by defining an implicit value of
`Iterable[query.Translator]`.  For example, if you want to add a
translator after the default translators, define an implicit value in
a static scope where you invoke the SQL interpolation.

```scala
import com.github.tarao.slickjdbc._

implicit val translators: Iterable[query.Translator] = Seq(
  query.MarginStripper,
  query.CallerCommenter,
  new query.Translator {
    def apply(query: String, context: query.Context) =
      ??? /* return a translated string */
  }
)
```

## <a name="getresult"></a> Result mapper by column name

This feature requires `getresult.GetResult` trait or object.

With the trait, you can use `getResult` method and can use `column`
method in the block of `getResult` without a receiver like in the
example in [Overview](#overview) section.

If you prefer the object, you just need to `import` it.

```scala
import com.github.tarao.slickjdbc._
import getresult.GetResult
```

The usage of `GetResult` is quite similar to that of Slick's
`GetResult`.  You will receive an object to extract a result value in
a block passed to `GetResult.apply`.

```scala
implicit val getEntryResult = GetResult { r => Entry(
  r.column("entry_id"),
  r.column("url")
) }
```

It also requires `getresult.AutoUnwrapOption` to extract a column
value of non-`Option[_]` type.  This also available both as a trait
and an object.

You can use this feature without the extended SQL interpolation.

### Mapping columns by name

As you have already seen, a column value can be extracted by `column`
method.  If you want to explicitly specify a return type or the return
type somehow cannot be inferred, you can pass a type parameter to
`column` to specify a return type.

```scala
implicit val getEntryResult = getResult { Entry(
  column[Long]("entry_id"),
  column[String]("url")
) }
```

### Mapping columns by position

You can also extract the result by the index of a column.

```scala
implicit val getEntryResult = getResult { Entry(
  column(1),
  column(2)
) }
```

If you prefer the way of `<<` method in Slick's `GetResult`, it is
also available in our `GetResult`.

```scala
implicit val getEntryResult = getResult { Entry(<<, <<) }
```

`<<?` and `skip` are also available.

### `Option[_]` values

`getresult.AutoUnwrapOption` makes it possible to get non-`Option[_]`
value directly.  Note that this may cause `NoSuchElementException`
thrown if the value in the DB record is `NULL`.

If you want to specify a default value instead of throwing an
exception, then retrieve the result as an `Option[_]` and give a
default value.

```scala
implicit val getEntryResult = getResult { Entry(
  column[Option[Long]]("entry_id").getOrElse(0),
  column[Option[String]]("url").getOrElse("http://example.com/default/page")
) }
```

### User-defined type binders

The way of extracting a typed value from a raw query result is
different from that of Slick's `GetResult`.  We provide a
`TypeBinder[_]` which is similar to `TypeBinder[_]` in
[ScalikeJDBC][scalikejdbc] to specify extractor methods for both
indexed and named results.

Let's take `url` field of `Entry` as an example.  Assume that you want
to use your custom case class `URL` as a `url` field instead of
`String`.

```scala
case class URL(url: String)
case class Entry(id: Long, url: URL)
```

Then you need to define a custom `TypeBinder[_]` for `URL`.  A good
way to define a custom type binder for a user-defined type is to
extend a type binder for a primitive type.


```scala
implicit def urlBinder(implicit
  binder: TypeBinder[Option[String]]
): TypeBinder[Option[URL]] = binder.map(_.map(URL(_)))
```

This binder uses a `TypeBinder[Option[String]]` as a parent binder,
which is defined by default, and convert the string retrieved by the
parent to a `URL`.  (The first `map` is to convert retrieved value,
and the second one is to take care of the `Option[_]` type.)

Note that you should define a `TypeBinder[_]` for `Option[URL]` not
for `URL` since unwrapping `Option[_]` will be done by general
unwrapping conversion by `AutoUnwrapOption`.

With this binder, you can define a result mapper for `Entry`.

```scala
implicit val getEntryResult = getResult { Entry(
  column("entry_id"),
  column("url")
) }
```

Notice that there is no difference in the code compared to the one
defined before for `case class Entry(id: Long, url: String)` but the
(inferred) return type of `column("url")` is now `URL`.

## License

- Copyright (C) INA Lintaro
- MIT License

[slick]: http://slick.typesafe.com/
[refined]: https://github.com/fthomas/refined
[nonempty]: https://github.com/tarao/nonempty-scala
[scalikejdbc]: http://scalikejdbc.org/

[ci]: https://github.com/tarao/slick-jdbc-extension-scala/actions/workflows/ci.yaml
[ci-img]: https://github.com/tarao/slick-jdbc-extension-scala/actions/workflows/ci.yaml/badge.svg
[maven]: https://maven-badges.herokuapp.com/maven-central/com.github.tarao/slick-jdbc-extension_2.13
[maven-img]: https://maven-badges.herokuapp.com/maven-central/com.github.tarao/slick-jdbc-extension_2.13/badge.svg
