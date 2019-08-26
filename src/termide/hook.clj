(ns termide.hook
  (:require [clojure.java.shell :refer [sh]]
            [clojure.java.io :refer [file]]))

(def state-file (file ".build"))

(defn once-build
  "Hook that is started only after the first build"
  {:shadow.build/stage :flush}
  [build-state & args]
  (when-not (.exists state-file)
            (spit state-file "")

            (sh "tmux" "new-window" "lein repl :connect 8777 ; bash")
            (sh "tmux" "new-window" "lein termide vim ; bash"))

  build-state)
