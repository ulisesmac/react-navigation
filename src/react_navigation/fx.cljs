(ns react-navigation.fx
  (:require [react-navigation.navigation :as nav]
            [re-frame.core :as rf]))

(rf/reg-fx
 :fx.navigation/navigate
 (fn [screen]
   (nav/navigate! screen)))

(rf/reg-fx
 :fx.navigation/replace
 (fn [screen]
   (nav/replace! screen)))

(rf/reg-fx
 :fx.navigation/reset-to-root-and-open
 (fn [screen]
   (nav/reset-to-root-and-open! screen)))

(rf/reg-fx
 :fx.navigation/pop-to
 (fn [screen]
   (nav/pop-to! screen)))

(rf/reg-fx
 :fx.navigation/go-back
 (fn [_]
   (nav/go-back!)))
