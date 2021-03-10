## Interacting with DOM

After you've obtained an HTML element with [queries](./queries.md), you can 
interact with it: read its properties (e.g. classes or attributes) or perform 
some actions to it (e.g. click it). `cuic` doesn't provide any ready-made 
DSLs like `click("#my-button").having({text: "Save"}).and.then.is({visible: true})`.
Instead, `cuic`'s motivation is to provide a set of simple core building blocks 
that encourage you to create your own project specific utilitities, using 
Clojure language primitives and best practices as much as you can. Simple
functions are composable and easier to remember than complex DSLs.

### Inspecting element properties

`cuic` provides various functions for element inspection. In order to use 
these fucntions you need a handle to an existing element by using either 
`find` or `query`. Here are just some examples, see [[cuic.core]] namespace
for a complete list of built-in functions.

```clojure 
;; Check whether the save button has "primary" class or not
(c/has-class? (c/find "#save-btn") "primary")

;;;;;

(defn todos []
  (c/query ".todo-item"))

;; Utility function for retrieving todo item text
(defn todo-text [todo]
  (-> (c/find {:in todo :by "label"})
      (c/inner-text)
      (string/trim)))

;; Compose utility with query results
(is (= ["lol "bal] (map todo-text (todos))))
```

DOM inspection **never** performs any implicit waiting. Instead, each function 
return the state as it is (or was) during the invocation time. Note that 
due to the mutable nature of DOM, read functions are **not** referentially 
transparent. In other words, subsequent calls may return different 
results if the DOM changes between the calls. You must take this 
into account when implementing your functions and assertions: use 
[[cuic.core/wait]] to mitigate the issues with asynchrony or when you're 
expecting something to be found. Inspection functions do not perform any 
mutations to the DOM making them safe to call multiple times.

To see the complete list of read functions, see `cuic.core` reference
from the [API docs](https://cljdoc.org/d/cuic/cuic). 

### Simulating user actions

`cuic` provides multiple built-in actions to simulate the user behaviour on 
the page: clicks, typing, focusing on elements, scrolling, etc... Like 
inspection functions, actions require a handle to the target element. However, 
unlike inspection functions, actions **may implicitly wait** until the target 
element satisfies some specific condition(s) that enable the action: for 
example `(c/click save-btn)` will wait until the `save-btn` becomes visible 
and enabled. If the required condition is not satisfied within the defined 
timeout, action fails with an exception.

> **Attention!** Because actions may actually *mutate* the page state, be 
> careful to call them only once. In other words, do **not** place any
> actions inside `cuic.core/wait` or results may be devastating. 

All built-in actions take the target element as a first argument, so 
they work well with Clojure's built-in `doto` macro. 

```clojure 
(defn add-todo [text]
  (doto (c/find ".new-todo)
    (c/clear-text)
    (c/fill text))
  (c/press 'Enter))
```

To see the complete list of available actions, see `cuic.core` reference
from the [API docs](https://cljdoc.org/d/cuic/cuic). 

### JavaScript evaluation

Sometimes built-in functions don't provide any sensible means for getting
the required information from the page. In such cases, the information may
be available via direct JavaScript evaluation. That's why `cuic` has
`eval-js` and `exec-js` which provide a way to evaluate plain JavaScript 
expressions on the page and get results back as Clojure data structures.
`eval-js` expects an *expression* and returns the result of that expression.
`exec-js` is the mutating counterpart of `eval-js`. Instead of an expression,
it takes an entire function body. It does not return anything unless explicitly 
defined with `return <expr>;` statement at the end of the function body.

Both functions can be parametrized by giving the (named) arguments as 
a map and using map keys as variables in the JavaScript code. Arguments
must be either serializable JSON values or references to JavaScript objects
(either html elements or objects returned from previous `eval-js` / `exec-js` 
invocations). The return value is serialized back to native Clojure data
structures for plain JSON values and other object references are returned as
special `cuic` data structures.

Both `eval-js` and `exec-js` support JavaScript `this` context binding. The 
bound `this` context must be a reference to JavaScript object. By default,
`this` is bound to the `window` object.

```clojure 
(defn title-parts [separator]
  {:pre [(string? separator)]}
  (c/eval-js "document.title.split(sep)" {:sep separator}))

;; Returns boolean whether the given checkbox has indeterminate state or not
(defn indeterminate? [checkbox]
  (c/eval-js "this.indeterminate === true" {} checkbox))
  
(defn set-title [new-title]
  (c/exec-js "const prev = document.title;
              document.title = val;
              return prev;" 
             {:val new-title}))
             
(def local-store (c/eval-js "localStorage"))
(c/exec-js "store.setItem('foo', val)" {:store local-store :val "tsers"})
(c/eval-js "store.getItem('foo')" {:store local-store})
```

> **NOTE:** Performance-wise passing object references from Clojure
> to JavaScript and vice versa is slower than passing primitive values
> or `this` binding. If you have only one object reference, you should
> use `this` instead of named argument.
 

Both `eval-js` and `exec-js` also support asynchronous values out-of-box. If 
your expression or statement is asynchronous, you can wait for it with JavaScript's
`await` keyword. Clojure code will wait until the async result is available
and then return it to the caller. 

```clojure 
;; will yield true after 500 ms
(is (= "tsers" (c/eval-js "await new Promise(resolve => setTimeout(() => resolve('tsers'), 500))")))
```

## Closing words

At this point you should be familiar with element queries and how
to inspect the queried elements and how to simulate user actions
using them. The next section goes deeper into real use cases and
introduce some ways to handle asynchrony and race conditions that 
are always present in the UI tests.
