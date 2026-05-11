(ns react-navigation.bottom-tabs
  (:require
   ["@react-navigation/bottom-tabs" :refer [createBottomTabNavigator]]
   [react-navigation.navigator.impl :as impl]
   [react-navigation.util :as util]
   [reagent-extended-compiler.utils.transforms :as transforms]
   [reagent.core :as r]))

(defn- convert-screens-map [m error-boundary]
  (update m :screens impl/->js-screens-data error-boundary))

(defn- convert-groups-map [config error-boundary]
  (update config :groups
          (fn [groups]
            (update-vals groups
                         (fn [group]
                           (cond-> group
                             (:screens group) (convert-screens-map error-boundary)))))))

(defn- wrap-tab-bar [config error-boundary]
  (update config :tab-bar
          (fn [tab-bar]
            (fn [props]
              (r/as-element [error-boundary [tab-bar props]])))))

(defn create-bottom-tab-navigator
  ([screens-data]
   (create-bottom-tab-navigator screens-data {}))
  ([screens-data {:keys [error-boundary]
                  :or   {error-boundary #'util/error-boundary}}]
   (cond-> screens-data
     (:screens screens-data) (convert-screens-map error-boundary)
     (:groups screens-data)  (convert-groups-map error-boundary)
     (:tab-bar screens-data) (wrap-tab-bar error-boundary)
     :always                 transforms/->js-prop-obj
     :always                 (createBottomTabNavigator)))
  ([]
   (let [tabs (createBottomTabNavigator)]
     {:navigator (r/adapt-react-class (.-Navigator tabs))
      :screen    (r/adapt-react-class (.-Screen tabs))})))
