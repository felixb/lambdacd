(ns lambdacd.styleguide
  (:require [lambdacd.testutils :refer [query]]
             [lambdacd.testcases :as testcases]))

(defn- testcase [query]
  (second (re-find #"testcase=([^&]+)" query)))

(defn- initialize-styleguide-overview []
  (println "overview"))

(defn- initialize-testcase [testcase]
  (case testcase
    "main" (testcases/main)))

(defn initialize-styleguide []
  (let [testcase (testcase (query))]
    (if testcase
      (initialize-testcase testcase)
      (initialize-styleguide-overview))))