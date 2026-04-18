(ns react-navigation.navigation
  (:require
   [applied-science.js-interop :as j]
   [react-navigation.native :refer [create-navigation-container-ref common-actions stack-actions]]))

(defonce navigation-ref (create-navigation-container-ref))

(defn current-route []
  (keyword "screen" (-> navigation-ref
                        (j/call-in [:current :getCurrentRoute])
                        (j/get :name))))

(defn ->js-nav-params [params]
  (-> params
      (update-keys prn-str)
      (update-vals prn-str)
      clj->js))

(defn- ready? []
  (j/call navigation-ref :isReady))

(defn navigate!
  ([route]
   (navigate! route nil))
  ([route params]
   (if (ready?)
     (let [js-route-name (name route)]
       ;; TODO: check if instead a ref/var should be passed
       (j/call navigation-ref :navigate js-route-name (->js-nav-params params)))
     (js/console.error "NAVIGATION IS NOT READY!")))
  ([navigator route params]
   (if (ready?)
     (let [navigator-name (name navigator)
           js-route-name  (name route)]
       ;; TODO: check if instead a ref/var should be passed
       (j/call navigation-ref :navigate navigator-name #js{:screen js-route-name
                                                           :params (->js-nav-params params)}))
     (js/console.error "NAVIGATION IS NOT READY!")))
  )

(defn pop-to!
  ([route]
   (pop-to! route nil))
  ([route params]
   (if (ready?)
     (let [js-route-name (name route)
           pop-to-action (j/call stack-actions :popTo js-route-name (->js-nav-params params)
                                 #js{:merge true})]
       (j/call navigation-ref :dispatch pop-to-action))
     (js/console.error "NAVIGATION IS NOT READY!"))))

(defn preload!
  ([route]
   (preload! route nil))
  ([route params]
   (if (ready?)
     (let [js-route-name (name route)]
       (j/call common-actions :preload js-route-name (->js-nav-params params)))
     (js/console.error "NAVIGATION IS NOT READY!"))))

(defn set-params! [params]
  (if (ready?)
    (j/call navigation-ref :dispatch
            (j/call common-actions :setParams (->js-nav-params params)))
    (js/console.error "NAVIGATION IS NOT READY!")))

(defn reset-root! [index routes]
  (if (ready?)
    (let [js-routes (->> routes
                         (map (fn [route-data]
                                (if (keyword? route-data)
                                  #js{:name (name route-data)}
                                  #js{:name   (-> route-data :name name)
                                      :params (->js-nav-params (:params route-data))})))
                         (to-array))]
      (j/call navigation-ref :resetRoot #js{:index  index
                                            :routes js-routes})
      js-routes)
    (js/console.error "NAVIGATION IS NOT READY!")))

(defn reset-to-root-and-open!
  ([route]
   (reset-to-root-and-open! route nil))
  ([route params]
   (if (ready?)
     (let [root-state  (j/call navigation-ref :getRootState)
           root-routes (j/get root-state :routes)
           root-route  (when root-routes
                         (aget root-routes 0))]
       (if root-route
         (j/call navigation-ref
                 :resetRoot
                 #js{:index  1
                     :routes (array root-route
                                    #js{:name   (name route)
                                        :params (->js-nav-params params)})})
         (js/console.error "NAVIGATION ROOT ROUTE NOT FOUND!")))
     (js/console.error "NAVIGATION IS NOT READY!"))))

(defn go-back! []
  (j/call navigation-ref :dispatch (j/call common-actions :goBack)))

(defn push!
  ([route]
   (push! route nil))
  ([route params]
   (if (ready?)
     (let [js-route-name (name route)
           push-action   (j/call stack-actions :push js-route-name (->js-nav-params params))]
       (j/call navigation-ref :dispatch push-action))
     (js/console.error "NAVIGATION IS NOT READY!"))))

(defn replace!
  ([route]
   (replace! route nil))
  ([route params]
   (if (ready?)
     (let [js-route-name  (name route)
           replace-action (j/call stack-actions :replace js-route-name (->js-nav-params params))]
       (j/call navigation-ref :dispatch replace-action))
     (js/console.error "NAVIGATION IS NOT READY!"))))


;; TODO: add reset-root! funciton

(comment

 (.getRootState navigation-ref)

 (.resetRoot navigation-ref #js{:index  0
                                :routes #js[#js{:name "tracked-packages"}]})

 (reset-root! 1 [:screen/tracked-packages :screen/package-info])

 (navigate! :screen/home :screen/new-package ;{:new-package? true}
            )
 )
