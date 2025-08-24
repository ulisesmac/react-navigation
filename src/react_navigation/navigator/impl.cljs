(ns react-navigation.navigator.impl
  (:require
   [applied-science.js-interop :as j]
   [cljs.reader]
   [react-navigation.navigation :as nav]
   [react-navigation.util :as util]
   [reagent.core :as r]))

;; Hot reload settings for development
(defonce reload-counter (r/atom 0))

(defn update-counter {:dev/after-load true} []
  (swap! reload-counter inc))

(defn- component-wrapper [params reagent-comp-screen display-name error-boundary]
  (let [js-params   (.-params ^js (:route params))
        ;; TODO: optimize transformation of nil values
        push-params (some-> js-params ^String .-cljData cljs.reader/read-string)
        pop-params  (some-> js-params ^String .-cljDataBack cljs.reader/read-string)]
    (with-meta
     [error-boundary
      [reagent-comp-screen {:navigation   {:pop-to   nav/pop-to!
                                           :navigate nav/navigate!}
                            :route-params (merge push-params pop-params)}]]
     {:key (str display-name "-" @reload-counter)})))

(defn- reactify-screen [screen-kw screen error-boundary]
  ;; Remove the class component when it's addressed
  ;; https://github.com/reagent-project/reagent/issues/548
  ;; TODO: Optimize it on prod build
  (let [display-name          (str "Screen-" (name screen-kw))
        dummy-class-component (fn [params]
                                [component-wrapper params screen display-name error-boundary])]
    (r/reactify-component
     (with-meta dummy-class-component {:display-name display-name}))))

(defn ->js-screens-data
  ([screens-map]
   (->js-screens-data screens-map #'util/error-boundary))
  ([screens-map error-boundary]
   (reduce-kv (fn [acc screen-kw screen]
                (assoc acc (name screen-kw) (cond
                                              (j/get screen :displayName)
                                              screen

                                              (and (fn? screen))
                                              (reactify-screen screen-kw screen error-boundary)

                                              (and (map? screen) (fn? (:screen screen)))
                                              (reactify-screen screen-kw (:screen screen) error-boundary)

                                              :else
                                              screen)))
              {}
              screens-map)))
