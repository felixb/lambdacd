(ns lambdacd.ui-core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [reagent.core :as reagent :refer [atom]]
            [goog.events :as events]
            [goog.history.EventType :as EventType]
            [cljs.core.async :as async]
            [lambdacd.utils :as utils]
            [lambdacd.api :as api]
            [lambdacd.pipeline :as pipeline]
            [lambdacd.route :as route]
            [lambdacd.history :as history]
            [lambdacd.commons :as commons]
            [lambdacd.output :as output]
            [lambdacd.state :as state])
  (:import goog.History))

(enable-console-print!)

(def poll-frequency 1000)

(defn poll [atom fn]
  (go-loop []
    (let [history (async/<! (fn))]
      (reset! atom history))
    (async/<! (utils/timeout poll-frequency))
    (recur)))

(defn poll-history [history-atom]
  (poll history-atom api/get-build-history))

(defn poll-state [state-atom build-number-atom]
  (poll state-atom #(api/get-build-state @build-number-atom)))

(defn current-build-component [build-state-atom build-number step-id-to-display-atom output-details-visible do-tail]
  (if (not (nil? @build-state-atom))
    [:div {:key build-number :class "blocked"}
     [:h2 (str "Current Build " build-number)]
     [pipeline/pipeline-component build-number build-state-atom]
     [:h2 "Output"]
     [output/output-component @build-state-atom @step-id-to-display-atom output-details-visible do-tail]]
    [commons/loading-screen]))


(defn root [build-number-atom step-id-to-display-atom history state output-details-visible do-tail]
  (let [build-number @build-number-atom]
    (if build-number
      (do
        [:div
         [:div {:id "builds"} [history/build-history-component @history]]
          [:div {:id "currentBuild"} [current-build-component state build-number step-id-to-display-atom output-details-visible do-tail]]])
      [:div {:id "loading"}
       :h1 "Loading..."]
    )))


(defn- navigate [build-number-atom step-id-to-display-atom state-atom do-tail token]
  (let [nav-result (route/dispatch-route build-number-atom step-id-to-display-atom state-atom do-tail token)]
    (if (not (= :ok (:routing nav-result)))
      (.setToken (History.) (:redirect-to nav-result))
      )))

(defn hook-browser-navigation! [build-number-atom step-id-to-display-atom do-tail state-atom]
  (doto (History.)
    (events/listen
      EventType/NAVIGATE
      (fn [event]
        (navigate build-number-atom step-id-to-display-atom state-atom do-tail (.-token event))))
    (.setEnabled true)))

(defn init! []
  (let [build-number-atom (atom nil)
        step-id-to-display-atom (atom nil)
        history-atom (atom nil)
        state-atom (atom nil)
        output-details-visible (atom false)
        do-tail (atom false)]
    (poll-history history-atom)
    (poll-state state-atom build-number-atom)
    (hook-browser-navigation! build-number-atom step-id-to-display-atom do-tail state-atom)
    ; #' is necessary so that fighweel can update: https://github.com/reagent-project/reagent/issues/94
    (reagent/render-component [#'root
                               build-number-atom
                               step-id-to-display-atom
                               history-atom state-atom
                               output-details-visible
                               do-tail] (.getElementById js/document "app"))))


