(ns react-navigation.bottom-tabs
  (:require
   ["@react-navigation/bottom-tabs" :refer [createBottomTabNavigator]]
   [react-navigation.navigator.impl :as impl]
   [reagent-extended-compiler.utils.transforms :as transforms]
   [reagent.core :as r]))

(defn create-bottom-tab-navigator
  ([screens-data]
   (create-bottom-tab-navigator screens-data {}))
  ([screens-data {:keys [error-boundary] :as config}]
   (-> screens-data
       (update :screens impl/->js-screens-data error-boundary)
       transforms/->js-prop-obj
       (createBottomTabNavigator)))
  ([]
   (let [tabs (createBottomTabNavigator)]
     {:navigator (r/adapt-react-class (.-Navigator tabs))
      :screen    (r/adapt-react-class (.-Screen tabs))})))
