(ns leiningen.termide
  (:refer-clojure :exclude [test])
  (:import java.lang.ProcessBuilder
           java.lang.ProcessBuilder$Redirect)
  (:require [leiningen.core.main :refer [warn]]
            [leiningen.core.project :refer [merge-profiles]]
            [leiningen.core.eval :refer [eval-in-project]]
            [leiningen.run :refer [run-form]]
            [nrepl.core :as nrepl]
            [clojure.string :refer [trim]]
            [clojure.java.io :refer [file]]
            [clojure.java.shell :refer [sh]]))

(defn termide-version [] "0.1.0")
(defn nREPL_PORT [] 8777)
(def state-file (file ".build"))


(defn setup
  "Install dependencies (using nixpkgs)"
  [project args]
  (-> (ProcessBuilder. ["git" "init"]) .inheritIO .start .waitFor)
  (let [cmd ["nix-shell" "-p" "nodejs" "--run" "npm install"]]
       (-> (ProcessBuilder. cmd) .inheritIO .start .waitFor))
  (let [cmd ["nix-shell" "-p" "nodejs" "--run" "npm install karma-cli karma-junit-reporter"]]
       (-> (ProcessBuilder. cmd) .inheritIO .start .waitFor)))

(defn test
  "Run clj and cljs tests"
  [project args]
  ;; clj
  (-> (ProcessBuilder. ["lein" "test"]) .inheritIO .start .waitFor)
  (println)
  ;; cljs
  (let [cmd ["./node_modules/.bin/shadow-cljs" "compile" "karma-test"]]
       (-> (ProcessBuilder. cmd) .inheritIO .start .waitFor))
  (let [chromium-bin (trim (:out (sh "which" "chromium")))
        cmd ["./node_modules/.bin/karma" "start" "--single-run" "--reporters" "junit,dots"]
        pb (ProcessBuilder. cmd)]
       (-> pb .environment (.put "CHROME_BIN" chromium-bin))
       (-> pb .inheritIO .start .waitFor)))

(defn dev
  "Like the dev-alias provided by re-frame template, but with additional buildHook and termide in dependencies"
  [project args]
  (let [buildHook "{:build-hooks [(termide.hook/once-build)]}"]
       (.delete state-file)
       (.deleteOnExit state-file)
       (eval-in-project (merge-profiles project [(get-in project [:profiles :dev])
                                                 {:dependencies [['termide (termide-version)]]}])
                        (run-form `shadow.cljs.devtools.cli ["watch" "app" "--config-merge" buildHook]))))

(defn vim
  "Open vim with piggieback for shadow-cljs"
  [project args]
  (let [example-file (if (empty? args)
                         (file ".example.cljs"))
        args* (if (empty? args)
                  [(.getPath example-file)]
                  args)
        cmd (concat ["vim" "-c" "Piggieback (shadow.cljs.devtools.api/nrepl-select :app)"] args*)]
        (when example-file
              (spit example-file (str "(.log js/console \"hello world\")" "\n"
                                      "(* 6 7)"))
              (.deleteOnExit example-file))
        (-> (ProcessBuilder. cmd) .inheritIO .start .waitFor)))

(defn tmux
  "Open tmux with recommended windows"
  [project args]
  (let [keepOpen " ; bash"
        p (ProcessBuilder. ["tmux" "new" (str "git branch ; git log -n1 ; git status" keepOpen) ";"
                                   "new-window" "-d" (str "lein termide test" keepOpen) ";"
                                   "new-window" "-d" (str "lein termide dev" keepOpen)])]
       (.remove (.environment p) "TMUX")
       (-> p .inheritIO .start .waitFor)))

(defn clj-eval
  "Eval clojure code in the context of running nrepl-session"
  [project args]
  (let [conn (nrepl/connect :port (nREPL_PORT))]
     (-> (nrepl/client conn Long/MAX_VALUE)
         (nrepl/message {:op "eval" :code (apply str args)})
         nrepl/combine-responses println)))

(defn cljs-eval
  "Eval clojurescript code in the context of running nrepl-session"
  [project args]
  (let [conn (nrepl/connect :port (nREPL_PORT))
        client (-> (nrepl/client conn Long/MAX_VALUE)
                   nrepl/client-session)]
       (-> (nrepl/message client {:op "eval" :code "(shadow.cljs.devtools.api/nrepl-select :app)"})
           nrepl/combine-responses println)
       (-> (nrepl/message client {:op "eval" :code (apply str args)})
           nrepl/combine-responses println)))

(defn termide
  "Terminal IDE for re-frame/shadow-cljs projects"
  {:subtasks [#'setup #'test #'dev #'vim #'tmux #'clj-eval #'cljs-eval]}
  [project & args]
  (let [sub-name (first args)
        sub-args (rest args)]
       (case sub-name
             "setup"
               (setup project sub-args)
             "test"
               (test project sub-args)
             "dev"
               (dev project sub-args)
             "vim"
               (vim project sub-args)
             "tmux"
               (tmux project sub-args)
             "clj-eval"
               (clj-eval project sub-args)
             "cljs-eval"
               (cljs-eval project sub-args)
             nil
               (do (setup project sub-args)
                   (tmux project sub-args))
             (warn "Unknown task"))))
