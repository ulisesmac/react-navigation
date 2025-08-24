(ns react-navigation.navigation
  (:require
   [applied-science.js-interop :as j]
   [react-navigation.native :refer [create-navigation-container-ref
                                    create-static-navigation
                                    common-actions
                                    stack-actions]]))

(defonce navigation-ref (create-navigation-container-ref))

(defn current-route []
  (keyword "screen" (-> navigation-ref
                        (j/call-in [:current :getCurrentRoute])
                        (j/get :name))))

(defn- ready? []
  (j/call navigation-ref :isReady))

(defn navigate!
  ([route]
   (navigate! route nil))
  ([route params]
   (if (ready?)
     (let [js-route-name (name route)]
       ;; TODO: check if instead a ref/var should be passed
       (j/call navigation-ref :navigate js-route-name #js{:isCljEncoded true
                                                          :cljData      (prn-str params)}))
     (js/console.error "NAVIGATION IS NOT READY!")))
  ([navigator route params]
   (if (ready?)
     (let [navigator-name (name navigator)
           js-route-name  (name route)]
       ;; TODO: check if instead a ref/var should be passed
       (j/call navigation-ref :navigate navigator-name #js{:screen js-route-name
                                                           :params #js{:cljData (prn-str params)}}))
     (js/console.error "NAVIGATION IS NOT READY!")))
  )

(defn pop-to!
  ([route]
   (pop-to! route nil))
  ([route params]
   (if (ready?)
     (let [js-route-name (name route)
           pop-to-action (j/call stack-actions :popTo
                                 js-route-name
                                 #js{:isCljEncoded true
                                     :cljDataBack  (prn-str params)}
                                 #js{:merge true})]
       (j/call navigation-ref :dispatch pop-to-action))
     (js/console.error "NAVIGATION IS NOT READY!"))))

(defn preload!
  ([route]
   (preload! route nil))
  ([route params]
   (if (ready?)
     (let [js-route-name (name route)]
       (j/call common-actions :preload js-route-name #js{:cljData (prn-str params)}))
     (js/console.error "NAVIGATION IS NOT READY!"))))

(defn reset-root! [index routes]
  (if (ready?)
    (let [js-routes (->> routes
                         (map (fn [route-data]
                                (if (keyword? route-data)
                                  #js{:name (name route-data)}
                                  #js{:name   (-> route-data :name name)
                                      :params #js{:isCljEncoded true
                                                  :cljData      (prn-str (-> route-data :params))}})))
                         (to-array))]
      (j/call navigation-ref
              :resetRoot #js{:index  index
                             :routes js-routes})
      js-routes)
    (js/console.error "NAVIGATION IS NOT READY!")))

(defn go-back! []
  (j/call navigation-ref :dispatch (j/call common-actions :goBack)))


;; TODO: add reset-root! funciton

(comment

 (.getRootState navigation-ref)

 (.resetRoot navigation-ref #js{:index  0
                                :routes #js[#js{:name "tracked-packages"}]})

 (reset-root! 1 [:screen/tracked-packages :screen/package-info])

 (navigate! :screen/home :screen/new-package ;{:new-package? true}
            )
 )
