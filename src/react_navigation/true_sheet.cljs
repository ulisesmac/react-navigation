(ns react-navigation.true-sheet
  (:require
   ["@lodev09/react-native-true-sheet/navigation" :refer [createTrueSheetNavigator]]
   [react-navigation.navigator.impl :as impl]
   [reagent-extended-compiler.utils.transforms :as transforms]))

(defn- reactify-screens-map [m]
  (update m :screens impl/->js-screens-data))

(defn- reactify-groups-map [config]
  (update config :groups
          (fn [groups]
            (update-vals groups
                         (fn [group]
                           (cond-> group
                             (:screens group) reactify-screens-map))))))

(defn create-true-sheet-navigator [config]
  (cond-> config
    (:screens config) reactify-screens-map
    (:groups config)  reactify-groups-map
    :always           transforms/->js-prop-obj
    :always           (createTrueSheetNavigator)))
