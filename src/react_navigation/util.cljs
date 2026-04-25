(ns react-navigation.util
  (:require
   [applied-science.js-interop :as j]
   [reagent.core :as r]))

(defonce react-native-build-error (atom nil))

(declare rehydratable-state)

(defn- rehydratable-copy [obj]
  (doto (js/Object.assign #js{} obj)
    (js-delete "key")
    (js-delete "stale")
    (js-delete "routeNames")
    (js-delete "preloadedRoutes")))

(defn- rehydratable-route [route]
  (let [route-copy   (rehydratable-copy route)
        route-state  (j/get route-copy :state)
        route-params (j/get route-copy :params)]
    (when route-state
      (j/assoc! route-copy :state (rehydratable-state route-state))
      (when (j/get route-params :screen)
        (js-delete route-copy "params")))
    route-copy))

(defn rehydratable-state [state]
  (when state
    (let [state-copy (rehydratable-copy state)]
      (when-let [routes (j/get state-copy :routes)]
        (->> routes
             array-seq
             (map rehydratable-route)
             into-array
             (j/assoc! state-copy :routes)))
      state-copy)))

(defn ^:dev/before-load clear-error! []
  (reset! react-native-build-error nil))

(defn error-boundary [_child]
  (let [error? (r/atom false)
        info   (r/atom nil)]
    (r/create-class
     {:display-name                 "ErrorBoundary"
      :component-did-catch          (fn [this error error-info]
                                      (reset! info {:error      error
                                                    :error-info error-info})
                                      (reset! react-native-build-error {:error      error
                                                                        :error-info error-info}))
      :get-derived-state-from-error (fn [e]
                                      (reset! error? true)
                                      #js{})
      :reagent-render               (fn [child]
                                      ;; TODO: add a check for development and a screen
                                      ;;       for productions
                                      (if @error?
                                        [:rn/view {:style {:flex               1
                                                           :justify-content    :center
                                                           :align-items        :center
                                                           :row-gap            12
                                                           :padding-horizontal 20}}
                                         [:rn/text {:style {:font-size   18
                                                            :font-weight 500
                                                            :color       :red}}
                                          "ERROR!"]
                                         [:rn/text
                                          (str (:error @info) "\n"
                                               (some-> @info ^js (:error-info) (.-componentStack) (subs 0 100))
                                               "\n")]]
                                        child))})))
