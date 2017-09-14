# unsiemly

> unseemly |ˌənˈsēmlē|, adj.
>
> (of behavior or actions) not proper or appropriate: an unseemly squabble.

> unsiemly |ˌənˈsēmlē|, noun
>
> a library for sending structured data to SIEMs

This project attempts to give you a simple abstraction for sending structured
data to a <abbr title="security information and event management">SIEM</abbr>.
It's intended for use by security operations teams, but since most modern SIEMs
just look an awful lot like stream processors tools, you can probably use it
for a bunch of other stuff.

Currently supports [ElasticSearch][es] (with support for AWS' hosted
Elasticsearch and its proprietary message signing) and [GCP's
StackDriver][gcpsd]. Because we use `java.time`/JSR310, this project requires
JDK 8.

## Usage

The primary abstraction is a [manifold][manifold] stream. This makes it easy to
test your code by giving you a clean separation between your logic and the
actual mechanics of getting data into your SIEM.

```clojure
(require '[unsiemly.core :as u])
```

There are two entry points: `u/->siem!` and `u/siem-sink!`. If you already have
a stream and you just want it to point at a SIEM now, `u/siem!` is what you
want. `u/siem-sink!` will build a new stream for you that you can put stuff
into. Both take an opts map.

### Options

The following options exist regardless of your specific SIEM type:

   * `::u/siem-type` (keyword, required): determines the type of SIEM to connect
     to. See below for potential values.
   * `::u/log-name` (string, required): sets the log name for your specific
     SIEM. This has slightly different effects depending on which one you're
     using; for example, on ElasticSearch this will set index names, but on
     StackDriver it will set the log name.

A simple builtin `:stdout` SIEM type exists that just prints each message. It
takes no additional options.

For ElasticSearch, the `::u/siem-type` value is `:elasticsearch`. The indices
are automatically partitioned by day, formatted as `$yourlogname-yyyy-MM-dd`.
The following options exist (were `es` is an alias for the
`unsiemly.elasticsearch` namespace):

   * `::es/hosts` (sequence of hosts with optional ports, required): The
     ElasticSearch hosts to connect to. A sniffer will automatically be
     configured, so if other hosts in the cluster are available and reachable,
     they will be used automatically.
   * `::es/aws-request-signing` (boolean, optional, default false): Should
     requests be signed with AWS credentials? This is only necessary for AWS'
     hosted Elasticsearch service. Credentials are accessed just like in the AWS
     SDK, so AWS credentials in your home directory or ones available via the
     AWS metadata service (e.g. on an EC2 instance or in a Lambda) will just
     work out of the box.
   * `::es/aws-region` (string, optional, default `us-east-1`): The AWS region
     an AWS-managed endpoint is in. This is only necessary for AWS' hosted
     ElasticSearch service: the region is required to perform signatures
     correctly.


For GCP StackDriver, the `::u/siem-type` value is `:stackdriver` and no extra
options exist. Credentials are automatically taken from the environment as per
the GCP SDK.

### Manifold stream 101

To put stuff onto a stream:

```clojure
(require '[manifold.stream :as ms])
(ms/put! siem {"hi" "from unsiemly"})
```

By default, streams won't keep your process running (most of the work is done in
daemon threads), so if you have a short-lived process and you just want to put
some stuff on the stream and then quit:

```clojure
(ms/put-all! siem msgs)
(ms/close! siem)
@(ms/on-drained siem)
```

For more details, see the manifold docs.

## Reformatting values

Usually, the data you have won't be in a format that your SIEM can consume.

By default, common data types that can't be appropriately serialized are already
handled. For example, SIEMs that consume JSON will have keywords transformed to
strings, timestamps are converted to ISO8601, et cetera.

If you have additional parsing needs, check out `unsiemly.xforms`, which has
utilities for less obvious transforms.

## Configuration via the environment

```clojure
(require '[unsiemly.env :refer [opts-from-env!]]')
(def siem (u/siem-sink! (opts-from-env!)))
```

Environment variable keys match the regular opt name but upper case and with
underscores, for example `SIEM_TYPE` turns into `:unsiemly.core/siem-type`. Opts
specific to a SIEM type are prefixed with the name of that SIEM type, for
example `ELASTICSEARCH_HOSTS` turns into `:unsiemly.elasticsearch/hosts`. Lists
(like `ELASTICSEARCH_HOSTS`) are comma-delimited. Booleans are just the strings
`true` and `false`.

## Relation to [unclogged][unclogged]

This project shares a number of things with unclogged:

* Their original purpose was to forward data to SIEMs
* The API is very similar
* They share an author

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

[unclogged]: https://github.com/RackSec/unclogged
[es]: https://www.elastic.co/
[gcpsd]: https://cloud.google.com/stackdriver/
[manifold]: https://github.com/ztellman/manifold
[specter]: https://github.com/nathanmarz/specter
