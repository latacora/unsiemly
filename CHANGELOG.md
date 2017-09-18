# Change Log

All notable changes to this project will be documented in this file. This change log follows the conventions of [keepachangelog.com](http://keepachangelog.com/).

## [Unreleased]

### Added

- By default, the GCP implementation will coerce incoming data structures so
  that they can be sent to StackDriver. Keywords will be turned into their name,
  instants, their ISO8601 timestamps, and everything else will be str'd. (#10)

## [0.1.1] - 2017-09-17

### Changed

- Made Clojars the default deploy target, making deploys easier in the future

## 0.1.0 - 2017-09-17

Initial release.

### Added

- Public API: `->siem!` and `siem-sink!`, to mirror [unclogged]
- Basic StackDriver and Elasticsearch support
- Basic data transforms, especially for timestamps

[Unreleased]: https://github.com/latacora/unsiemly/compare/0.1.1...HEAD
[0.1.1]: https://github.com/latacora/unsiemly/compare/0.1.0...0.1.1
