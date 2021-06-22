(defproject org.clojars.redcreation/red-db "0.2.0-SNAPSHOT"
  :description "红创数据库操作"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :deploy-repositories [["clojars" {:url "https://repo.clojars.org"
                                    :sign-releases false}]]
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [mount "0.1.16"]
                 [conman "0.9.1"]
                 [honeysql "1.0.461"]
                 [cprop "0.1.17"]
                 [camel-snake-kebab "0.4.2"]]
  :profiles {:dev
             {:resource-paths ["resources"]
              :dependencies [[com.h2database/h2 "1.4.200"]
                             [mysql/mysql-connector-java "8.0.16"]
                             [org.xerial/sqlite-jdbc "3.34.0"]
                             [migratus "1.3.5"]
                             [p6spy/p6spy "3.8.7"]]}}
  :repl-options {:init-ns red-db.core-test})
