Change Log
==========


Version 2.11
------------

_2026-11-04_

* Upgrade Compose to 1.12.1 ([#196](https://github.com/block/radiography/pull/196), [#199](https://github.com/block/radiography/pull/199)).
* Upgrade Kotlin to 2.4.21 ([#197](https://github.com/block/radiography/pull/197)).
* Support the `GapComposer`, `LinkComposer`, and `RememberObserverHolder` internals introduced in newer Compose versions ([#196](https://github.com/block/radiography/pull/196)).
* Subcompositions are now scanned in deterministic child placement order ([#196](https://github.com/block/radiography/pull/196)).

Version 2.10
------------

_2026-07-23_

* Fix the `radiography` stoic-plugin launcher script when invoked via a symlink, e.g. from a Homebrew install ([#192](https://github.com/square/radiography/pull/192)).

Version 2.9
-----------

_2026-02-25_

* Upgrade Kotlin to 2.3.10 ([#190](https://github.com/square/radiography/pull/190)).
* Introduce radiography command-line tool ([#180](https://github.com/square/radiography/pull/180)).
* Release stoic-plugin via workflow ([#191](https://github.com/square/radiography/pull/191)).

Version 2.8
-----------

_2026-02-19_

* Upgrade Compose to 1.10.1 ([#184](https://github.com/square/radiography/pull/184)).
* Bump `minSdk` to 23 and `compileSdk`/`targetSdk` to 35 ([#184](https://github.com/square/radiography/pull/184)).
* Add `topLeft` bounds to `ScannableView.ComposeView` ([#175](https://github.com/square/radiography/pull/175)).
* Handle deprecated `InvisibleToUser` semantics property ([#185](https://github.com/square/radiography/pull/185)).
* Sort `SemanticsProperties` printing to ensure consistent ordering ([#184](https://github.com/square/radiography/pull/184)).
* Upgrade Gradle, AGP, and migrate to version catalogs ([#177](https://github.com/square/radiography/pull/177), [#179](https://github.com/square/radiography/pull/179), [#189](https://github.com/square/radiography/pull/189)).

Version 2.7
-----------

_2024-08-24_

Update to Compose 1.7 ([#169](https://github.com/square/radiography/pull/169)).

Version 2.6
-----------

_2024-04-30_

Compose call group chain information is now available. (#160)

Version 2.5
-------------

_2023-11-01_

Compose 1.5 is now supported. Due to breaking API changes within the Compose runtime, this version
is not compatible with earlier versions of Compose. If you need to use an earlier version of Compose,
use a 2.x version of Radiography.

The ScannableView.ComposeView has a new property, `semanticsConfigurations`, which exposes SemanticsConfiguration objects
from both Modifiers and the new Semantics tree. In newer versions of Compose, SemanticsConfigurations cannot
be read from the Modifier list, and are present only in the Semantics tree, 
so if you were previously using the `modifiers` property for this you should switch to
using `semanticsConfigurations` instead.

Version 2.4.1
-------------

_2021-9-15_

* Upgrade Curtains to 1.2.2 to avoid main thread check crashes. (#145)

Version 2.4.0
-------------

_2021-8-18_

* Use Curtains instead of WindowScanner. (#136)
* Upgrade Compose to 1.0.1 (and other dependency updates). (#141)

Version 2.3.0
-------------

_2021-3-1_

* Upgrade Compose to beta01. (#128)

Version 2.2.0
-------------

_2021-1-26_

* Don't render curly braces when they would be empty. (#108)
* Upgrade Compose to 1.0.0-alpha09. (#117)
* Lazily reflect the mKeyedTags field and don't crash if it's not there. (#122)

Version 2.1.0
-------------

_2020-10-14_

* Introduce support for rendering across subcompositions. (#104)
* Fix for bug where layout IDs to skip were queried incorrectly. (#94 – thanks @samruston!)
* Fix grammatical errors in documentation. (#92 – thanks @androiddevnotesfork!)
* Upgrade Compose to 1.0.0-alpha05. (#106)

Version 2.0.0
-------------

_2020-09-04_

* Use fancy drawing characters for rendering tree lines. (#83)
* Don't include "text-length" when full text included. (#82)
* Move internal code to internal package. (#87)
* Rename `showTextValue` parameter to `renderTextValue`. (#90)
* Update Compose to 1.0.0-alpha02. (#79)

Version 2.0.0-beta.1
--------------------

_2020-09-02_

* Correctly render nodes whose descriptions are more than one line. (#42)
* Support rendering trees nested deeper than 64 levels. (#41)
* Refactor `ViewStateRenderer` and `ViewFilter` API to be subclassable and more accessible from
  Java. (#44)
* Use the × character for formatting dimensions instead of the letter x. (#49)
* Add a sample app that uses Compose. (#53)
* Introduce support for rendering Compose hierarchies. (#33)
* Make SAM interfaces fun interfaces so they can be given as lambdas in Kotlin 1.4. (#47)
* Introduce ScanScope, a more flexible way to define what to scan. (#70)

Version 2.0.0-alpha.2
---------------------

_2020-08-12_

* Only access the view hierarchy from the main thread. (#30)
* Remove build config class. (#36)
* Change ViewFilter input from View to Any. (#37)
* Rename StateRenderer to ViewStateRenderer. (#38)
* Generate JVM overloads for methods with default args. (#38)
* Make ViewStateRenderers properties static and fix case. (#38)
* Make ViewFilter.and an extension function. (#38)

Version 2.0.0-alpha.1
---------------------

_2020-08-11_

* First public release!
* Convert the code to Kotlin. (#8, #9)
* Rewrite the API to be more Kotlin-friendly. (#16)
* Introduce `StateRenderer` to allow custom rendering of arbitrary view attributes. (#23)
* Drop the `com.squareup` package prefix, so the package is now just `radiography`. (#15)
* Add a sample app. (#18)
* Fix: Crash when `skippedIds` is empty. (#16)
* Fix: `FocusedWindowViewFilter` wasn't filtering correctly. (#18)

Version starts at 2.0.0 to differentiate from some internal releases.
