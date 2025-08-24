# react-navigation (CLJS)

Idiomatic ClojureScript helpers and wrappers around React Navigation for React Native apps.

Focus: simple data-first configuration for stacks and ergonomic navigation from CLJS.

## Install

- deps.edn: add the library

```clojure
;; use local/root in a monorepo
{:deps {react-navigation/react-navigation {:local/root "react-navigation"}}}
```

- npm: install React Navigation and peers if you need them (match your RN/React versions)

```sh
npm install @react-navigation/native \
  @react-navigation/stack @react-navigation/native-stack \
  react-native-screens react-native-safe-area-context \
  react-native-gesture-handler react-native-reanimated
```

Follow each package’s RN setup steps (e.g. gesture-handler, reanimated, screens).

## Quick Start

Define a stack with keyword screen names and Reagent components. It's shadow-clj's hot reload compatible out of the box.

```clojure
(ns app.nav
  (:require [react-navigation.stack :as stack]
            [react-navigation.navigation :as nav]
            [react-navigation.native :as rnn]))

(defn home-screen []
  [:rn/view
   [:rn/text "Home"]
   [:rn/button {:title    "Details"
                :on-press #(nav/navigate! :screen/details {:id 42})}]])

(defn details-screen [{:keys [route-params]}]
  (let [{:keys [id]} route-params]
    [:rn/view
     [:rn/text (str "Details: " id)]
     [:rn/button {:title "Back" :on-press nav/go-back!}]]))

(def root-stack
  (stack/create-stack-navigator
    {:initial-route-name :screen/home
     :screen-options     {:header-shown false ,,,}
     :screens            {:screen/home    #'home-screen
                          :screen/details #'details-screen}}))

(def static-navigation (rnn/create-static-navigation root-stack))

(defn navigation-root []
  [static-navigation {:ref nav/navigation-ref}])
```

Wondering how `:rn/view` and `:rn/text` work? See [reagent-extended-compiler](https://github.com/ulisesmac/reagent-extended-compiler).

## Passing Parameters

Push params when navigating, and read them from `route-params`:

```clojure
;; Sender
(nav/navigate! :screen/details {:id 42 :from :home})

;; Receiver
(defn details-screen [{:keys [route-params]}]
  (let [{:keys [id from]} route-params]
    [:rn/view
     [:rn/text (str "Details: " id " from " (name from))]]))
```

You can also pass data back when popping to a route:

```clojure
;; Child screen, go back to :screen/home with data
(nav/pop-to! :screen/home {:saved? true})

;; Home screen receives it on next render
(defn home-screen [{:keys [route-params]}]
  (let [{:keys [saved?]} route-params]
    [:rn/view
     (when saved? [:rn/text "Saved!"])]))
```

## Navigation API

- `nav/navigate!`:
  - `(navigate! :screen/details)`
  - `(navigate! :screen/details {:id 42})`
  - `(navigate! :navigator/root :screen/details {:id 42})` to target a nested navigator
- `nav/go-back!`: go back one screen
- `nav/pop-to!`: pop the stack to a route by name, with optional params
- `nav/reset-root!`: replace root routes, e.g. auth flows
- `nav/current-route`: returns current route keyword

Hook usage inside components:

```clojure
(ns feature.comp
  (:require [react-navigation.native :as rnn]))

(defn button []
  (let [{:keys [navigate go-back]} (rnn/use-navigation)]
    [:rn/view
     [:rn/button {:title "Open" :on-press #(navigate :screen/details)}]
     [:rn/button {:title "Back" :on-press go-back}]]))
```

## Stacks and Options

Pass CLJS maps; keys are kebab-case and converted to JS props (camelCase) for React Navigation.

```clojure
(stack/create-stack-navigator
  {:initial-route-name :screen/home
   :screen-options     {:header-shown         false
                        :animation            :slide_from_right
                        :detach-previous-screen false}
   :screens            {:screen/home #'home-screen}})
```

Advanced: group screens with shared options by using `:groups` and `:screen-layout`.
See a complete example in `react-navigation/examples/screens_defs.cljs`.

## Notes

- Requires the npm `@react-navigation/*` packages and their peer dependencies.
- Screen names are keywords; params are CLJS data. They are encoded/decoded for you when navigating.
- Works with Reagent components and supports var references (`#'my-screen`) for hot reload friendliness.
