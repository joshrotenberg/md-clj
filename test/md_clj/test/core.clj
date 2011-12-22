(ns md-clj.test.core
  (:use md-clj.core
        clojure.test)
  (:import [org.zeromq ZMsg ZFrame]))

(deftest zmsg-test
  (let [single-string (zmsg "a string")
        single-byte-array (zmsg (.getBytes "another one"))
        three-string-list (zmsg '("one" "two" "three"))
        three-string-vec (zmsg ["four" "five" "six"])
        number-integer (zmsg 3)
        number-long (zmsg 32423403233)
        number-double (zmsg 3.14)
        number-float (zmsg (Float. 3.14))
        string-array (zmsg (into-array ["these" "are" "in" "an" "array"]))
        byte-array-array (zmsg (into-array
                                [(.getBytes "bytes") (.getBytes "dude")]))
        number-array (zmsg (into-array [1 2 3 4 5]))]
        
    (is (= "a string" (.toString (first single-string))))
    (is (= "another one" (.toString (first single-byte-array))))
    (is (= 3 (.size three-string-list)))
    (is (= '("one" "two" "three") (map #(.toString %) (.toArray three-string-list))))
    (is (= '("four" "five" "six") (map #(.toString %) (.toArray three-string-vec))))
    (is (= 3 (read-string (.toString (first number-integer)))))
    (is (= 32423403233 (read-string (.toString (first number-long)))))
    (is (= 3.14 (read-string (.toString (first number-double)))))
    (is (= 3.14 (read-string (.toString (first number-float)))))
    (is (= '("these" "are" "in" "an" "array")
           (map #(.toString %) (.toArray string-array))))
    (is (= '("bytes" "dude")
           (map #(.toString %) (.toArray byte-array-array))))
    (is (= '(1 2 3 4 5) (map #(read-string (.toString %)) (.toArray number-array))))

    ))
