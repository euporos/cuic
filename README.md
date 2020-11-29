# <img src="kuikka.svg" align="left" width="60" height="60"> CUIC

Clojure UI testing with Chrome

[![Build Status](https://img.shields.io/travis/milankinen/cuic/master.svg?style=flat-square)](https://travis-ci.org/milankinen/cuic)
[![Clojars Project](https://img.shields.io/clojars/v/cuic.svg?style=flat-square)](https://clojars.org/cuic)
[![cljdoc](https://img.shields.io/badge/cljdoc-latest-blue?style=flat-square)](https://cljdoc.org/d/cuic/cuic/CURRENT)

## Motivation

I needed a library for writing robust and maintainable UI tests for my work 
and hobby Clojure(Script) projects. The library had to run on top of the 
JVM to simplify test setups and enable code sharing, but without the 
annoying WebDriver version hassle. 

The design of the current version of `cuic` is the result of countless 
(re)written tests, hours after hours of CI test runs and enless debugging
sessions, driven by the following core principles:

  * Utilization of Clojure core data structures and language features 
    instead of custom macros and DSLs
  * Minimal and unambiguous but easily extendable API contract 
  * Seamless integration with `clojure.test` and the tooling around it
  * First class REPL usage

## Show me the code!

Here's a small snippet showing how to test the classic
[TodoMVC app](http://todomvc.com/examples/react) with `cuic`

```clojure 
(ns example-todomvc-tests
  (:require [clojure.test :refer :all]
            [cuic.core :as c]
            [cuic.test :refer [deftest* is* browser-test-fixture]]))

(use-fixtures
  :once
  (browser-test-fixture))

(defn todos []
  (->> (c/query ".todo-list li")
       (map c/text-content)))

(defn add-todo [text]
  (doto (c/find ".new-todo")
    (c/fill text))
  (c/press 'Enter))

(deftest* creating-new-todos
  (c/goto "http://todomvc.com/examples/react")
  (is* (= [] (todos)))
  (add-todo "Hello world!")
  (is* (= ["Hello world!"] (todos)))
  (add-todo "Tsers!")
  (is* (= ["Hello world!" "Tsers!"] (todos))))
```

## Documentation

Each `cuic` function has a Clojure doc-string describing its behaviour and usage. 
Generated API docs and guides are also available as **[cljdoc](https://cljdoc.org/d/cuic/cuic)**.

* [Usage guide](https://cljdoc.org/d/cuic/cuic/CURRENT/doc/usage)
    * [Launching Chrome](https://cljdoc.org/d/cuic/cuic/CURRENT/doc/usage/launching-chrome)
    * [Searching elements](https://cljdoc.org/d/cuic/cuic/CURRENT/doc/usage/searching-elements)
    * [Interacting with page](https://cljdoc.org/d/cuic/cuic/CURRENT/doc/usage/interacting-with-page)
    * [Dealing with asynchrony](https://cljdoc.org/d/cuic/cuic/CURRENT/doc/usage/dealing-with-asynchrony)
    * [Writing tests](https://cljdoc.org/d/cuic/cuic/CURRENT/doc/usage/writing-tests)
    * [REPL usage](https://cljdoc.org/d/cuic/cuic/CURRENT/doc/usage/repl-usage)

## Similar projects

* [Etaoin](https://github.com/igrishaev/etaoin) - Pure Clojure implementation of Webdriver protocol
* [clj-chrome-devtools](https://github.com/tatut/clj-chrome-devtools) - Clojure wrapper for Chrome Debug Protocol

## License

MIT
