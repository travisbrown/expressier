Type providers for regular expressions
======================================

This is a quick proof-of-concept demonstration of what a type provider for
regular expressions might look like. There are currently lots of limitations
and problems with the implementation, and essentially no documentation or
tests. For a more detailed introduction to type providers in Scala, please see
[this project](https://github.com/travisbrown/type-provider-examples).

Motivation
----------

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
  case _ => sys.error("Do something sensible here, please.")
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
  case _ => sys.error("Do something sensible here, please.")
}.toMap
```

This works, but it's more verbose, a little redundant, and it involves a
partial function. We know `toInt` should never explode here, but the compiler
doesn't.

With our type provider
----------------------

The implementation provided here allows the following usage:

``` scala
import expressier._

val Row = """(\w\w)\s*:\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)\s*""".regex

val totals: Map[String, Int] = rows.map {
  case Row(id, first, second, third) => (id, first + second + third)
  case _ => sys.error("Do something sensible here, please.")
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

With named groups
-----------------

Now suppose we've got a regular expression with some named groups:

``` scala
import expressier._

val Data = """(?<id>\w+)-(?<sub>.):(?<value>\d+)""".regex
```

Now we can access these names as methods on the match:

``` scala
val result = Data.parse("foobar-Z:12345").getOrElse(
  sys.error("Do the right thing here, please.")
)
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

Warnings
--------

The implementation is a mess. There's no obvious library for parsing regular
expressions in Java (or at least I didn't turn one up this evening), so I've
just cracked open `java.util.regex`, and my `internals` package does a lot
of horrible work with reflection on private fields and non-public classes
inside `Pattern`. This is a limitation of the ecosystem, though, not of the
approach.

For the named group syntax, I'm subclassing `TupleN`, which are case classes.
This is convenient for a demo, and it's safe, since I'm just adding aliases
for some of the case class members, so I don't have to worry about breaking
equality, etc. But it's still probably not a good idea.

Lastly: often the behavior I provide in this demo is of course exactly the
opposite of what you want (e.g. if you're using `(\d{3}-(\d{3})-(\d{4})` for
U.S. phone numbers, you'd better not even think about turning those matches
into integers. One solution would be to allow a target type parameter on the
extension method:

``` scala
val UsPhoneRegex = """(\d{3}-(\d{3})-(\d{4})""".regex[(String, String, String)]
```

Or even better, to support extraction directly to a case class, and have the type
alignment confirmed at compile time:

``` scala
case class UsPhone(area: String, local1: String, local2: String)

val UsPhoneRegex = """(\d{3}-(\d{3})-(\d{4})""".regex[UsPhone]
```

I could also imagine extraction into [Shapeless](https://github.com/milessabin/shapeless)
records, etc.

