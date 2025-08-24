(ns react-navigation.stack
  (:require
   ["@react-navigation/stack" :refer [createStackNavigator TransitionSpecs TransitionPresets]]
   [react-navigation.navigator.impl :as impl]
   [reagent-extended-compiler.utils.transforms :as transforms]))

(defn- reactify-screens-map [m]
  (update m :screens impl/->js-screens-data))

(defn create-stack-navigator [config]
  (let [screens-at-root?   (:screens config)
        screens-in-groups? (-> config :groups vals first :screens seq)]
    (cond-> config
      screens-at-root?   reactify-screens-map
      screens-in-groups? (update :groups update-vals reactify-screens-map)
      :always            transforms/->js-prop-obj
      :always            (createStackNavigator))))

(def transition-specs TransitionSpecs)

(def transition-presets TransitionPresets)
