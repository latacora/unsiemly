# Change Log

All notable changes to this project will be documented in this file. This change log follows the conventions of [keepachangelog.com](http://keepachangelog.com/).

## [Unreleased]

## [0.8.0] -- 2018-04-10

### Added

- BigQuery now reports errors on insertion. Previously, BigQuery may error
  silently, typically when the connection itself worked, but e.g. a table didn't
  exist or the data is in the wrong format.

### Changed

- Bumped dependencies.

## [0.7.0] -- 2018-02-21

### Added

- BigQuery support. `::unsiemly/siem-type` is `:bigquery`. Project id, dataset
  id and table id can be explicitly configured via keys in the
  `unsiemly.bigquery` namespace; if no explicit BigQuery-specific keys are
  provided, will use the default project, the log name as the dataset id and
  `unsiemly" as the table id.`

## [0.6.0] -- 2017-12-14

### Changed

- The stdout sink now explicitly flushes on every callback call.

## [0.5.0] -- 2017-12-01

### Added

- JSON support for the stdout reporter. To use it, set the `::stdout/format` opt
  to `:json` (or `STDOUT_FORMAT` env var to "json"). Pretty printing also works
  for JSON.

### Fixed

- Setting the `STDOUT_PRETTY_PRINTED` environment variable worked incorrectly:
  its behavior was not to pretty print if unset, and pretty print if set to any
  value. The intended behavior (now implemented) was to set it if the
  environment variable was set to `"true"`, and unset it if the environment
  variable was unset or set to `"false"`. (This is a minor bug, since it only
  affected you if you were setting the environment variable to `"false"` or an
  invalid value.)

## [0.4.0] - 2017-11-30

### Added

- Pretty printing support for stdout, enabled by setting
  `::unsiemly.stdout/pretty-printed` to `true` (or the `STDOUT_PRETTY_PRINTED`
  environment variable to the string `true`).

### Changed

- Upgraded dependencies

## [0.3.0] - 2017-09-18

### Added

- `process!` API, which takes a number of messages and returns a deferred that
   fires when they have all been consumed. (#15, also related to #16)

## [0.2.0] - 2017-09-18

### Added

- The GCP implementation will coerce incoming data structures so that they can
  be sent to StackDriver. Keywords will be turned into their name, instants,
  their ISO8601 timestamps, and everything else will be str'd. (#10)

### Changed

- Upgraded dependencies

## [0.1.1] - 2017-09-17

### Changed

- Made Clojars the default deploy target, making deploys easier in the future

## 0.1.0 - 2017-09-17

Initial release.

### Added

- Public API: `->siem!` and `siem-sink!`, to mirror [unclogged]
- Basic StackDriver and Elasticsearch support
- Basic data transforms, especially for timestamps

[Unreleased]: https://github.com/latacora/unsiemly/compare/0.8.0...HEAD
[0.6.0]: https://github.com/latacora/unsiemly/compare/0.7.0...0.8.0
[0.6.0]: https://github.com/latacora/unsiemly/compare/0.6.0...0.7.0
[0.5.0]: https://github.com/latacora/unsiemly/compare/0.5.0...0.6.0
[0.4.0]: https://github.com/latacora/unsiemly/compare/0.4.0...0.5.0
[0.4.0]: https://github.com/latacora/unsiemly/compare/0.3.0...0.4.0
[0.3.0]: https://github.com/latacora/unsiemly/compare/0.2.0...0.3.0
[0.2.0]: https://github.com/latacora/unsiemly/compare/0.1.1...0.2.0
[0.1.1]: https://github.com/latacora/unsiemly/compare/0.1.0...0.1.1
