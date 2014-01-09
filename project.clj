(defproject co-working "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [com.palletops/pallet "0.8.0-beta.8"]
                 [org.cloudhoist/pallet-vmfest "0.3.0-alpha.2"]
                 [com.palletops/lein-crate "0.8.0-alpha.1"]
                 [com.palletops/git-crate "0.8.0-alpha.1"]
                 [com.palletops/ssh-transport "0.4.2"]
                 [com.palletops/java-crate "0.8.0-beta.4"]
                 [org.clojars.tbatchelli/vboxjws "4.2.6"]
                 [org.cloudhoist/pallet-jclouds "1.5.2"]
                 [org.jclouds/jclouds-allblobstore "1.5.5"]
                 [org.jclouds/jclouds-allcompute "1.5.5"]
                 [org.jclouds.driver/jclouds-slf4j "1.5.5"]
                 [org.jclouds.driver/jclouds-sshj "1.5.5"]
                 [ch.qos.logback/logback-classic "1.0.0"]
                 [org.clojure/tools.cli "0.2.1"]]
  :dev-dependencies [[lein-marginalia "0.7.0"]
                     [org.cloudhoist/pallet
                      "0.7.3" :type "test-jar"]
                     [com.palletops/pallet-lein "0.6.0-beta.9"]]
  :profiles {:dev
             {:dependencies [[org.cloudhoist/pallet "0.7.3" :classifier "tests"]]
              :plugins [[com.palletops/pallet-lein "0.6.0-beta.9"]]}}
  :local-repo-classpath true
  :repositories {"sonatype" "https://oss.sonatype.org/content/repositories/releases"
                 "sonatype-snapshots" "https://oss.sonatype.org/content/repositories/snapshots"}
  ;; :repl-options {:init (do (require 'gis-try.repl)
  ;;                          (gis-try.repl/force-slf4j))}
)
