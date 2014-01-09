(ns co-working.core
  (:require
   [pallet.core.session :as session]
   [pallet.compute   :as compute]
   [pallet.configure :as configure]
   [pallet.node :as node]
   [pallet.crate.java :as java]
   [pallet.crate.git :as git]
   [pallet.crate.lein :as lein])
  (:use [pallet.crate.automated-admin-user :only [automated-admin-user]]
        [pallet.action :only [with-action-options]]
        [pallet.actions :only [packages package-manager package symbolic-link 
                               file exec-script directory remote-file 
                               minimal-packages exec-checked-script]]
        [pallet.api :only [plan-fn lift converge group-spec server-spec node-spec]]
        [pallet.crate :only [defplan get-settings]]
        [clojure.pprint]))

(defn show-nodes
  "A better node list"
  [srvc]
  (map #(vector (node/id %)
                (node/primary-ip %)
                (node/group-name %))
       (compute/nodes srvc)))

#_
(defn local-pallet-dir
  "Get the .pallet dir of the user currently running pallet"
  []
  (.getAbsolutePath
   (doto (if-let [pallet-home (System/getenv "PALLET_HOME")]
           (java.io.File. pallet-home)
           (java.io.File. (System/getProperty "user.home") ".pallet"))
     .mkdirs)))

#_
(defplan sane-package-manager
  []
  (package-manager :universe)
  (package-manager :multiverse)
  (package-manager :update))

(defplan standard-prereqs
  "General prerequesite packages and configurations"
  []
  (package-manager :update)
  (package-manager :upgrade)
  (packages :aptitude
            ["curl" "vim-nox" "ntp" "ntpdate" "htop" "gnu-standards" "flex"
             "bison" "gdb" "gettext" "build-essential" "perl-doc" "unzip"
             "rlwrap" "subversion" "unrar" "screen" "tmux"]))


(defplan clojure-development
  "tools needed to run clojure applications"
  []
  (let [administrative-user (:username (session/admin-user (session/session)))
        admin-home-dir (str "/home/" administrative-user)]
    (exec-script
     (if-not ("which" lein)
       (do
         ("wget" -q -O "/usr/local/bin/lein" "https://github.com/technomancy/leiningen/raw/stable/bin/lein")
         ("chmod" 755 ~"/usr/local/bin/lein")
         ("sudo" -u ~administrative-user ("lein" "self-install")))))))

(defplan co-working
  []
  (let [administrative-user (:username (session/admin-user (session/session)))
        admin-home-dir (str "/home/" administrative-user)]
    (packages :aptitude ["tmux"])
    (git/install-git)
    ;; (git/clone-or-pull "/usr/local/share/wemux" "git://github.com/zolrath/wemux.git")
    ;; (symbolic-link "/usr/local/share/wemux/wemux" "/usr/local/bin/wemux"
    ;;                :action :create
    ;;                :force true)
    ;; (remote-file "/usr/local/etc/wemux.conf" :force true :action :create
    ;;              :remote-file "/usr/local/share/wemux/wemux.conf.example")       
    ;; (exec-script (if-not (wemux list)
    ;;                (sudo -u ~administrative-user wemux new "-d")))
    ;; (file "/tmp/wemux-wemux" :mode 1777)
    ))

#_
(defn load-props
  [file-name]
  (with-open [^java.io.Reader reader (clojure.java.io/reader file-name)]
    (let [props (java.util.Properties.)]
      (.load props reader)
      (into {} (for [[k v] props] [(keyword k) (read-string v)])))))

#_
(defn update-users
  "load in configuration from file and add users"
  [request]
  (let [administrative-user (:username (session/admin-user request))
        admin-home-dir (str "/home/" administrative-user)
        user-git-dir "/www/analyticplus"
        users-list (load-props (str "resources/users_keys.properties"))]
    (-> request
        (for-> [[uname keyfile] users-list
                :let [user-home-dir (str "/home/" (name uname))
                      user-name (name uname)
                      user-www-dir (str user-home-dir "/www")
                      user-ssh-dir (str user-home-dir "/.ssh")
                      git-clone-target (str user-home-dir user-git-dir)]]
               (user user-name :shell "/bin/bash" :create-home true :home user-home-dir)
               (directory user-ssh-dir :owner user-name :group user-name :mode "755")
               (remote-file (str user-ssh-dir "/authorized_keys")
                            :local-file (str "resources/keyfiles/" keyfile)
                            :owner user-name :group user-name :mode "600")
               (exec-script
                (if (file-exists? (str ~admin-home-dir "/.ssh/known_hosts"))
                  (rm (str ~admin-home-dir "/.ssh/known_hosts"))))))))

;; ## Set Admin User with Assumptions(tm)
;;
;; * We assume that the user running pallet commands will have a ~/.pallet directory
;; * ~/.pallet dir contains ssh keys for the specified admin user in the format: admin-user-name_rsa.pub
#_
(defn set-admin-user
  "Use conventions to assume locations of keys for admin-user"
  [a-user]
  (let [l-p-dir (local-pallet-dir)]
    (admin-user a-user
                     :private-key-path (str l-p-dir "/" a-user "_rsa")
                     :public-key-path (str l-p-dir "/" a-user "_rsa.pub"))))

;; ## If it is needed, this will set the administrative user as 'padmin'
;; - public/private ssh keys will be looked up in the ~/.pallet directory 
;; - default behavior with this turned off is to use the user/ssh keys of the user running the converge
;(def ^:dynamic *admin-user* (set-admin-user "padmin"))

(def co-worker-default-node
  (node-spec
   :hardware {:min-cores 1 :min-ram 512}
   :image {:os-family :debian :os-64-bit true}
   :network {:inbound-ports [22 80]}))

(def with-base-server
  (server-spec
   :phases {:bootstrap (plan-fn (automated-admin-user))

            :settings (plan-fn
                       ;(java/settings {:vendor :openjdk})
                       (java/settings {:vendor :oraclepp :version "7"
                                       :components #{:jdk}
                                       :instance-id :oracle-7})
                       (lein/lein-settings {:version "2.1.3"
                                            :dir "/usr/local/bin/"}))
            :configure (plan-fn
                        ;; this will might your life better, but don't do on a slow internet connection
                        (standard-prereqs)
                                        ;(co-working)
                        (git/install-git)
                        ;; (java/install {})
                        ;; (clojure-development)
                                        ;                        (update-users)
                        )
            ;; :update-users (plan-fn (update-users))
            :java (plan-fn (java/install {}))
            :clojure (plan-fn (clojure-development))
            :co-working (plan-fn (co-working))
            :git-install (plan-fn
                          (git/install-git))
            :git-get (plan-fn
                      (let [co-dir "/tmp/p.03"]
                        (with-action-options {:script-prefix :no-sudo}
                          (git/clone "https://github.com/pallet/pallet.git"
                                     :checkout-dir co-dir)
                          (with-action-options {:script-dir co-dir}
                            (git/checkout "0.6.x"
                                          :remote-branch "remotes/origin/support/0.6.x")))))
            :git-gat (plan-fn
                      (with-action-options {:script-dir "/tmp/p.02"
                                            :script-prefix :no-sudo}
                        (git/checkout "0.7.x"
                                      :remote-branch "remotes/origin/support/0.7.x")))
            :tor-sync (plan-fn
                       (remote-file "/usr/local/bin/btsync" 
                                    :force true 
                                    :action :create
                                    :url 
                                    "http://btsync.s3-website-us-east-1.amazonaws.com/btsync_x64.tar.gz")
                       (exec-script
                        ("tar" "xfz" "/tmp/btsync.tar.gz" "-C" "/usr/local/bin/")))
            :sync-secret (plan-fn
                          (exec-checked-script
                           "foofy"
                           ("btsync" "--generate-secret")))
            :lein-install (plan-fn (lein/install-lein))
}))
 
(def co-worker-cs
  (group-spec
   "co-worker-cs" :extends [with-base-server]
   :node-spec co-worker-default-node))

(comment
  ;; Define compute
  (def aws-srvc (configure/compute-service :aws))
  (def vmfest (configure/compute-service :vmfest))

  ;; Examples of use
  ;;
  ;; Create a server
  (def cap (converge {co-worker-cs 1} :compute vmfest))
  (show-nodes vmfest)

  ;; Destroy all running servers
  (def cap (converge {co-worker-cs 0} :compute vmfest))

  ;; Update users on all running servers with the list in resources/users_keys.properties
  ;(def cap (lift co-worker-cs :compute vmfest :phase :update-users))
  ;(def cap (lift co-worker-cs :compute vmfest :phase :co-working))
  (def cap (lift co-worker-cs :compute vmfest :phase :configure))
  (def cap (lift co-worker-cs :compute vmfest :phase :git-install))
  (def cap (lift co-worker-cs :compute vmfest :phase :git-get))
  (def cap (lift co-worker-cs :compute vmfest :phase :git-gat))
  (def cap (lift co-worker-cs :compute vmfest :phase :java))
  (def cap (lift co-worker-cs :compute vmfest :phase :lein-install))
  (def cap (lift co-worker-cs :compute vmfest :phase :tor-sync))
  (def cap (lift co-worker-cs :compute vmfest :phase :sync-secret))
  )


;; Install java on all running servers
;(def cap (lift co-worker-cs :compute vmfest :phase :java))

;; Install clojure on all running servers
;(def cap (lift co-worker-cs :compute vmfest :phase :clojure))

;; Output all a list of all runninger servers
;(pprint (show-nodes vmfest))

