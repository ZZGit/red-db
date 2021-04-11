(defproject red-db "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [conman "0.9.1"]
                 [honeysql "1.0.461"]
                 [cprop "0.1.17"]
                 [p6spy/p6spy "3.8.7"]]
  :profiles {:dev
             {:resource-paths ["resources"]
              :dependencies [[mount "0.1.16"]
                             [com.h2database/h2 "1.4.200"]
                             [migratus "1.3.5"]]}}
  :repl-options {:init-ns red-db.core-test})
