(ns react-navigation.native
  (:require
   ["@react-navigation/native" :refer [CommonActions
                                       createStaticNavigation
                                       createStaticNavigator
                                       createNavigationContainerRef
                                       useNavigation
                                       StackActions]]
   [applied-science.js-interop :as j]
   [reagent.core :as r]))

(def create-navigation-container-ref createNavigationContainerRef)

(defn use-navigation []
  (let [js-nav-functions (useNavigation)]
    {:navigate (j/get js-nav-functions :navigate)
     :go-back  (j/get js-nav-functions :goBack)}))

(defn reset-route [new-route]
  (.reset CommonActions #js {:index 1
                             :routes new-route}))

(defn create-static-navigation [stack]
  (r/adapt-react-class (createStaticNavigation stack)))


(def stack-actions StackActions)

(def common-actions CommonActions)
