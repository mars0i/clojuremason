masonclj
===

**masonclj** is a very small library that provides functions and macros
that smooth a few rough edges to using
[MASON](https://cs.gmu.edu/~eclab/projects/mason) in
[Clojure](https://clojure.org).  (Most of the rough edges are due to
design choices by the architects of both Clojure and MASON that are very
reasonable choices on their own within the separate contexts of Clojure
and Java, but that turn out to be somewhat awkward when combined.)

## What is in masonclj and how does one use it?

See the [masonclj
documentation](https://github.com/mars0i/masonclj/blob/master/doc/masonclj/README.md)
in the doc/masonclj directory.

Also see the *example* directory, which contains a simple MASON model in
Clojure using masonclj.  The masonclj source is under *src*.

## Useful background material

[General-purpose notes on Clojure with MASON or other approaches to
ABMs](https://github.com/mars0i/masonclj/blob/master/doc/general/README.md)
are in the doc/general directory.  This provides rationales for some of
the design choices assumed by masonclj, but might also be of interest
to people who want to write ABMs in Clojure without MASON.  Some of
the Clojure-Java interop notes might of broader interest.

## What else do you need to know?

The documentation here doesn't provide introductions to Clojure or
MASON.  MASON has a detailed PDF manual that begins with a tutorial and
a example. To read the MASON manual, you'll need to know a little
bit of Java or have enough experience to figure it out as you read.  (My
"majure" repo includes some Clojure implementations of the Students
example from the MASON tutorial.) You will also have to learn a bit more
about Clojure-Java interop than is usually necessary, but studying the
source under the *example* directory with good interop resources at hand
is probably a good strategy.

The book <em>Clojure Programming</em> by Emerick et al.  from O'Reilly
is the best single source of information about Clojure-Java interop I've
encountered.  Some of the documents here may help, too, but they don't
go into detail.

## License

This software and text is copyright 2016, 2017, 2018, 2019 by [Marshall
Abrams](http://members.logical.net/~marshall/), and is distributed under
the [Gnu Lesser General Public License version
3.0](https://www.gnu.org/licenses/lgpl.html) as specified in the file
LICENSE, except where noted, or where code has been included that was
released under a different license.
