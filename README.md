# unsiemly

> unseemly |ˌənˈsēmlē|, adj.
> (of behavior or actions) not proper or appropriate: an unseemly squabble.

> unsiemly |ˌənˈsēmlē|, noun
> a library for sending structured data to SIEMs

## Installation

FIXME

## Usage

FIXME: explanation

## Relation to [unclogged][unclogged]

This project shares a number of things with unclogged:

* Their original purpose was to forward data to SIEMs
* The API is very similar
* They share one of their authors

However, they are different projects with different goals. This project is an
abstraction over SIEMs, consuming mostly-structured data and giving you tools
for transforming it into a structure your SIEM can usefully consume.
Meanwhile, [unclogged][unclogged] only cares about syslog. Syslog strictly
consumes strings, not structured messages. unclogged will let you cheat and send
in a structured message, but internally it just converts the object to a string.
That process can't be configured, and if you're unlucky you'll just see a
`clojure.lang.LazySeq@deadbeef`. Otherwise, you'll get an EDN-ish data
structure, which might be fine, but also might be totally different from what
your SIEM expects for further processing, alerting, et cetera.


It would make sense for unsiemly to use unclogged to send information to a
syslog-speaking SIEM (see issue #2). Neither project replaces the other: they're
cousins operating on a different abstraction layer.

## License

Copyright © Latacora

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
