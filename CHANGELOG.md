# Change Log

All notable changes to this project will be documented in this file. This change log follows the conventions of [keepachangelog.com](http://keepachangelog.com/).

## [Unreleased]

### Added

- Pretty printing support for stdout, enabled by setting
  `::unsiemly.stdout/pretty-printed` to `true` (or the `STDOUT_PRETTY_PRINTED`
  environment variable to the string `true`).

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

[Unreleased]: https://github.com/latacora/unsiemly/compare/0.3.0...HEAD
[0.3.0]: https://github.com/latacora/unsiemly/compare/0.2.0...0.3.0
[0.2.0]: https://github.com/latacora/unsiemly/compare/0.1.1...0.2.0
[0.1.1]: https://github.com/latacora/unsiemly/compare/0.1.0...0.1.1
