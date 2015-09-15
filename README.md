# expressier

[![Build status](https://travis-ci.org/travisbrown/expressier.svg?branch=master)](https://travis-ci.org/travisbrown/expressier)
[![Coverage status](http://codecov.io/github/travisbrown/expressier/coverage.svg?branch=master)](http://codecov.io/github/travisbrown/expressier?branch=master)

## tl;dr

Get more useful stuff out of your regular expressions:

```scala
scala> import io.expressier._
import io.expressier._

scala> case class Item(name: String, x: Int, c: Char, s: String)
defined class Item

scala> val parse = """(?<name>\w+): (?<x>\d+), (?<c>.), (?<s>[A-Z]*)""".express[Item]
parse: io.expressier.Express.To[Item] = ...

scala> val result: Option[Item] = parse("Foo: 1001, ?, BAR")
result: Option[Item] = Some(Item(Foo,1001,?,BAR))
```

Or don't even bother with the case class:

```scala
scala> val Qux = x"""(?<first>.+) (?<second>\d+) (?<third>.) (?<last>\1)"""

scala> Qux("foo 12345 - foo").map(_.first)
res0: Option[String] = Some(foo)

scala> Qux("foo 12345 - foo").map(_.second)
res1: Option[Int] = Some(12345)
```

A group matching `\d+` will end up as an `Int`, a `.` will be a `Char`, etc.
If the types don't line up or you try to refer to a named group that doesn't
exist, the compiler will let you know.

## Overview

This is a quick proof-of-concept demonstration of what a type provider for
regular expressions might look like. There are currently lots of limitations
and problems with the implementation, and there's essentially no documentation
beyond this README. For a more detailed introduction to type providers in Scala,
please see [this project](https://github.com/travisbrown/type-provider-examples)
and the [associated slide deck][type-provider-slides].

expressier is cross-built for Scala 2.10 and 2.11 (from a single codebase,
thanks to Miles Sabin's new [macro-compat][macro-compat] project), but it
provides only partial support for [Scala.js][scala-js], since named groups in
regular expressions currently do not work on that platform.

## Motivation

Suppose I've got a file with some kind of simple data format that I've read
into a list of strings.

``` scala
val rows = List(
  "en: 456, 123, 1203",
  "de:  12, 567,  200",
  "cz:   1,  32,   10"
)
```

Now suppose I only care about totals, and want a map from the language identifier
to the sum of the three values. Assuming the format is fairly simple, it's
reasonable to try to solve this problem with a regular expression:

``` scala
val Row = """(\w\w)\s*:\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)\s*""".r
```

Now we can write something like the following:

``` scala
rows.map {
  case Row(id, first, second, third) => (id, first + second + third)
  case _ => sys.error("Invalid input")
}.toMap
```

Except of course that thanks to [the magic of `any2stringadd`](https://issues.scala-lang.org/browse/SI-194)
this gives us something completely different from what we expected:

``` scala
Map(en -> 4561231203, de -> 12567200, cz -> 13210)
```

Where the values are strings, not integers. It's easy to forget that even
though we know that we should be able to convert `(\d+)` into an integer
(assuming we'll never have more than nine or ten digits), the compiler doesn't,
and it's just handing those matches back to us as strings. So we write the
following:

``` scala
rows.map {
  case Row(id, first, second, third) =>
    (id, first.toInt + second.toInt + third.toInt)
  case _ => sys.error("Invalid input")
}.toMap
```

This works, but it's more verbose, a little redundant, and it involves a
partial function. We know `toInt` should never explode here, but the compiler
doesn't.

## With our type provider

The implementation provided here allows the following usage:

``` scala
import io.expressier._

val Row = x"""(\w\w)\s*:\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)\s*"""

val totals: Map[String, Int] = rows.collect {
  case Row(id, first, second, third) => (id, first + second + third)
}.toMap
```

Or something like this:

``` scala
scala> val Row(id, first, second, third) = "en: 456, 123, 1203"
id: String = en
first: Int = 456
second: Int = 123
third: Int = 1203
```

The digit groups are now typed as integers, and as a bonus we get compile-time
validation for our regular expression syntax.

## With named groups

Now suppose we've got a regular expression with some named groups:

``` scala
import io.expressier._

val Data = x"""(?<id>\w+)-(?<sub>.):(?<value>\d+)"""
```

Now we can access these names as methods on the match:

``` scala
val result = Data("foobar-Z:12345").getOrElse(sys.error("Invalid input"))
```

And then:

``` scala
scala> result.id
res0: String = foobar

scala> result.sub
res1: Char = Z

scala> result.value
res2: Int = 12345
```

Note that the type provider has determined that the sub-identifier can be
returned as a single character.

## Case classes

It's also now possible to collect the results of a regular expression match into
a case class (thanks to [Shapeless][shapeless]'s `LabelledGeneric`):

```scala
scala> case class Foo(i: Int, s: String, c: Char)
defined class Foo

scala> val parse = """(?<i>\d{4}): (?<c>.)(?<s>[a-z]+)""".express[Foo]
parse: io.expressier.Express.To[Foo] = ...

scala> val result: Option[Foo] = parse("1234: Foo")
result: Option[Foo] = Some(Foo(1234,oo,F))
```

Note that the orders of the fields and named groups don't have to match.

## Warnings


The implementation is a mess. There's no obvious library for parsing regular
expressions in Java (or at least I didn't turn one up this evening), so I've
just cracked open `java.util.regex`, and my `internals` package does a lot
of horrible work with reflection on private fields and non-public classes
inside `Pattern`. This is a limitation of the ecosystem, though, not of the
approach.

## License

circe is licensed under the **[Apache License, Version 2.0][apache]** (the
"License"); you may not use this software except in compliance with the License.

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

[apache]: http://www.apache.org/licenses/LICENSE-2.0
[macro-compat]: https://github.com/milessabin/macro-compat
[scala-js]: http://www.scala-js.org/
[shapeless]: https://github.com/milessabin/shapeless
[type-provider-slides]: https://github.com/travisbrown/type-provider-examples/blob/master/docs/scalar-2014-slides.pdf
