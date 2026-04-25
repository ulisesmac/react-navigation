(ns react-navigation.navigator.impl
  (:require
   [applied-science.js-interop :as j]
   [cljs.reader]
   [react-navigation.util :as util]
   [reagent-extended-compiler.utils.transforms :as transforms]
   [reagent.core :as r]))

;; Hot reload settings for development
(defonce reload-counter (r/atom 0))

(defn update-counter {:dev/after-load true} []
  (swap! reload-counter inc))

(defn- component-wrapper [props reagent-comp-screen display-name error-boundary]
  (let [route-params (some-> ^js (:route props)
                            .-params
                            js->clj
                            (update-keys cljs.reader/read-string)
                            (update-vals cljs.reader/read-string))]
    (with-meta
     [error-boundary
      [reagent-comp-screen {:route-params route-params}]]
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
                                              (-> screen
                                                  (update :screen
                                                          (fn [screen-component]
                                                            (reactify-screen
                                                             screen-kw
                                                             screen-component
                                                             error-boundary)))
                                                  transforms/->js-prop-obj)

                                              :else
                                              screen)))
              {}
              screens-map)))
