(ns react-navigation.util
  (:require [reagent.core :as r]))

(defn error-boundary [_child]
  (let [error? (r/atom false)
        info   (r/atom nil)]
    (r/create-class
     {:display-name                 "ErrorBoundary"
      :component-did-catch          (fn [this error error-info]
                                      (reset! info {:error      error
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
