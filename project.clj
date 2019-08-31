(defproject termide "_"
  :git-version
    {:version-file "resources/version.edn"
     :status-to-version lein-git-version.plugin/default-status-to-version}
  :description "Terminal IDE for re-frame/shadow-cljs projects"
  :url "https://github.com/johannesloetzsch/termide"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :eval-in-leiningen true
  :plugins [[johannesloetzsch/lein-git-version "2.0.9"]])
