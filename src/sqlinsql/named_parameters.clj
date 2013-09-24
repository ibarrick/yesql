(ns sqlinsql.named-parameters)

(defn consume-to
  [text escape? marker?]
  (loop [accumulator []
         [head & remainder :as string] text]
    (cond (not head) [accumulator nil nil]
          (and (escape? head)
               (marker? (first remainder))) (recur (conj accumulator head (first remainder))
                                                   (rest remainder))
               (marker? head) [accumulator head remainder]
               :else (recur (conj accumulator head)
                            remainder)))) 

(defn split-at-parameters
  [query]
  (loop [chars []
         [head & tail :as remainder] query]
    (case head
      nil [(apply str chars)]
      \' (let [[string marker next-bit] (consume-to tail #{\\} #{\'})]
           (recur (into (conj chars head)
                        (conj string marker))
                  next-bit)) 
      \? (cons (apply str chars)
               (cons (symbol (str head))
                     (split-at-parameters tail)))
      \: (case (first tail)
           \: (recur (conj chars head (first tail))
                     (rest tail))
           (let [[parameter marker next-bit] (consume-to tail
                                                         (constantly false)
                                                         #{\space \newline \, \" \' \: \& \; \( \) \| \= \+ \- \* \% \/ \\ \< \> \^})]
             (cons (apply str chars)
                   (cons (symbol (apply str parameter))
                         (split-at-parameters (cons marker next-bit)))))) 
      (recur (conj chars head)
             tail)))) 

(defn convert-named-query
  "Convert a named-parameter query into a plain placeholder query, plus a list of the parameter names."
  [query]
  (let [split (split-at-parameters query)]
    [(reduce (fn [accumulator token]
               (str accumulator
                    (if (symbol? token)
                      "?"
                      token)))
             ""
             split)
     (filter symbol? split)]))