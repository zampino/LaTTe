(ns latte.core
  "This namespace provides the top-level forms of the LaTTe
  framework. 

  Users (as opposed to developpers) of the framework should
  mostly depend on this namespace."

  (:require [clojure.pprint :as pp])

  (:require [latte.kernel.utils :as u])
  (:require [latte.kernel.presyntax :as stx])
  (:require [latte.kernel.typing :as ty])
  (:require [latte.kernel.norm :as n])
  (:require [latte.kernel.defenv :as defenv])
  (:require [latte.kernel.defs :as d])
  (:require [latte.kernel.proof :as p])
  )


;;{
;; ## Definitions (defined terms)
;;}

(defn parse-defterm-args [args]
    (when (> (count args) 4)
      (throw (ex-info "Too many arguments for defterm" {:max-arity 4 :nb-args (count args)})))
    (when (< (count args) 2)
      (throw (ex-info "Not enough arguments for defterm" {:min-arity 2 :nb-args (count args)})))
  (let [body (last args)
        params (if (= (count args) 2)
                 []
                 (last (butlast args)))
        doc (if (= (count args) 4)
              (nth args 1)
              "No documentation.")
        def-name (first args)]
    (when (not (symbol? def-name))
      (throw (ex-info "Name of defterm must be a symbol." {:def-name def-name})))
    (when (not (string? doc))
      (throw (ex-info "Documentation string for defterm must be ... a string." {:def-name def-name :doc doc})))
    (when (not (vector? params))
      (throw (ex-info "Parameters of defterm must be a vector." {:def-name def-name :params params})))
    [def-name doc params body]))

(defn mk-doc [content explanation]
  (str "\n"
       (with-out-str
         (pp/pprint content))
       "\n"
       explanation))

(defmacro defterm
  "Defines a mathematical term composed of a `name`, and optional (but highly recommended)
  `docstring`, a vector of `parameters` and a `lambda-term` as definitional content.

  An `ex-info` exception is thrown if the term cannot be defined.

  Note that it is a Clojure `def`, the term is defined in the namespace where the `defterm` 
  form is invoked."
  [& args]
  (let [[def-name doc params body] (parse-defterm-args args)]
    ;;(println "def-name =" def-name " doc =" doc " params =" params " body =" body)
    (when (defenv/registered-definition? {} def-name)
      (do
        ;;(throw (ex-info "Cannot redefine term." {:name def-name})))
        ;; TODO: maybe disallow redefining if type is changed ?
        ;;       otherwise only warn ?
        (println "[Warning] redefinition as term: " def-name)))
    (let [[status definition] (as-> {:tag :term :name def-name :doc doc} $
                                (d/handle-term-definition $ {} [] params body))]
      (when (= status :ko)
        (throw (ex-info "Cannot define term." {:name def-name, :error definition})))
      (let [quoted-def# (list 'quote `~definition)]
        `(do
           (def ~def-name ~quoted-def#)
           (alter-meta! (var ~def-name)  (fn [m#] (assoc m#
                                                         :doc (mk-doc (quote ~body) ~doc)
                                                         :arglists (list (quote ~params)))))
           [:defined :term (quote ~def-name)])))))

;;{
;; ## Definitions of theorems
;;}

(defn parse-defthm-args [args]
    (when (> (count args) 4)
      (throw (ex-info "Too many arguments for defthm" {:max-arity 4 :nb-args (count args)})))
    (when (< (count args) 2)
      (throw (ex-info "Not enough arguments for defthm" {:min-arity 2 :nb-args (count args)})))
  (let [body (last args)
        params (if (= (count args) 2)
                 []
                 (last (butlast args)))
        doc (if (= (count args) 4)
              (nth args 1)
              "No documentation.")
        def-name (first args)]
    (when (not (symbol? def-name))
      (throw (ex-info "Name of defthm must be a symbol." {:def-name def-name})))
    (when (not (string? doc))
      (throw (ex-info "Documentation string for defthm must be ... a string." {:def-name def-name :doc doc})))
    (when (not (vector? params))
      (throw (ex-info "Parameters of defthm must be a vector." {:def-name def-name :params params})))
    [def-name doc params body]))

(defmacro defthm
  [& args]
  (let [[def-name doc params ty] (parse-defthm-args args)]
    ;;(println "def-name =" def-name " doc =" doc " params =" params " ty =" ty)
    (when (defenv/registered-definition? {} def-name)
      (do
        ;;(throw (ex-info "Cannot redefine term." {:name def-name})))
        ;; TODO: maybe disallow redefining if type is changed ?
        ;;       otherwise only warn ?
        (println "[Warning] redefinition as theorem: " def-name)))
    (let [definition (as-> {:tag :theorem :name def-name :doc doc} $
                       (d/handle-thm-declaration $ {} params ty))
          quoted-def# (list 'quote `~definition)]
      `(do
         (def ~def-name ~quoted-def#)
         (alter-meta! (var ~def-name)  (fn [m#] (assoc m#
                                                       :doc (mk-doc (quote ~ty) ~doc)
                                                       :arglists (list (quote ~params)))))
         [:declared :theorem (quote ~def-name)]))))

;;{
;; ## Definitions of axioms
;;}

(defn parse-defaxiom-args [args]
    (when (> (count args) 4)
      (throw (ex-info "Too many arguments for defaxiom" {:max-arity 4 :nb-args (count args)})))
    (when (< (count args) 2)
      (throw (ex-info "Not enough arguments for defaxiom" {:min-arity 2 :nb-args (count args)})))
  (let [body (last args)
        params (if (= (count args) 2)
                 []
                 (last (butlast args)))
        doc (if (= (count args) 4)
              (nth args 1)
              "No documentation.")
        def-name (first args)]
    (when (not (symbol? def-name))
      (throw (ex-info "Name of defaxiom must be a symbol." {:def-name def-name})))
    (when (not (string? doc))
      (throw (ex-info "Documentation string for defaxiom must be ... a string." {:def-name def-name :doc doc})))
    (when (not (vector? params))
      (throw (ex-info "Parameters of defaxiom must be a vector." {:def-name def-name :params params})))
    [def-name doc params body]))

(defmacro defaxiom
  [& args]
  (let [[def-name doc params ty] (parse-defaxiom-args args)]
    ;;(println "def-name =" def-name " doc =" doc " params =" params " ty =" ty)
    (when (defenv/registered-definition? {} def-name)
      (do
        ;;(throw (ex-info "Cannot redefine term." {:name def-name})))
        ;; TODO: maybe disallow redefining if type is changed ?
        ;;       otherwise only warn ?
        (println "[Warning] redefinition as axiom: " def-name)))
    (let [definition (as-> {:tag :axiom :name def-name :doc doc} $
                       (d/handle-axiom-declaration $ {} params ty))
          quoted-def# (list 'quote `~definition)]
      `(do
         (def ~def-name ~quoted-def#)
         (alter-meta! (var ~def-name)  (fn [m#] (assoc m#
                                                       :doc (mk-doc (quote ~ty) ~doc)
                                                       :arglists (list (quote ~params)))))
         [:declared :axiom (quote ~def-name)]))))

;;{
;; ## Top-level term parsing
;;}

(defn parse-context-args [def-env args]
  (loop [args args, ctx []]
    (if (seq args)
      (do
        (when (not (and (vector? (first args))
                        (= (count (first args)) 2)))
          (throw (ex-info "Context argument must be a binding pair." {:argument (first args)})))
        (let [[x ty] (first args)
              ty' (stx/parse def-env ty)]
          (when (not (symbol? x))
            (throw (ex-info "Binding variable  must be a symbol." {:argument (first args) :variable x})))
          (when (not (ty/proper-type? def-env ctx ty'))
            (throw (ex-info "Binding type is not a type." {:argument (first args) :binding-type ty})))
          (recur (rest args) (conj ctx [x ty']))))
      ctx)))

(defmacro term [& args]
    (let [def-env {}
          t (stx/parse def-env (last args))
          ctx (parse-context-args def-env (butlast args))]
      ;;(println "[term] t = " t " ctx = " ctx)
      (if (latte.kernel.norm/beta-eq? t :kind)
        '□
        (let [ty (ty/type-of def-env ctx t)]
          (list 'quote t)))))

;;{
;; ## Top-level type checking
;;}

(defmacro type-of [& args]
  (let [def-env {}
        t (stx/parse def-env (last args))
        ctx (parse-context-args def-env (butlast args))]
    (let [ty (ty/type-of def-env ctx t)]
      (list 'quote ty))))

(defmacro check-type? [& args]
  (let [def-env {}
        t (stx/parse def-env (last (butlast args)))
        ty (stx/parse def-env (last args))
        ctx (parse-context-args def-env (butlast (butlast args)))]
    (let [tty (ty/type-of def-env ctx t)]
      (n/beta-delta-eq? def-env ty tty))))

;;{
;; ## Top-level term equivalence
;;}

(defn === [t1 t2]
  (let [def-env {}
        t1 (stx/parse def-env t1)
        t2 (stx/parse def-env t2)]
    (n/beta-delta-eq? def-env t1 t2)))


(def term= ===)

;;{
;; ## Proof handling
;;}

(defmacro proof
  "Provides a proof of theorem named `thm-name` using the given proof `method`
  and `steps`.

  There are for now two proof methods available:

    - the `:term` method with one step: a direct proof/lambda-term
      inhabiting the theorem/type (based on the proof-as-term, proposition-as-type
      correspondances). This is a low-level proof method.

    - the `:script` method with a declarative proof script. It is a high-level
  (human-readable) proof method. A low-level proof term is
  synthetized from the script"
  {:style/indent [2 :form :form [1]]}
  [thm-name method & steps]
  (let [def-env {}
        [status thm] (defenv/fetch-definition def-env thm-name)]
    (when (= status :ko)
      (throw (ex-info "No such theorem." {:name thm-name})))
    (let [[status proof-term]
          (p/check-proof def-env (:params thm) (:type thm) method steps)]
      (if (= status :ko)
        (throw (ex-info (str "Proof failed: " (:msg proof-term)) {:theorem thm-name
                                                                  :error (dissoc proof-term :msg)}))
        (let [new-thm# (list 'quote (assoc thm :proof `~proof-term))]
          `(do
             (alter-var-root (var ~thm-name) (fn [_#] ~new-thm#))
             [:qed (quote ~thm-name)]))))))

(defmacro try-proof
  "Tries (but does not register) a proof of theorem named `thm-name` using the given proof `method`
  and `steps`."
  {:style/indent [2 :form :form [1]]}
  [thm-name method & steps]
  (let [def-env {}
        [status thm] (defenv/fetch-definition def-env thm-name)]
    (if (= status :ko)
      [:ko {:msg "No such theorem." :name thm-name}]
      (let [[status proof-term]
            (p/check-proof def-env (:params thm) (:type thm) method steps)]
        (if (= status :ko)
          [:ko {:msg (str "Proof failed: " (:msg proof-term)) :theorem thm-name
                :error (dissoc proof-term :msg)}]
          (let [new-thm (list 'quote (assoc thm :proof proof-term))
                name# (name thm-name)]
            [:qed ~name#]))))))

;;{
;; ## Indentation rules
;;}

(defmacro lambda
  {:style/indent [1 :form :form]} 
  [bindings body]
  `((quote  λ) ~bindings ~body))

(defmacro forall
  {:style/indent [1 :form :form]} 
  [bindings body]
  `((quote Π) ~bindings ~body))

(defmacro assume
  {:style/indent [1 :form [1]]} 
  [bindings & body]
  `((quote assume) ~bindings ~body))

(defmacro have
  {:style/indent [1]}
  [& args]
  `((quote have) ~@args))

