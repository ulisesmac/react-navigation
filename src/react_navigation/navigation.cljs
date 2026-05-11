(ns react-navigation.navigation
  (:require
   [applied-science.js-interop :as j]
   [cljs.reader]
   [react-navigation.native :refer [common-actions create-navigation-container-ref stack-actions]]
   [reagent-extended-compiler.utils.transforms :as xf]
   [reagent.core :as r]))

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

(defn ->clj-nav-params [params]
  (some-> params
          js->clj
          (update-keys cljs.reader/read-string)
          (update-vals cljs.reader/read-string)))

(defn true-sheet-parts [{:keys [header footer] :as options}]
  (fn [props]
    (let [route (j/lookup (j/get props :route))
          props {:route        {:key  (:key route)
                                :name (:name route)}
                 :route-params (->clj-nav-params (:params route))}]
      (-> options
          (assoc :header (r/as-element [header props])
                 :footer (r/as-element [footer props]))
          (xf/->js-prop-obj)))))

(defn- ->js-nested-nav-params [routes params]
  (reduce (fn [child route]
            #js{:screen (name route)
                :params child
                :merge  true})
          (->js-nav-params params)
          (reverse routes)))

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
     (js/console.error "NAVIGATION IS NOT READY!")))
  ([navigator route params]
   (if (ready?)
     (let [js-navigator-name (name navigator)
           route-params      (if (sequential? route)
                               (->js-nested-nav-params route params)
                               #js{:screen (name route)
                                   :params (->js-nav-params params)
                                   :merge  true})
           pop-to-action     (j/call stack-actions
                                      :popTo
                                      js-navigator-name
                                      route-params
                                      #js{:merge true})]
       (j/call navigation-ref :dispatch pop-to-action))
     (js/console.error "NAVIGATION IS NOT READY!"))))

(defn pop-to-target!
  ([target route]
   (pop-to-target! target route nil))
  ([target route params]
   (if (ready?)
     (let [js-route-name (name route)
           pop-to-action (j/call stack-actions :popTo js-route-name (->js-nav-params params)
                                 #js{:merge true})]
       (j/assoc! pop-to-action :target target)
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

(defn ->js-nav-route [route-data]
  (if (keyword? route-data)
    #js{:name (name route-data)}
    (let [js-route #js{:name (-> route-data :name name)}]
      (when-let [params (:params route-data)]
        (j/assoc! js-route :params (->js-nav-params params)))
      js-route)))

(defn reset-root-state! [state]
  (if (ready?)
    (j/call navigation-ref :resetRoot state)
    (js/console.error "NAVIGATION IS NOT READY!")))

(defn reset-root! [index routes]
  (if (ready?)
    (let [js-routes (->> routes
                         (map ->js-nav-route)
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

(defn- navigator-state-key [state route-name]
  (when-let [routes (j/get state :routes)]
    (some (fn [route]
            (or (when (= route-name (j/get route :name))
                  (j/get-in route [:state :key]))
                (when-let [child-state (j/get route :state)]
                  (navigator-state-key child-state route-name))))
          (array-seq routes))))

(defn go-back!
  ([]
   (j/call navigation-ref :dispatch (j/call common-actions :goBack)))
  ([navigator]
   (if (keyword? navigator)
     (let [route-name (name navigator)]
       (if-let [target (navigator-state-key (j/call navigation-ref :getRootState)
                                            route-name)]
         (let [go-back-action (j/call common-actions :goBack)]
           (j/assoc! go-back-action :target target)
           (j/call navigation-ref :dispatch go-back-action))
         (js/console.error "NAVIGATION TARGET NOT FOUND!" route-name)))
     (go-back!))))

(defn push!
  ([route]
   (push! route nil))
  ([route params]
   (if (ready?)
     (let [js-route-name (name route)
           push-action   (j/call stack-actions :push js-route-name (->js-nav-params params))]
       (j/call navigation-ref :dispatch push-action))
     (js/console.error "NAVIGATION IS NOT READY!")))
  ([navigator route params]
   (if (ready?)
     (if-let [target (navigator-state-key (j/call navigation-ref :getRootState)
                                          (name navigator))]
       (let [push-action (j/call stack-actions :push (name route) (->js-nav-params params))]
         (j/assoc! push-action :target target)
         (j/call navigation-ref :dispatch push-action))
       (js/console.error "NAVIGATION TARGET NOT FOUND!" (name navigator)))
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
