(ns md-clj.test.core
  (:use md-clj.core
        clojure.test)
  (:import [org.zeromq ZMsg ZFrame]))

(deftest to-zmsg-test
  (let [zmsg (to-zmsg (doto (ZMsg.) (.add (ZFrame. "something"))))
        zframe (to-zmsg (ZFrame. "something else"))
        zframe-array (to-zmsg (into-array [(ZFrame. "another")]))
        single-string (to-zmsg "a string")
        single-byte-array (to-zmsg (.getBytes "another one"))
        three-string-list (to-zmsg '("one" "two" "three"))
        three-string-vec (to-zmsg ["four" "five" "six"])
        number-integer (to-zmsg 3)
        number-long (to-zmsg 32423403233)
        number-double (to-zmsg 3.14)
        number-float (to-zmsg (Float. 3.14))
        string-array (to-zmsg (into-array ["these" "are" "in" "an" "array"]))
        byte-array-array (to-zmsg (into-array
                                [(.getBytes "bytes") (.getBytes "dude")]))
        number-vec (to-zmsg [1 2 3])
        number-array (to-zmsg (into-array [1 2 3 4 5]))]

    (is (= "something" (.toString (first zmsg))))
    (is (= "something else" (.toString (first zframe))))
    (is (= "another" (.toString (first zframe-array))))
    (is (= "a string" (.toString (first single-string))))
    (is (= "another one" (.toString (first single-byte-array))))
    (is (= 3 (.size three-string-list)))
    (is (= '("one" "two" "three")
           (map #(.toString %) (.toArray three-string-list))))
    (is (= '("four" "five" "six")
           (map #(.toString %) (.toArray three-string-vec))))
    (is (= 3 (read-string (.toString (first number-integer)))))
    (is (= 32423403233 (read-string (.toString (first number-long)))))
    (is (= 3.14 (read-string (.toString (first number-double)))))
    (is (= 3.14 (read-string (.toString (first number-float)))))
    (is (= '("these" "are" "in" "an" "array")
           (map #(.toString %) (.toArray string-array))))
    (is (= '("bytes" "dude")
           (map #(.toString %) (.toArray byte-array-array))))
    (is (= '(1 2 3 4 5)
           (map #(read-string (.toString %)) (.toArray number-array))))
    ))

(deftest from-zmsg-test
  (is (= 1 (from-zmsg (to-zmsg 1) :as-number)))
  (is (= "1" (from-zmsg (to-zmsg 1) :as-string)))
  (is (= "1" (String. (from-zmsg (to-zmsg 1) :as-bytes))))

  (is (= '(1 2 3) (from-zmsg (to-zmsg [1 2 3]) :as-number )))
  (is (= '("1" "2" "3")  (from-zmsg (to-zmsg [1 2 3]) :as-string )))
  (is (= '("1" "2" "3") (map #(String. %)
                             (from-zmsg (to-zmsg [1 2 3]) :as-bytes ))))

  (is (= "foo" (from-zmsg (to-zmsg "foo") :as-string)))
  (is (= "foo" (String. (from-zmsg (to-zmsg "foo") :as-bytes))))

  (is (= '("foo" "baz") (from-zmsg (to-zmsg ["foo" "baz"]) :as-string)))
  (is (= '("foo" "baz") (map #(String. %) (from-zmsg (to-zmsg ["foo" "baz"]) :as-bytes))))
  )

(deftest with-message-type-test
    (with-message-type :as-bytes
    (is (= (Class/forName "[B") (class (from-zmsg (to-zmsg "2"))))))
  (with-message-type :as-string
    (is (= java.lang.String (class (from-zmsg (to-zmsg "2"))))))
  (with-message-type :as-number
    (is (= java.lang.Long (class (from-zmsg (to-zmsg "2"))))))
  (with-message-type :as-zmsg
    (is (= org.zeromq.ZMsg (class (from-zmsg (to-zmsg "2"))))))

  (with-message-type :as-bytes
    (is (= (Class/forName "[B")
           (class (first (from-zmsg (to-zmsg ["2" "3"])))))))
  (with-message-type :as-string
    (is (= java.lang.String
           (class (first (from-zmsg (to-zmsg ["2" "3"])))))))
  (with-message-type :as-number
    (is (= java.lang.Long
           (class (first (from-zmsg (to-zmsg ["2" "3"])))))))
  (with-message-type :as-zmsg
    (is (= org.zeromq.ZMsg
           (class (from-zmsg (to-zmsg ["2" "3"]))))))
  )