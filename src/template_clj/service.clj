(ns template-clj.service
  (:require [io.pedestal.http :as http]
            [io.pedestal.http.body-params :as body-params]
            [datomic.api :as d]
            [next.jdbc :as jdbc]
            [ring.util.response :as ring-resp]))

(defn- sucess-response
  [result status]
   (-> result
       ring-resp/response
       (ring-resp/status status)))

(defn- bad-response
  ([errors]
   (bad-response errors 500))
  ([errors status]
   (let [st (if (nil? status) 500 status)]
     (-> {:errors errors}
         ring-resp/response
         (ring-resp/status st)))))

(defn- handle-response-create!
  [response]
  (let [object response]
    (if false
      (bad-response "error" 404)
        (sucess-response {:message "create" :db object} 201))))


(defn home-page
  [request]
  (ring-resp/response {:message "Hello World!!" :status 200}))

(defn get-all-users! [storage]
  @storage)

(defn get-in-memory-database
  [{{storage :storage} :components}]
  (:storage storage))

(defn get-connection-postgres
  [{{db :db} :components}]
  db)


(defn get-all-users-db! [db]
  (jdbc/execute! (:conn db) ["SELECT * from users"]))


(defn get-all-users
  [request]
  (let [storage (get-in-memory-database request)]
    (ring-resp/response {:message (get-all-users! storage)})))

(defn get-all-users-db
  [request]
  (let [conn (get-connection-postgres request)]
    (ring-resp/response {:message (get-all-users-db! conn)})))

(def common-interceptors
  [(body-params/body-params) http/json-body])

(defn find-users [db]
  (d/q '[:find [(pull ?user [:user/type
                             :user/email
                             :user/name
                             :user/id])
                ...]
         :where [?user :user/name]]
       db))

(defn get-all-users-datomic! [conn]
  (find-users conn))

(defn get-all-users-datomic
  [{{datomic :datomic} :components}]
  (ring-resp/response {:message (get-all-users-datomic! (d/db (:conn datomic)))}))

(def routes
  #{["/" :get (conj common-interceptors `home-page)]
    ["/users-db" :get (conj common-interceptors `get-all-users-db)]
    ["/users-datomic" :get (conj common-interceptors `get-all-users-datomic)]
    ["/users" :get (conj common-interceptors `get-all-users)]})
